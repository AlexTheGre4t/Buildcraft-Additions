package buildcraftAdditions.blocks.multiBlocks;

import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.world.World;

import buildcraftAdditions.multiBlocks.MultiBlockPaternKEBT3;
import buildcraftAdditions.reference.Variables;
import buildcraftAdditions.tileEntities.Bases.TileKineticEnergyBufferBase;
import buildcraftAdditions.tileEntities.TileKEBT3;
import buildcraftAdditions.utils.PlayerUtils;

/**
 * Copyright (c) 2014-2015, AEnterprise
 * http://buildcraftadditions.wordpress.com/
 * Buildcraft Additions is distributed under the terms of GNU GPL v3.0
 * Please check the contents of the license located in
 * http://buildcraftadditions.wordpress.com/wiki/licensing-stuff/
 */
public class MultiBlockKEBT3Core extends MultiBlockBase {

	public MultiBlockKEBT3Core() {
		super("blockKEBT3Core", Variables.Identifiers.KEBT3_CORE, new MultiBlockPaternKEBT3(), "KEB/T3/core");
	}

	@Override
	public TileEntity createNewTileEntity(World world, int meta) {
		return new TileKEBT3();
	}

	@Override
	public void onBlockPlacedBy(World world, int x, int y, int z, EntityLivingBase entity, ItemStack stack) {
		if (entity instanceof EntityPlayer) {
			TileEntity tileEntity = world.getTileEntity(x, y, z);
			if (tileEntity instanceof TileKineticEnergyBufferBase)
				((TileKineticEnergyBufferBase) tileEntity).setOwner(PlayerUtils.getPlayerUUID((EntityPlayer) entity));
		}
	}
}
