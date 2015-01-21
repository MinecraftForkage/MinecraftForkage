package net.minecraftforkage.setup_plugin;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
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

public class FMLModSearchTransformer extends JarTransformer {

	static class SidedProxyInfo {
		String className;
		String fieldName;
		String clientSideClass;
		String serverSideClass;
		String type = "sided-proxy";
	}
	
	static class InstanceFieldInfo {
		String className;
		String fieldName;
		String mod;
		String type = "mod-instance";
	}
	
	private List<Object> mods = new ArrayList<>();
	private Map<String, String> modClasses = new HashMap<>(); // class name -> mod ID
	
	// one of the few places in FML where the word "inject" is actually correct
	private List<Object> fieldsToInject = new ArrayList<>();
	
	private class ModSearchClassVisitor extends ClassVisitor {

		public ModSearchClassVisitor() {
			super(Opcodes.ASM5);
		}
		
		Map<String, Object> initDataObject = new HashMap<>();
		
		Map<String, Object> modObject = new HashMap<>();
		{
			modObject.put("modContainerClass", "cpw.mods.fml.common.FMLContainer");
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
						
						modObject.put("modid", fields.remove("modid"));
						initDataObject.put("modAnnotation", fields);
						modObject.put("dependencies", dependencies);
						modObject.put("sortingRules", sortingRules);
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
							SidedProxyInfo spi = new SidedProxyInfo();
							{
								spi.className = className;
								spi.fieldName = fieldName;
							}
							@Override
							public void visit(String name, Object value) {
								if(name.equals("clientSide")) spi.clientSideClass = (String)value;
								if(name.equals("serverSide")) spi.serverSideClass = (String)value;
							}
							@Override
							public void visitEnd() {
								fieldsToInject.add(spi);
							}
						};
					}
					if(desc.equals("Lcpw/mods/fml/common/Mod$Instance;")) {
						return new AnnotationVisitor(Opcodes.ASM5) {
							InstanceFieldInfo ifi = new InstanceFieldInfo();
							{
								ifi.className = className;
								ifi.fieldName = fieldName;
							}
							@Override
							public void visit(String name, Object value) {
								if(name.equals("value")) ifi.mod = (String)value;
							}
							@Override
							public void visitEnd() {
								fieldsToInject.add(ifi);
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
		return Stage.CLASS_INFO_EXTRACTION_STAGE;
	}

	@Override
	public void transform(AbstractZipFile zipFile) throws Exception {
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
		for(Object obj : fieldsToInject)
			if(obj instanceof InstanceFieldInfo) {
				InstanceFieldInfo ifi = (InstanceFieldInfo)obj;
				if(ifi.mod == null) {
					if(!classToSourceMap.containsKey(ifi.className))
						throw new RuntimeException(ifi.className+"."+ifi.fieldName+" is marked @Instance(), but from an unknown source so we can't find a mod ID to fill it with");
					ifi.mod = findModBySource(classToSourceMap, classToSourceMap.get(ifi.className));
					if(ifi.mod == null)
						throw new RuntimeException(ifi.className+"."+ifi.fieldName+" is marked @Instance(), but from a source with no mods so we can't find a mod ID to fill it with");
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
