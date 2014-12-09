package bytecode;
import java.io.Reader;
import java.util.Properties;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Opcodes;


public class AddOBFID extends BaseStreamingJarProcessor {
	public static void main(String[] args) {
		new AddOBFID().go(args);
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
			String obfid;
			
			boolean isInterface;
			
			@Override
			public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
				className = name;
				super.visit(version, access, name, signature, superName, interfaces);
				
				obfid = props.getProperty(className);
				
				isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
			}
			
			@Override
			public void visitEnd() {
				if(obfid != null)
					super.visitField((isInterface ? Opcodes.ACC_PUBLIC : Opcodes.ACC_PRIVATE) | Opcodes.ACC_STATIC | Opcodes.ACC_FINAL, "__OBFID", "Ljava/lang/String;", null, obfid);
				super.visitEnd();
			}
		};
	}
}
