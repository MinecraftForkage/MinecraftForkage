package cpw.mods.fml.common.asm.transformers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.logging.log4j.Level;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import cpw.mods.fml.common.FMLLog;
import cpw.mods.fml.common.asm.transformers.deobf.FMLRemappingAdapter;
import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.LaunchClassLoader;

/**
 * FML does this as part of the deobfuscation transformer (see {@link FMLRemappingAdapter}).
 * Some mods rely on it (mostly un-updated 1.7.2 mods running in 1.7.10?)
 * 
 * Ideally this should be removed.
 */
public class StaticFieldFixingTransformer implements IClassTransformer {
	private static Map<String, String> fieldTypes = new HashMap<String, String>();
	private static Set<String> seenClasses = new HashSet<>();
	
	@Override
	public byte[] transform(String arg0, String arg1, byte[] arg2) {
		if(arg0.startsWith("net.minecraft.")) {
			
			new ClassReader(arg2).accept(new ClassVisitor(Opcodes.ASM5) {
				String classInternalName;
				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					classInternalName = name;
					seenClasses.add(name);
					super.visit(version, access, name, signature, superName, interfaces);
				}
				@Override
				public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
					if((access & Opcodes.ACC_STATIC) != 0) {
						fieldTypes.put(classInternalName+"/"+name, desc);
					}
					
					return null;
				}
			}, ClassReader.SKIP_CODE);
			
			return arg2;
		
		} else {
			ClassWriter cw = new ClassWriter(0);
			new ClassReader(arg2).accept(new ClassVisitor(Opcodes.ASM5, cw) {
				
				String classInternalName;
				@Override
				public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
					classInternalName = name;
					super.visit(version, access, name, signature, superName, interfaces);
				}
				
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
						@Override
						public void visitFieldInsn(int opcode, String owner, String name, String desc) {
							if((opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC) && owner.startsWith("net/minecraft/")) {
								
								if(!seenClasses.contains(owner)) {
									try {
										StaticFieldFixingTransformer.class.getClassLoader().loadClass(owner.replace('/', '.'));
									} catch(ClassNotFoundException ex) {
										FMLLog.log(Level.ERROR, ex, "Failed to load "+owner+" referenced by "+classInternalName);
									}
								}
								
								String knownDesc = fieldTypes.get(owner+"/"+name);
								if(knownDesc != null)
									desc = knownDesc;
								
							}
							super.visitFieldInsn(opcode, owner, name, desc);
						}
					};
				}
			}, 0);
			return cw.toByteArray();
		}
	}
}
