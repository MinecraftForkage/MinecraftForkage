package net.minecraftforkage.setup_plugin;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.minecraftforkage.instsetup.AbstractZipFile;
import net.minecraftforkage.instsetup.JarTransformer;
import net.minecraftforkage.instsetup.PackerContext;

public class OptionalTransformer extends JarTransformer {
	
	boolean DEBUG = Boolean.getBoolean("minecraftforkage.OptionalTransformer.debug");
	
	static class OptionalInterfaceRecord {
		String modid;
		String iface;
		boolean striprefs;
	}
	
	static class OptionalMethodRecord {
		String modid;
		String methodName;
		String methodDesc;
	}
	
	private static class OptionalRemovalClassVisitor extends ClassVisitor {
		public OptionalRemovalClassVisitor() {
			super(Opcodes.ASM5);
		}
		
		String className;
		
		List<OptionalInterfaceRecord> interfaces = new ArrayList<>();
		List<OptionalMethodRecord> methods = new ArrayList<>();
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = name;
		}
		
		private class OptionalInterfaceAnnotationVisitor extends AnnotationVisitor {
			public OptionalInterfaceAnnotationVisitor() {
				super(Opcodes.ASM5);
			}
			
			OptionalInterfaceRecord record = new OptionalInterfaceRecord();
			
			@Override
			public void visit(String name, Object value) {
				switch(name) {
				case "iface": record.iface = (String)value; break;
				case "modid": record.modid = (String)value; break;
				case "striprefs": record.striprefs = (Boolean)value; break;
				default: throw new RuntimeException("Unknown annotation item "+name+" on Optional.Interface annotation on "+className);
				}
			}

			@Override
			public void visitEnd() {
				interfaces.add(record);
			}
		}

		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if(desc.startsWith("Lcpw/mods/fml/common/Optional")) {
				if(desc.equals("Lcpw/mods/fml/common/Optional$Interface;")) {
					return new OptionalInterfaceAnnotationVisitor();
				
				} else if(desc.equals("Lcpw/mods/fml/common/Optional$InterfaceList;")) {
					return new AnnotationVisitor(Opcodes.ASM5) {
						@Override
						public AnnotationVisitor visitArray(String name) {
							if(!name.equals("value"))
								throw new RuntimeException("Unknown annotation item "+name+" on Optional.InterfaceList annotation on "+className);
							return this;
						}
						@Override
						public AnnotationVisitor visitAnnotation(String name,String desc) {
							if(name != null || !desc.equals("Lcpw/mods/fml/common/Optional$Interface;"))
								throw new RuntimeException("Unknown annotation item "+name+" of type "+desc+" on Optional.InterfaceList annotation on "+className);
							return new OptionalInterfaceAnnotationVisitor();
						}
					};
				
				} else
					throw new RuntimeException("saw unknown Optional annotation "+desc+" on "+className);
			}
			return null;
		}
		
		@Override
		public MethodVisitor visitMethod(int access, final String methodName, final String methodDesc, String signature, String[] exceptions) {
			return new MethodVisitor(Opcodes.ASM5) {
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					if(desc.equals("Lcpw/mods/fml/common/Optional$Method;")) {
						final OptionalMethodRecord record = new OptionalMethodRecord();
						record.methodName = methodName;
						record.methodDesc = methodDesc;
						return new AnnotationVisitor(Opcodes.ASM5) {
							@Override
							public void visit(String name, Object value) {
								if(name.equals("modid")) record.modid = (String)value;
								else throw new RuntimeException("Unknown annotation item "+name+" on Optional.Method on "+className+"."+methodName);
							}
							@Override
							public void visitEnd() {
								methods.add(record);
							}
						};
						
					} else if(desc.startsWith("Lcpw/mods/fml/common/Optional")) {
						throw new RuntimeException("saw unknown Optional annotation "+desc+" on "+className+"."+methodName);
					}
					return null;
				}
			};
		}
	}
	
	@Override
	public String getID() {
		return "MinecraftForkage|OptionalInterfaceTransformer";
	}
	
	@Override
	public void transform(AbstractZipFile zipFile, PackerContext context) throws Exception {
		
		Set<String> installedModIDs = new HashSet<>();
		
		JsonArray installedModsArray = zipFile.readGSON("mcforkage-installed-mods.json", JsonArray.class);
		for(JsonElement e : installedModsArray)
			installedModIDs.add(e.getAsJsonObject().get("modid").getAsString());
		
		for(String filename : zipFile.getFileNames()) {
			if(filename.endsWith(".class")) {
				ClassWriter cw = new ClassWriter(0);
				ClassNode cn = new ClassNode();
				try (InputStream in = zipFile.read(filename)) {
					new ClassReader(in).accept(cn, 0);
				}
				
				OptionalRemovalClassVisitor cv = new OptionalRemovalClassVisitor();
				cn.accept(cv);
				
				if(cv.methods.size() == 0 && cv.interfaces.size() == 0)
					continue;
				
				
				for(OptionalInterfaceRecord optIntf : cv.interfaces) {
					if(!installedModIDs.contains(optIntf.modid)) {
						if(DEBUG)
							System.out.println(optIntf.modid+" is not installed; removing "+optIntf.iface+" from "+cv.className);
						if(!cn.interfaces.remove(optIntf.iface.replace('.', '/')))
							System.err.println("Can't remove interface "+optIntf.iface+" from "+cv.className+" as it isn't there.");
						
						if(optIntf.striprefs) {
							// Remove any method whose signature mentions optIntf.iface
							
							String interfaceDescriptor = "L" + optIntf.iface.replace('.', '/') + ";";
							
							Iterator<MethodNode> it = cn.methods.iterator();
							while(it.hasNext()) {
								MethodNode method = it.next();
								if(method.desc.contains(interfaceDescriptor))
									it.remove();
							}
						}
						
					} else {
						if(DEBUG)
							System.out.println(optIntf.modid+" is installed; not removing "+optIntf.iface+" from "+cv.className);
					}
				}
				
				for(OptionalMethodRecord optMethod : cv.methods) {
					if(!installedModIDs.contains(optMethod.modid)) {
						if(DEBUG)
							System.out.println(optMethod.modid+" is not installed; removing "+optMethod.methodName+optMethod.methodDesc+" from "+cv.className);
						
						boolean found = false;
						
						Iterator<MethodNode> it = cn.methods.iterator();
						while(it.hasNext()) {
							MethodNode method = it.next();
							if(method.name.equals(optMethod.methodName) && method.desc.equals(optMethod.methodDesc)) {
								it.remove();
								found = true;
							}
						}
						
						if(!found)
							throw new AssertionError("Method to remove "+optMethod.methodName+optMethod.methodDesc+" not found in "+cv.className);
						
					} else {
						if(DEBUG)
							System.out.println(optMethod.modid+" is installed; not removing "+optMethod.methodName+" from "+cv.className);
					}
				}
				
				
				cn.accept(cw);
				
				try (OutputStream out = zipFile.write(filename)) {
					out.write(cw.toByteArray());
				}
			}
		}
	}
}
