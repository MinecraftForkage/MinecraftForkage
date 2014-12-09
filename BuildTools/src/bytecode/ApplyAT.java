package bytecode;

import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;


public class ApplyAT {
	
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
		if(args.length != 1) {
			System.err.println("Usage: java ApplyAT atfile.cfg < infile > outfile");
			System.exit(1);
		}
		
		try {
			
			List<Pattern> actions = loadActions(new FileReader(args[0]));
			
			try (ZipInputStream zipIn = new ZipInputStream(System.in)) {
				try (ZipOutputStream zipOut = new ZipOutputStream(System.out)) {
					ZipEntry ze;
					while((ze = zipIn.getNextEntry()) != null) {
						
						zipOut.putNextEntry(new ZipEntry(ze.getName()));
						
						if(!ze.getName().endsWith(".class")) {
							copyResource(zipIn, zipOut, ze);
						
						} else {
							// class file
							ClassWriter cw = new ClassWriter(0);
							new ClassReader(zipIn).accept(new ApplyATClassVisitor(cw, actions), 0);
							
							zipOut.write(cw.toByteArray());
						}
						
						zipOut.closeEntry();
					}
				}
			}
			
		} catch(Throwable t) {
			t.printStackTrace();
			System.exit(1);
		}
		
		System.exit(0);
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

	private static byte[] buffer = new byte[32768];
	private static void copyResource(ZipInputStream zipIn, ZipOutputStream zipOut, ZipEntry ze) throws IOException {
		while(true) {
			int read = zipIn.read(buffer);
			if(read <= 0)
				break;
			zipOut.write(buffer, 0, read);
		}
		zipIn.closeEntry();
	}
}
