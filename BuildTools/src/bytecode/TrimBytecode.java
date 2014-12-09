package bytecode;
import java.io.Reader;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class TrimBytecode extends BaseStreamingJarProcessor {
	public static void main(String[] args) {
		new TrimBytecode().go(args);
	}
	
	@Override
	protected boolean hasConfig() {
		return false;
	}

	@Override
	protected void loadConfig(Reader file) throws Exception {
	}

	@Override
	public ClassVisitor createClassVisitor(ClassVisitor parent) throws Exception {
		return new ClassVisitor(Opcodes.ASM5, parent) {
			@Override
			public void visitSource(String source, String debug) {
			}
			
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
					@Override
					public void visitLineNumber(int line, Label start) {
					}
					@Override
					public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
					}
					@Override
					public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
						// frames are important, but not for diffing
					}
				};
			}
			
			@Override
			public void visitInnerClass(String name, String outerName, String innerName, int access) {
			}
			
			@Override
			public void visitOuterClass(String owner, String name, String desc) {
			}
		};
	}
}
