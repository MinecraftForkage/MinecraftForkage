package net.mcforkage.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraft.launchwrapper.Launch;

public class HardcoreEnderExpansionTransformer implements IClassTransformer {
	@Override
	public byte[] transform(String arg0, String arg1, byte[] arg2) {
		if(arg2 == null || !arg0.equals("chylex.hee.system.logging.Log"))
			return arg2;
		
		ClassWriter cw = new ClassWriter(0);
		new ClassReader(arg2).accept(new ClassVisitor(Opcodes.ASM5, cw) {
			@Override
			public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
				if(!name.equals("<clinit>"))
					return super.visitMethod(access, name, desc, signature, exceptions);
				return new MethodVisitor(Opcodes.ASM5, super.visitMethod(access, name, desc, signature, exceptions)) {
					@Override
					public void visitLdcInsn(Object cst) {
						if("fml.deobfuscatedEnvironment".equals(cst)) {
							String NEW_KEY = "chylex.hee.isDevEnvironment";
							if(!Launch.blackboard.containsKey(NEW_KEY))
								Launch.blackboard.put(NEW_KEY, Boolean.FALSE);
							cst = NEW_KEY;
						}
						super.visitLdcInsn(cst);
					}
				};
			}
		}, 0);
		return cw.toByteArray();
	}
}
