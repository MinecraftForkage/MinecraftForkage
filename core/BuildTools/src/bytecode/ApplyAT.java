package bytecode;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ApplyAT extends BaseStreamingJarProcessor {
	
	public static class Pattern {
		String clazz; // internal name
		String object; // field name, or method name+descriptor
		String action;
		
		
		boolean matches(String clazz, String object) {
			if(!this.clazz.equals(clazz))
				return false;
			
			if(this.object == null)
				return object == null;
			
			if(object == null)
				return false;
			
			if(this.object.equals("*"))
				return !object.contains("(");
			
			if(this.object.equals("*()"))
				return object.contains("(");
			
			return this.object.equals(object);
		}
		
		int changeAccess(int old) {
			String action = this.action;
			if(action.endsWith("-f")) {
				action = action.substring(0, action.length() - 2);
				old &= ~Opcodes.ACC_FINAL;
			
			} else if(action.endsWith("+f")) {
				action = action.substring(0, action.length() - 2);
				old |= Opcodes.ACC_FINAL;
			}
			
			int oldAcc = old & (Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED | Opcodes.ACC_PRIVATE);
			
			switch(action) {
			case "public":
				old &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED);
				old |= Opcodes.ACC_PUBLIC;
				break;
			case "protected":
				if(oldAcc == Opcodes.ACC_PRIVATE || oldAcc == 0) {
					old &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC);
					old |= Opcodes.ACC_PROTECTED;
				}
				break;
			case "default":
				if(oldAcc == Opcodes.ACC_PRIVATE)
					old &= ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PUBLIC | Opcodes.ACC_PROTECTED);
				break;
			default: throw new RuntimeException("unknown action for "+clazz+"/"+object+": "+this.action);
			}
			
			return old;
		}
	}
	
	public static List<Pattern> loadActions(Reader file) throws Exception {
		List<Pattern> actions = new ArrayList<>();
		try (Scanner s = new Scanner(file)) {
			while(s.hasNextLine()) {
				String line = s.nextLine();
				if(line.contains("#"))
					line = line.substring(0, line.indexOf('#'));
				line = line.trim();
				if(line.equals(""))
					continue;
				
				String[] parts = line.split(" ");
				
				Pattern p = new Pattern();
				p.action = parts[0];
				
				p.clazz = parts[1].replace('.', '/');
				
				if(parts.length < 3)
					p.object = null;
				else if(parts.length == 3)
					p.object = parts[2].replace('.', '/');
				else
					throw new Exception("unparseable line: "+line);
				
				actions.add(p);
			}
		}
		return actions;
	}
	
	public static void main(String[] args) {
		new ApplyAT().go(args);
	}
	
	private List<Pattern> actions;
	@Override
	public void loadConfig(Reader file) throws Exception {
		actions = loadActions(file);
	}
	
	@Override
	public ClassVisitor createClassVisitor(ClassVisitor parent) throws Exception {
		return new ApplyATClassVisitor(parent, actions);
	}
	
	static int changeAccess(List<Pattern> actions, int old, String clazz, String object) {
		for(Pattern p : actions)
			if(p.matches(clazz, object))
				old = p.changeAccess(old);
		return old;
	}
	
	public static class ApplyATClassVisitor extends ClassVisitor {
		
		private List<Pattern> actions;
		
		public ApplyATClassVisitor(ClassVisitor parent, List<Pattern> actions) {
			super(Opcodes.ASM5, parent);
			this.actions = actions;
		}
		
		String classInternalName;
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			access = changeAccess(actions, access, name, null);
			classInternalName = name;
			super.visit(version, access, name, signature, superName, interfaces);
		}
		
		@Override
		public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
			access = changeAccess(actions, access, classInternalName, name);
			return super.visitField(access, name, desc, signature, value);
		}
		
		@Override
		public void visitInnerClass(String name, String outerName, String innerName, int access) {
			int newAccess = changeAccess(actions, access, name, null);
			super.visitInnerClass(name, outerName, innerName, newAccess);
		}
		
		@Override
		public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
			access = changeAccess(actions, access, classInternalName, name+desc);
			return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
				@Override
				public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
					
					// if calling a method which was private but now isn't, replace INVOKESPECIAL with INVOKEVIRTUAL
					// check this by calling changeAccess with ACC_PRIVATE and checking if the result still has ACC_PRIVATE
					if(opcode == Opcodes.INVOKESPECIAL && owner.equals(classInternalName) && !name.contains("<")) {
						int methodNewAccess = changeAccess(actions, Opcodes.ACC_PRIVATE, classInternalName, name+desc);
						if((methodNewAccess & Opcodes.ACC_PRIVATE) == 0)
							opcode = Opcodes.INVOKEVIRTUAL;
					}
					super.visitMethodInsn(opcode, owner, name, desc, itf);
				}
			};
		}
	}
}
