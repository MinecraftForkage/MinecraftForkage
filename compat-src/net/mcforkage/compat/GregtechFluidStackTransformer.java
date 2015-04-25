package net.mcforkage.compat;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;

import net.minecraft.launchwrapper.IClassTransformer;

public class GregtechFluidStackTransformer implements IClassTransformer {

	@Override
	public byte[] transform(String arg0, String arg1, byte[] arg2) {
		if(arg2 == null || !arg0.equals("gregtech.api.objects.GT_FluidStack"))
			return arg2;
		
		ClassNode cn = new ClassNode();
		new ClassReader(arg2).accept(cn, 0);
		
		for(MethodNode mn : cn.methods) {
			if(mn.name.equals("fixFluidIDForFucksSake")) {
				mn.instructions.clear();
				mn.instructions.add(new InsnNode(Opcodes.RETURN));
				if(mn.localVariables != null) mn.localVariables.clear();
				if(mn.tryCatchBlocks != null) mn.tryCatchBlocks.clear();
			}
		}
		
		ClassWriter cw = new ClassWriter(0);
		cn.accept(cw);
		return cw.toByteArray();
	}

}
