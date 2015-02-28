package net.mcforkage.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.util.CheckClassAdapter;

import net.minecraft.launchwrapper.IClassTransformer;

public class UndergroundBiomesConstructsTransformer implements IClassTransformer {
	// Only needed for 16-bit block IDs
	@Override
	public byte[] transform(String arg0, String arg1, byte[] arg2) {
		if(arg2 == null)
			return arg2;
		
		if(arg0.equals("exterminatorJeff.undergroundBiomes.worldGen.OreUBifier")) {
			ClassWriter cw = new ClassWriter(0);
			new ClassReader(arg2).accept(new ClassVisitor(Opcodes.ASM5, new CheckClassAdapter(cw)) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					if(name.equals("renewBlockReplacers"))
						return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
							public void visitIntInsn(int opcode, int operand) {
								if(operand == 4096) {
									super.visitLdcInsn(Integer.valueOf(65536));
								} else
									super.visitIntInsn(opcode, operand);
							}
						};
					return super.visitMethod(access, name, desc, signature, exceptions);
				}
			}, 0);
			return cw.toByteArray();
		}
		
		if(arg0.equals("exterminatorJeff.undergroundBiomes.worldGen.BiomeUndergroundDecorator")) {
			
			ClassWriter cw = new ClassWriter(0);
			new ClassReader(arg2).accept(new ClassVisitor(Opcodes.ASM5, cw) {
				@Override
				public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
					
					return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
						int state = 0;
						
						@Override
						public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
							if(owner.equals("net/minecraft/world/chunk/storage/ExtendedBlockStorage") && name.equals("func_76658_g")) {
								// getBlockLSBArray
								if(state != 0) throw new RuntimeException("can't tell what to transform");
								state = 1;
								
								super.visitFieldInsn(Opcodes.GETFIELD, owner, "block16BArray", "[S");
							
							} else if(owner.equals("net/minecraft/world/chunk/storage/ExtendedBlockStorage") && name.equals("func_76660_i")) {
								// getBlockMSBArray
								super.visitInsn(Opcodes.POP);
								super.visitInsn(Opcodes.ACONST_NULL);
								
							} else
								super.visitMethodInsn(opcode, owner, name, desc, itf);
						}
						
						@Override
						public void visitInsn(int opcode) {
							if(opcode == Opcodes.BALOAD && state == 1) {
								opcode = Opcodes.SALOAD;
								state = 0;
							}
							
							super.visitInsn(opcode);
						}
					};
				}
			}, 0);
			return cw.toByteArray();
		}
		
		return arg2;
	}
}
