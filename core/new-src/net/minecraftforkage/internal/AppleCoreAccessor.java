package net.minecraftforkage.internal;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Items;
import net.minecraft.item.EnumAction;
import net.minecraft.item.Item;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemFood;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.common.MinecraftForge;
import squeek.applecore.api.IAppleCoreAccessor;
import squeek.applecore.api.food.FoodEvent;
import squeek.applecore.api.food.FoodValues;
import squeek.applecore.api.food.IEdible;
import squeek.applecore.api.hunger.ExhaustionEvent;
import squeek.applecore.api.hunger.HealthRegenEvent;
import squeek.applecore.api.hunger.StarvationEvent;

public class AppleCoreAccessor implements IAppleCoreAccessor {

	@Override
	public boolean isFood(ItemStack food) {
		if (food == null)
			return false; // AppleCore does this
		
		Item item = food.getItem();
		
		if(item == null)
			// AppleCore returns null
			throw new IllegalArgumentException("Argument is itemstack without item?");

		EnumAction action = item.getItemUseAction(food);
		
		//// TODO: give items a way to alter this check
		//if(item instanceof ItemBlock || action == EnumAction.eat || action == EnumAction.drink) {
		
		// Actually, why even have that check? What non-edible items are there that have food values?
		// (TODO: confirm this check can be safely removed, then remove this comment block)
		
			if(getUnmodifiedFoodValues(food) != null)
				return true;
		//}
		
		return false;
	}

	@Override
	public FoodValues getFoodValues(ItemStack stack) {
		FoodValues originalValues = getUnmodifiedFoodValues(stack);
		
		// TODO: AppleCore does this, but what if an event listener wants to add food values to a non-food item?
		if(originalValues == null)
			return null;
		
		FoodEvent.GetFoodValues event = new FoodEvent.GetFoodValues(stack, originalValues);
		MinecraftForge.EVENT_BUS.post(event);
		return event.foodValues;
	}

	@Override
	public FoodValues getFoodValuesForPlayer(ItemStack stack, EntityPlayer player) {
		FoodValues originalValues = getUnmodifiedFoodValues(stack);
		
		// TODO: AppleCore does this, but what if an event listener wants to add food values to a non-food item?
		if(originalValues == null)
			return null;
		
		FoodEvent.GetPlayerFoodValues event = new FoodEvent.GetPlayerFoodValues(player, stack, originalValues);
		MinecraftForge.EVENT_BUS.post(event);
		return event.foodValues;
	}

	@Override
	public FoodValues getUnmodifiedFoodValues(ItemStack stack) {
		if (stack == null)
			return null; // AppleCore does this
		
		Item item = stack.getItem();
		if(item == null)
			// AppleCore returns null
			throw new IllegalArgumentException("Argument is itemstack without item?");
		
		if(item instanceof IEdible)
			return ((IEdible)item).getFoodValues(stack);
		
		if(item instanceof ItemFood) {
			// TODO: make ItemFood implement IEdible (or add IFood)
			ItemFood asIF = (ItemFood)item;
			return new FoodValues(asIF.func_150905_g(stack), asIF.func_150906_h(stack));
		}
		
		if(item == Items.cake)
			// TODO: make cake item implement IEdible (or add IFood)
			return new FoodValues(2, 0.1f);
		
		return null;
	}

	private NBTTagCompound cachedTag = new NBTTagCompound();
	@Override
	public float getExhaustion(EntityPlayer player) {
		return player.getFoodStats().getExhaustion();
	}

	@Override
	public float getMaxExhaustion(EntityPlayer player) {
		ExhaustionEvent.GetMaxExhaustion event = new ExhaustionEvent.GetMaxExhaustion(player);
		MinecraftForge.EVENT_BUS.post(event);
		return event.maxExhaustionLevel;
	}

	@Override
	public int getHealthRegenTickPeriod(EntityPlayer player) {
		HealthRegenEvent.GetRegenTickPeriod event = new HealthRegenEvent.GetRegenTickPeriod(player);
		MinecraftForge.EVENT_BUS.post(event);
		return event.regenTickPeriod;
	}

	@Override
	public int getStarveDamageTickPeriod(EntityPlayer player) {
		StarvationEvent.GetStarveTickPeriod event = new StarvationEvent.GetStarveTickPeriod(player);
		MinecraftForge.EVENT_BUS.post(event);
		return event.starveTickPeriod;
	}

}
