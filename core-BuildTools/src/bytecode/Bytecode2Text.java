package bytecode;

import installer.ProgressDialog;

import java.io.InputStream;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.WeakHashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;


public class Bytecode2Text {
	static final boolean TRIM = false;
	
	public static void main(String[] args) {
		try {
			go(System.in, System.out, null);
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
	}
	
	public static void go(InputStream inBase, PrintStream out, ProgressDialog dlg) throws Exception {
		try (ZipInputStream in = new ZipInputStream(inBase)) {
			ZipEntry ze;
			while((ze = in.getNextEntry()) != null) {
				if(!ze.getName().endsWith(".class")) {
					in.closeEntry();
					continue;
				}
				
				if(dlg != null) dlg.incrementProgress(1);
				
				out.println("FILE "+ze.getName());
				new ClassReader(in).accept(new TranslateClassVisitor(out), 0);
			}
		}
		out.close();
	}
	
	private static List<Object> objectStack = new ArrayList<>();
	private static WeakHashMap<Object, Boolean> seen = new WeakHashMap<>();
	private static HashMap<Label, Integer> labelIDs = new HashMap<>();
	private static String getObjectID(Object object) {
		if(object instanceof Label) {
			Integer id = labelIDs.get(object);
			if(id == null) {
				labelIDs.put((Label)object, id = labelIDs.size());
				return "NEWLABEL"+id;
			}
			return "LABEL"+id;
		}
		if(objectStack.size() > 0) {
			if(objectStack.get(objectStack.size() - 1) == object)
				return "TOP";
			
			if(objectStack.size() > 1 && objectStack.get(objectStack.size() - 2) == object) {
				objectStack.remove(objectStack.size() - 1);
				return "POP";
			}
		}
		if(seen.put(object, false) != null)
			throw new RuntimeException(object+" seen before, not on stack top");
		objectStack.add(object);
		return "PUSH";
		//Integer objectID = objectIDs.get(object);
		//if(objectID == null) objectIDs.put(object, objectID = nextObjectID++);
		//return objectID;
	}
	
	static void resetLabelIDs() {
		labelIDs.clear();
	}
	
	private static void logArg(PrintStream out, Object arg) {
	
		if(arg instanceof String) {
			try {
				out.println("S"+((String)arg).length()+" "+URLEncoder.encode((String)arg, "UTF-8"));
			} catch (UnsupportedEncodingException e) {
				throw new RuntimeException(e);
			}
			return;
		}
		
		if(arg instanceof Integer) {
			out.println("I"+arg);
			return;
		}
		
		if(arg instanceof Object[]) {
			out.println("A"+((Object[])arg).length+" "+arg.getClass().getName());
			for(Object o : (Object[])arg)
				logArg(out, o);
			return;
		}
		
		if(arg instanceof int[]) {
			out.println("A"+((int[])arg).length+" [I");
			for(int o : (int[])arg)
				logArg(out, o);
			return;
		}
		
		if(arg == null) {
			out.println("_null");
			return;
		}
		
		if(arg instanceof Boolean) {
			out.println((Boolean)arg ? "_true" : "_false");
			return;
		}
		
		if(arg instanceof Double) {
			out.println("D" + Double.doubleToRawLongBits((Double)arg));
			return;
		}
		
		if(arg instanceof Float) {
			out.println("F" + Float.floatToRawIntBits((Float)arg));
			return;
		}
		
		if(arg instanceof Type) {
			out.println("T" + ((Type)arg).getDescriptor());
			return;
		}
		
		if(arg instanceof Long) {
			out.println("J" + ((Long)arg));
			return;
		}
		
		if(arg instanceof Label) {
			out.println("L" + getObjectID(arg));
			return;
		}
		
		throw new RuntimeException(arg+" "+arg.getClass().getName());
	}
	
	static void logVoid(PrintStream out, Object object, String methodName, Object... args) {
		out.println("CALL "+getObjectID(object)+" "+methodName+" X "+args.length);
		for(Object arg : args)
			logArg(out, arg);
	}
	
	static <T> T log(PrintStream out, Object object, String methodName, T _return, Object... args) {
		if(_return == null)
			throw new NullPointerException("_return");
		out.println("CALL "+getObjectID(object)+" "+methodName+" "+getObjectID(_return)+" "+args.length);
		for(Object arg : args)
			logArg(out, arg);
		return _return;
	}
	
	private static class TranslateAnnotationVisitor extends AnnotationVisitor {
		private PrintStream out;
		public TranslateAnnotationVisitor(PrintStream out) {
			super(Opcodes.ASM5);
			this.out = out;
		}
		
		@Override
		public void visit(String name, Object value) {
			logVoid(out, this, "visit", name, value);
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String name, String desc) {
			return log(out, this, "visitAnnotation", new TranslateAnnotationVisitor(out), name, desc);
		}
		
		@Override
		public AnnotationVisitor visitArray(String name) {
			return log(out, this, "visitArray", new TranslateAnnotationVisitor(out), name);
		}
		
		@Override
		public void visitEnd() {
			logVoid(out, this, "visitEnd");
		}
		
		@Override
		public void visitEnum(String name, String desc, String value) {
			logVoid(out, this, "visitEnum", name, desc, value);
		}
	}
	
	private static class TranslateFieldVisitor extends FieldVisitor {
		private PrintStream out;
		public TranslateFieldVisitor(PrintStream out) {
			super(Opcodes.ASM5);
			this.out = out;
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return log(out, this, "visitAnnotation", new TranslateAnnotationVisitor(out), desc, visible);
		}
		
		@Override
		public void visitEnd() {
			logVoid(out, this, "visitEnd");
		}
		
		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			return log(out, this, "visitTypeAnnotation", new TranslateAnnotationVisitor(out), typeRef, typePath, desc, visible);
		}
	}
	
	private static class TranslateMethodVisitor extends MethodVisitor {
		private PrintStream out;
		public TranslateMethodVisitor(PrintStream out) {
			super(Opcodes.ASM5);
			this.out = out;
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return log(out, this, "visitAnnotation", new TranslateAnnotationVisitor(out), desc, visible);
		}
		
		@Override
		public AnnotationVisitor visitAnnotationDefault() {
			return log(out, this, "visitAnnotationDefault", new TranslateAnnotationVisitor(out));
		}
		
		@Override
		public void visitCode() {
			logVoid(out, this, "visitCode");
		}
		
		@Override
		public void visitEnd() {
			logVoid(out, this, "visitEnd");
		}
		
		@Override
		public void visitFieldInsn(int opcode, String owner, String name, String desc) {
			logVoid(out, this, "visitFieldInsn", opcode, owner, name, desc);
		}
		
		@Override
		public void visitFrame(int type, int nLocal, Object[] local, int nStack, Object[] stack) {
			logVoid(out, this, "visitFrame", type, nLocal, local, nStack, stack);
		}
		@Override
		public void visitIincInsn(int var, int increment) {
			logVoid(out, this, "visitIincInsn", var, increment);
		}
		
		@Override
		public void visitInsn(int opcode) {
			logVoid(out, this, "visitInsn", opcode);
		}
		
		@Override
		public AnnotationVisitor visitInsnAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			return log(out, this, "visitInsnAnnotation", new TranslateAnnotationVisitor(out), typeRef, typePath, desc, visible);
		}
		@Override
		public void visitIntInsn(int opcode, int operand) {
			logVoid(out, this, "visitIntInsn", opcode, operand);
		}
		
		@Override
		public void visitInvokeDynamicInsn(String name, String desc, Handle bsm, Object... bsmArgs) {
			logVoid(out, this, "visitInvokeDynamicInsn", name, desc, bsm, bsmArgs);
		}
		
		@Override
		public void visitJumpInsn(int opcode, Label label) {
			logVoid(out, this, "visitJumpInsn", opcode, label);
		}
		
		@Override
		public void visitLabel(Label label) {
			logVoid(out, this, "visitLabel", label);
		}
		
		@Override
		public void visitLdcInsn(Object cst) {
			logVoid(out, this, "visitLdcInsn", cst);
		}
		
		@Override
		public void visitLineNumber(int line, Label start) {
			if(!TRIM)
				logVoid(out, this, "visitLineNumber", line, start);
		}
		
		@Override
		public void visitLocalVariable(String name, String desc, String signature, Label start, Label end, int index) {
			if(!TRIM)
				logVoid(out, this, "visitLocalVariable", name, desc, signature, start, end, index);
		}
		
		@Override
		public AnnotationVisitor visitLocalVariableAnnotation(int typeRef, TypePath typePath, Label[] start, Label[] end, int[] index, String desc, boolean visible) {
			return log(out, this, "visitLocalVariableAnnotation", new TranslateAnnotationVisitor(out), typePath, start, end, index, desc, visible);
		}
		
		@Override
		public void visitLookupSwitchInsn(Label dflt, int[] keys, Label[] labels) {
			logVoid(out, this, "visitLookupSwitchInsn", dflt, keys, labels);
		}
		
		@Override
		public void visitMaxs(int maxStack, int maxLocals) {
			logVoid(out, this, "visitMaxs", maxStack, maxLocals);
		}
		
		@Override
		public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
			logVoid(out, this, "visitMethodInsn", opcode, owner, name, desc, itf);
		}
		
		@Override
		public void visitMultiANewArrayInsn(String desc, int dims) {
			logVoid(out, this, "visitMultiANewArrayInsn", desc, dims);
		}
		
		@Override
		public void visitParameter(String name, int access) {
			logVoid(out, this, "visitParameter", name, access);
		}
		
		@Override
		public AnnotationVisitor visitParameterAnnotation(int parameter, String desc, boolean visible) {
			return log(out, this, "visitParameterAnnotation", new TranslateAnnotationVisitor(out), parameter, desc, visible);
		}
		
		@Override
		public void visitTableSwitchInsn(int min, int max, Label dflt, Label... labels) {
			logVoid(out, this, "visitTableSwitchInsn", min, max, dflt, labels);
		}
		
		@Override
		public AnnotationVisitor visitTryCatchAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			return log(out, this, "visitTryCatchAnnotation", new TranslateAnnotationVisitor(out), typeRef, typePath, desc, visible);
		}
		
		@Override
		public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
			logVoid(out, this, "visitTryCatchBlock", start, end, handler, type);
		}
		
		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			return log(out, this, "visitTypeAnnotation", new TranslateAnnotationVisitor(out), typeRef, typePath, desc, visible);
		}
		
		@Override
		public void visitTypeInsn(int opcode, String type) {
			logVoid(out, this, "visitTypeInsn", opcode, type);
		}
		
		@Override
		public void visitVarInsn(int opcode, int var) {
			logVoid(out, this, "visitVarInsn", opcode, var);
		}
	}
	
	private static class TranslateClassVisitor extends ClassVisitor {
		private PrintStream out;
		public TranslateClassVisitor(PrintStream out) {
			super(Opcodes.ASM5);
			this.out = out;
		}
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			logVoid(out, this, "visit", version, access, name, signature, superName, interfaces);
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			return log(out, this, "visitAnnotation", new TranslateAnnotationVisitor(out), desc, visible);
		}
		
		@Override
		public void visitEnd() {
			logVoid(out, this, "visitEnd");
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			return log(out, this, "visitField", new TranslateFieldVisitor(out), access, name, desc, signature, value);
		}
		
		@Override
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			logVoid(out, this, "visitInnerClass", name, outerName, innerName, access);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			resetLabelIDs();
			return log(out, this, "visitMethod", new TranslateMethodVisitor(out), access, name, desc, signature, exceptions);
		}
		
		@Override
		public void visitOuterClass(String owner, String name, String desc) {
			logVoid(out, this, "visitOuterClass", owner, name, desc);
		}
		
		@Override
		public void visitSource(String source, String debug) {
			if(!TRIM)
				logVoid(out, this, "visitSource", source, debug);
		}
		
		@Override
		public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String desc, boolean visible) {
			return log(out, this, "visitTypeAnnotation", new TranslateAnnotationVisitor(out), typeRef, typePath, desc, visible);
		}
	}
}
