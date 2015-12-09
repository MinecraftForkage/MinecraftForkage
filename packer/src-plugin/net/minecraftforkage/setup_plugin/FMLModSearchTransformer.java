package net.minecraftforkage.setup_plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import net.minecraftforkage.instsetup.AbstractZipFile;
import net.minecraftforkage.instsetup.JarTransformer;
import net.minecraftforkage.instsetup.PackerContext;

public class FMLModSearchTransformer extends JarTransformer {

	private static class ProxyFieldInjectionData {
		String clientSideClass;
		String serverSideClass;
	}
	
	private static class ModInstanceInjectionData {
		String mod;
	}
	
	private static class FieldInjectionEntry {
		String className;
		String fieldName;
		String type;
		Object data;
		
		FieldInjectionEntry(String clazz, String field, String type, Object extraData) {
			this.className = clazz;
			this.fieldName = field;
			this.type = type;
			this.data = extraData;
		}
	}
	
	private List<Object> mods = new ArrayList<>();
	private Map<String, String> modClasses = new HashMap<>(); // class name -> mod ID
	
	// one of the few places in FML where the word "inject" is actually correct
	private List<FieldInjectionEntry> fieldsToInject = new ArrayList<>();
	
	private class ModSearchClassVisitor extends ClassVisitor {

		public ModSearchClassVisitor() {
			super(Opcodes.ASM5);
		}
		
		Map<String, Object> initDataObject = new HashMap<>();
		
		Map<String, Object> modObject = new HashMap<>();
		{
			modObject.put("initData", initDataObject);
		}
		
		boolean isMod = false;
		
		String className;
		
		@Override
		public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
			this.className = name.replace('/', '.');
		}
		
		@Override
		public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
			if(desc.equals("Lcpw/mods/fml/common/Mod;")) {
				return new AnnotationVisitor(Opcodes.ASM5) {
					
					Map<String, Object> fields = new HashMap<>();
					
					@Override
					public void visit(String name, Object value) {
						fields.put(name, value);
					}
					
					@Override
					public void visitEnd() {
						ArrayList<Object> dependencies = new ArrayList<>();
						ArrayList<Object> sortingRules = new ArrayList<>();
						
						String dependencyString = (String)fields.remove("dependencies");
						if(dependencyString != null) {
							for(String part : dependencyString.split(";")) {
								part = part.trim();
								int i = part.indexOf(":");
								if(i < 0)
									throw new RuntimeException("Mod "+fields.get("modid")+" has invalid dependency string "+dependencyString);
								
								String type = part.substring(0, i);
								String what = part.substring(i+1);
								
								Map<String, Object> parsedOtherMod = new HashMap<>();
								i = what.indexOf('@');
								if(i >= 0) {
									parsedOtherMod.put("mod", what.substring(0, i));
									parsedOtherMod.put("versionRange", what.substring(i+1));
								} else {
									parsedOtherMod.put("mod", what);
								}
								
								Map<String, Object> dependency = new HashMap<>();
								dependency.put("on", parsedOtherMod);
								if(type.startsWith("required-")) {
									type = type.substring(9);
									dependency.put("optional", false);
								} else {
									dependency.put("optional", true);
								}
								
								dependencies.add(dependency);
								
								Map<String, Object> sortingRule = new HashMap<>();
								sortingRule.put("mod", parsedOtherMod.get("mod"));
								sortingRule.put("type", type);
								
								if(!type.equals("after") && !type.equals("before"))
									throw new RuntimeException("Mod "+fields.get("modid")+" has invalid dependency string "+dependencyString);
								
								sortingRules.add(sortingRule);
							}
						}
						
						modObject.put("modContainerClass", "cpw.mods.fml.common.FMLContainer");
						modObject.put("modid", fields.remove("modid"));
						initDataObject.put("modAnnotation", fields);
						initDataObject.put("modClass", className);
						modObject.put("dependencies", dependencies);
						modObject.put("sortingRules", sortingRules);
						isMod = true;
					}
				};
			}
			
			if(desc.equals("Lcpw/mods/fml/common/API;") && className.endsWith(".package-info")) {
				return new AnnotationVisitor(Opcodes.ASM5) {
					
					Map<String, Object> fields = new HashMap<>();
					
					@Override
					public void visit(String name, Object value) {
						fields.put(name, value);
					}
					
					@Override
					public void visitEnd() {
						modObject.put("modid", fields.remove("provides"));
						initDataObject.put("modAnnotation", fields);
						initDataObject.put("package", className.substring(0, className.lastIndexOf('.')));
						
						if(fields.containsKey("owner") && !fields.get("owner").equals(modObject.get("modid"))) {
							Map<String, Object> ownerSortingRule = new HashMap<>();
							ownerSortingRule.put("mod", fields.get("owner"));
							ownerSortingRule.put("type", "after");
							modObject.put("sortingRules", Arrays.asList(ownerSortingRule));
						
						} else {
							modObject.put("sortingRules", Collections.emptyList());
						}
						
						modObject.put("modContainerClass", "cpw.mods.fml.common.ModAPIManager$APIContainer");
						modObject.put("dependencies", Collections.emptyList());
						
						isMod = true;
					}
				};
			}
			
			return null;
		}
		
		@Override
		public void visitEnd() {
			if(isMod) {
				mods.add(modObject);
				modClasses.put(className, (String)modObject.get("modid"));
			}
		}
		
		@Override
		public FieldVisitor visitField(int access, final String fieldName, String desc, String signature, Object value) {
			return new FieldVisitor(Opcodes.ASM5) {
				@Override
				public AnnotationVisitor visitAnnotation(String desc, boolean visible) {
					if(desc.equals("Lcpw/mods/fml/common/SidedProxy;")) {
						return new AnnotationVisitor(Opcodes.ASM5) {
							ProxyFieldInjectionData data = new ProxyFieldInjectionData();
							@Override
							public void visit(String name, Object value) {
								if(name.equals("clientSide")) data.clientSideClass = (String)value;
								if(name.equals("serverSide")) data.serverSideClass = (String)value;
							}
							@Override
							public void visitEnd() {
								fieldsToInject.add(new FieldInjectionEntry(className, fieldName, "sided-proxy", data));
							}
						};
					}
					if(desc.equals("Lcpw/mods/fml/common/Mod$Instance;")) {
						return new AnnotationVisitor(Opcodes.ASM5) {
							ModInstanceInjectionData data = new ModInstanceInjectionData();
							@Override
							public void visit(String name, Object value) {
								if(name.equals("value")) data.mod = (String)value;
							}
							@Override
							public void visitEnd() {
								fieldsToInject.add(new FieldInjectionEntry(className, fieldName, "mod-instance", data));
							}
						};
					}
					if(desc.equals("Lcpw/mods/fml/common/Mod$Metadata;")) {
						return new AnnotationVisitor(Opcodes.ASM5) {
							ModInstanceInjectionData data = new ModInstanceInjectionData();
							@Override
							public void visit(String name, Object value) {
								if(name.equals("value")) data.mod = (String)value;
							}
							@Override
							public void visitEnd() {
								fieldsToInject.add(new FieldInjectionEntry(className, fieldName, "mod-metadata", data));
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
		return "MinecraftForkage|FMLModFinder";
	}
	
	@Override
	public Stage getStage() {
		return Stage.MOD_IDENTIFICATION_STAGE;
	}

	@Override
	public void transform(AbstractZipFile zipFile, PackerContext context) throws Exception {
		for(String filename : zipFile.getFileNames()) {
			if(filename.endsWith(".class")) {
				try (InputStream in = zipFile.read(filename)) {
					new ClassReader(in).accept(new ModSearchClassVisitor(), ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG);
				}
			}
		}
		
		Map<String, String> classToSourceMap = zipFile.readProperties("mcforkage-class-to-source-map.properties");
		
		// fill in mod value for mod-instance injections where it's unknown
		// - it needs to be any mod from the same original JAR file
		for(FieldInjectionEntry obj : fieldsToInject)
			if(obj.data instanceof ModInstanceInjectionData) {
				ModInstanceInjectionData ifi = (ModInstanceInjectionData)obj.data;
				if(ifi.mod == null) {
					if(!classToSourceMap.containsKey(obj.className))
						throw new RuntimeException(obj.className+"."+obj.fieldName+" is marked @Instance(), but from an unknown source so we can't find a mod ID to fill it with");
					ifi.mod = findModBySource(classToSourceMap, classToSourceMap.get(obj.className));
					if(ifi.mod == null)
						throw new RuntimeException(obj.className+"."+obj.fieldName+" is marked @Instance(), but from a source with no mods so we can't find a mod ID to fill it with");
				}
			}
		
		zipFile.appendGSONArray("mcforkage-installed-mods.json", mods);
		zipFile.appendGSONArray("mcforkage-fields-to-inject.json", fieldsToInject);
	}

	private String findModBySource(Map<String, String> classToSourceMap, String source) {
		for(String modClass : modClasses.keySet())
			if(source.equals(classToSourceMap.get(modClass)))
				return modClasses.get(modClass);
		return null;
	}

}
