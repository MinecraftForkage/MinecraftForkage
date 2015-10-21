package bytecode;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ApplyExceptions extends BaseStreamingJarProcessor {
	public static void main(String[] args) {
		new ApplyExceptions().go(args);
	}
	
	Properties props = new Properties();
	@Override
	public void loadConfig(Reader file) throws Exception {
		props.load(file);
	}
	
	@Override
	public ClassVisitor createClassVisitor(ClassVisitor parent) throws Exception {
		return new ClassVisitor(Opcodes.ASM5, parent) {
			String className;
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				className = name;
				super.visit(version, access, name, signature, superName, interfaces);
			}
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if(exceptions == null)
					exceptions = new String[0];
				
				String propExcs = props.getProperty(className+"."+name+desc, "").split("\\|")[0];
				
				if(!propExcs.equals("")) {
					List<String> excSet = new ArrayList<String>(Arrays.asList(exceptions));
					for(String newExc : Arrays.asList(propExcs.split(",")))
						if(!excSet.contains(newExc))
							excSet.add(newExc);
				
					exceptions = excSet.toArray(new String[excSet.size()]);
				}
				
				return super.visitMethod(access, name, desc, signature, exceptions);
			}
		};
	}
}
