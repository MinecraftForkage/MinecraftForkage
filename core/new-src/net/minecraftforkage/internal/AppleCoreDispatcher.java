package net.minecraftforkage.internal;

import java.util.Random;

import net.minecraft.block.Block;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.eventhandler.Event.Result;
import squeek.applecore.api.IAppleCoreDispatcher;
import squeek.applecore.api.plants.PlantGrowthEvent;

public class AppleCoreDispatcher implements IAppleCoreDispatcher {

	@Override
	public Result validatePlantGrowth(Block block, World world, int x, int y, int z, Random random) {
		PlantGrowthEvent.AllowGrowthTick event = new PlantGrowthEvent.AllowGrowthTick(block, world, x, y, z, random);
		MinecraftForge.EVENT_BUS.post(event);
		return event.getResult();
	}

	@Override
	public void announcePlantGrowth(Block block, World world, int x, int y, int z, int previousMetadata) {
		MinecraftForge.EVENT_BUS.post(new PlantGrowthEvent.GrowthTick(block, world, x, y, z, previousMetadata));
	}

	@Override
	public void announcePlantGrowthWithoutMetadataChange(Block block, World world, int x, int y, int z) {
		// XXX: This method is ugly?
		announcePlantGrowth(block, world, x, y, z, world.getBlockMetadata(x, y, z));
	}

	@Override
	public void announcePlantGrowth(Block block, World world, int x, int y, int z) {
		// XXX: This method is ugly.
		announcePlantGrowth(block, world, x, y, z, Math.max(0, world.getBlockMetadata(x, y, z) - 1));
	}

}
