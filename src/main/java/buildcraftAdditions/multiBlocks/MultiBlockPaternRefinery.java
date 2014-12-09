package buildcraftAdditions.multiBlocks;

import net.minecraftforge.common.util.ForgeDirection;
/**
 * Copyright (c) 2014, AEnterprise
 * http://buildcraftadditions.wordpress.com/
 * Buildcraft Additions is distributed under the terms of GNU GPL v3.0
 * Please check the contents of the license located in
 * http://buildcraftadditions.wordpress.com/wiki/licensing-stuff/
 */
public class MultiBlockPaternRefinery extends MultiBlockPatern {

	public MultiBlockPaternRefinery() {
		super(new ForgeDirection[]{ForgeDirection.NORTH, ForgeDirection.NORTH, ForgeDirection.NORTH}, 'R');
		identifiers[1] = '\n';
	}
}