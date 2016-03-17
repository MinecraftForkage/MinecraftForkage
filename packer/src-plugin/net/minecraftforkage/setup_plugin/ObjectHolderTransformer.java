package net.minecraftforkage.setup_plugin;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Opcodes;

import net.minecraftforkage.instsetup.AbstractZipFile;
import net.minecraftforkage.instsetup.JarTransformer;
import net.minecraftforkage.instsetup.PackerContext;

public class ObjectHolderTransformer extends JarTransformer {

	private static class ObjectHolderEntry {
		public String objectID;
		public String className;
		public String fieldName;
	}
	
	ArrayList<ObjectHolderEntry> entries = new ArrayList<ObjectHolderEntry>();
	
	private class ObjectHolderClassVisitor extends ClassVisitor {

		public ObjectHolderClassVisitor() {
			super(Opcodes.ASM5);
		}
		
		private String className;
		private String classAnnotationValue;
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			className = name;
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if(desc.equals("Lcpw/mods/fml/common/registry/GameRegistry$ObjectHolder;")) {
				return new AnnotationVisitor(Opcodes.ASM5) {
					@Override
					public void visit(String name, Object value) {
						if(name.equals("value")) classAnnotationValue = (String)value;
					}
					@Override
					public void visitEnd() {
						if(classAnnotationValue == null)
							throw new AssertionError("ObjectHolder class annotation value not visited? in " + className);
					}
				};
			}
			return null;
		}
	
		@Override
		public FieldVisitor visitField(int access, final String fieldName, final String fieldDesc, String signature, Object value) {
			return new FieldVisitor(Opcodes.ASM5) {
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					if(desc.equals("Lcpw/mods/fml/common/registry/GameRegistry$ObjectHolder;")) {
						return new AnnotationVisitor(Opcodes.ASM5) {
							String value = null;
							@Override
							public void visit(String name, Object value) {
								if(name.equals("value"))
									this.value = (String)value;
							}
							@Override
							public void visitEnd() {
								if(this.value == null)
									throw new AssertionError("ObjectHolder field annotation value not visited? in " + className+"."+fieldName);
								
								ObjectHolderEntry entry = new ObjectHolderEntry();
								entry.className = className;
								entry.fieldName = fieldName;
								if(this.value.contains(":"))
									entry.objectID = this.value;
								else
								{
									if(classAnnotationValue == null)
										throw new RuntimeException("Field "+className+"."+fieldName+" uses abbreviated ObjectHolder name "+this.value+" but class has no @ObjectHolder");
									entry.objectID = classAnnotationValue+":"+this.value;
								}
								entries.add(entry);
							}
						};
					}
					return null;
				}
			};
		}
	}
	
	@Override
	public String getID() {
		return "MinecraftForkage|ObjectHolderExtractor";
	}
	
	@Override
	public Stage getStage() {
		return Stage.CLASS_INFO_EXTRACTION_STAGE;
	}

	@Override
	public void transform(AbstractZipFile zipFile, PackerContext context) throws Exception {
		for(String filename : zipFile.getFileNames()) {
			if(filename.endsWith(".class")) {
				try (InputStream in = zipFile.read(filename)) {
					new ClassReader(in).accept(new ObjectHolderClassVisitor(), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
				}
			}
		}
		
		zipFile.appendGSONArray("mcforkage-object-holders.json", entries);
	}

}
