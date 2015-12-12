package net.minecraftforkage.internal;

import net.minecraft.entity.player.EntityPlayer;
import squeek.applecore.api.IAppleCoreMutator;

public class AppleCoreMutator implements IAppleCoreMutator {

	@Override
	public void setExhaustion(EntityPlayer player, float exhaustion) {
		player.getFoodStats().setExhaustion(exhaustion);
	}

	@Override
	public void setHunger(EntityPlayer player, int hunger) {
		player.getFoodStats().setFoodLevel(hunger);
	}

	@Override
	public void setSaturation(EntityPlayer player, float saturation) {
		player.getFoodStats().setFoodSaturationLevel(saturation);
	}

	@Override
	public void setHealthRegenTickCounter(EntityPlayer player, int tickCounter) {
		// NOT IMPLEMENTED
	}

	@Override
	public void setStarveDamageTickCounter(EntityPlayer player, int tickCounter) {
		// NOT IMPLEMENTED
	}

}
