package bytecode;

import java.io.Reader;
import java.util.Properties;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;


public class ApplyParamNames extends BaseStreamingJarProcessor {
	public static void main(String[] args) {
		new ApplyParamNames().go(args);
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
			public MethodVisitor visitMethod(final int access, String name, String desc, String signature, String[] exceptions) {
				
				if((access & (Opcodes.ACC_NATIVE | Opcodes.ACC_ABSTRACT)) != 0)
					return super.visitMethod(access, name, desc, signature, exceptions);
				
				final Type[] paramTypes = Type.getArgumentTypes(desc);
				
				String[] paramNames = new String[paramTypes.length];
				
				String methodProp = props.getProperty(className+"."+name+desc, "");
				String propNames = methodProp.contains("|") ? methodProp.split("\\|", -1)[1] : "";
				
				if(!propNames.equals("")) {
					String[] propNamesArray = propNames.split(",");
					if(propNamesArray.length != paramTypes.length)
						throw new RuntimeException("method "+name+" has "+paramTypes.length+" params, but "+propNamesArray.length+" names given");
					paramNames = propNamesArray;
					
				} else if(name.matches("func_\\d+_.+")) {
					String methodIndex = name.substring(5, name.indexOf('_', 5));
					
					int paramIndex = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
					for(int k = 0; k < paramNames.length; k++) {
						paramNames[k] = "p_" + methodIndex + "_" + paramIndex + "_";
						paramIndex += paramTypes[k].getSize();
					}
					
				} else {
					int paramIndex = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
					for(int k = 0; k < paramNames.length; k++) {
						paramNames[k] = "p_" + name + "_" + paramIndex + "_";
						paramIndex += paramTypes[k].getSize();
					}
				}
				
				final String[] paramNames2 = paramNames;
				
				return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
					
					Label start, end;
					
					@Override
					public void visitCode() {
						super.visitCode();
						super.visitLabel(start = new Label());
					}
					
					@Override
					public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
					}
					
					@Override
					public void visitParameter(String name, int access) {
					}
					
					@Override
					public void visitEnd() {
						super.visitLabel(end = new Label());
						
						int paramIndex = (access & Opcodes.ACC_STATIC) != 0 ? 0 : 1;
						for(int k = 0; k < paramNames2.length; k++) {
							super.visitLocalVariable(paramNames2[k], paramTypes[k].getDescriptor(), null, start, end, paramIndex);
							paramIndex += paramTypes[k].getSize();
						}
						
						super.visitEnd();
					}
				};
			}
		};
	}
}
