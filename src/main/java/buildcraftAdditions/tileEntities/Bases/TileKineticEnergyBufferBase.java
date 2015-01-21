package buildcraftAdditions.tileEntities.Bases;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.Explosion;

import net.minecraftforge.common.util.ForgeDirection;

import cofh.api.energy.IEnergyProvider;
import cofh.api.energy.IEnergyReceiver;

import buildcraftAdditions.config.ConfigurationHandler;
import buildcraftAdditions.networking.ISyncronizedTile;
import buildcraftAdditions.networking.MessageConfiguration;
import buildcraftAdditions.networking.MessageSelfDestruct;
import buildcraftAdditions.networking.PacketHandler;
import buildcraftAdditions.tileEntities.TileKineticEnergyBufferTier1;
import buildcraftAdditions.utils.EnumPriority;
import buildcraftAdditions.utils.EnumSideStatus;
import buildcraftAdditions.utils.IConfigurableOutput;
import buildcraftAdditions.utils.Location;
import buildcraftAdditions.utils.Utils;

import io.netty.buffer.ByteBuf;
/**
 * Copyright (c) 2014, AEnterprise
 * http://buildcraftadditions.wordpress.com/
 * Buildcraft Additions is distributed under the terms of GNU GPL v3.0
 * Please check the contents of the license located in
 * http://buildcraftadditions.wordpress.com/wiki/licensing-stuff/
 */
public abstract class TileKineticEnergyBufferBase extends TileBase implements IEnergyReceiver, IEnergyProvider, IConfigurableOutput, ISyncronizedTile {
	public int energy, maxEnergy, maxInput, maxOutput, loss, fuse;
	public EnumSideStatus[] configuration = new EnumSideStatus[6];
	protected boolean[] blocked = new boolean[6];
	public EnumPriority[] priorities = new EnumPriority[6];
	public int tier;
	public boolean selfDestruct, engineControl;
	public String owner = "";
	public EntityPlayer destroyer;


	public TileKineticEnergyBufferBase(int maxEnergy, int maxInput, int maxOutput, int loss, int tier) {
		super();
		this.maxEnergy = maxEnergy;
		this.maxInput = maxInput;
		this.maxOutput = maxOutput;
		this.loss = loss;
		this.tier = tier;
		for (int t = 0; t < 6; t++) {
			configuration[t] = EnumSideStatus.INPUT;
			priorities[t] = EnumPriority.NORMAL;
		}
	}

	@Override
	public int receiveEnergy(ForgeDirection from, int maxReceive, boolean simulate) {
		if (configuration[from.ordinal()] != EnumSideStatus.INPUT && configuration[from.ordinal()] != EnumSideStatus.BOTH)
			return 0;
		int recieved = maxReceive;
		if (recieved > maxEnergy - energy)
			recieved = maxEnergy - energy;
		if (recieved > maxInput)
			recieved = maxInput;
		if (!simulate) {
			energy += recieved;
			blocked[from.ordinal()] = true;
		}
		return recieved;
	}

	@Override
	public int extractEnergy(ForgeDirection from, int maxExtract, boolean simulate) {
		if (configuration[from.ordinal()] != EnumSideStatus.OUTPUT && configuration[from.ordinal()] != EnumSideStatus.BOTH)
			return 0;
		int extracted = maxExtract;
		if (extracted > energy)
			extracted = energy;
		if (extracted > maxOutput)
			extracted = maxOutput;
		if (!simulate)
			energy -= extracted;
		return extracted;
	}

	@Override
	public int getEnergyStored(ForgeDirection from) {
		return energy;
	}

	@Override
	public int getMaxEnergyStored(ForgeDirection from) {
		return maxEnergy;
	}

	@Override
	public void readFromNBT(NBTTagCompound tag) {
		super.readFromNBT(tag);
		energy = tag.getInteger("energy");
		maxEnergy = tag.getInteger("maxEnergy");
		maxInput = tag.getInteger("maxInput");
		maxOutput = tag.getInteger("maxOutput");
		loss = tag.getInteger("loss");
		engineControl = tag.getBoolean("engineControl");
		if (tag.hasKey("configuration")) {
			for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
				configuration[direction.ordinal()] = Utils.intToStatus(tag.getInteger("configuration" + direction.ordinal()));
				priorities[direction.ordinal()] = EnumPriority.PRIORITIES[tag.getInteger("priority" + direction.ordinal())];
			}
		}
		if (tag.hasKey("owner"))
			owner = tag.getString("owner");
	}

	@Override
	public void writeToNBT(NBTTagCompound tag) {
		super.writeToNBT(tag);
		tag.setInteger("energy", energy);
		tag.setInteger("maxEnergy", maxEnergy);
		tag.setInteger("maxInput", maxInput);
		tag.setInteger("maxOutput", maxOutput);
		tag.setInteger("loss", loss);
		tag.setBoolean("configuration", true);
		tag.setBoolean("engineControl", engineControl);
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			tag.setInteger("configuration" + direction.ordinal(), Utils.statusToInt(configuration[direction.ordinal()]));
			tag.setInteger("priority" + direction.ordinal(), priorities[direction.ordinal()].ordinal());
		}
		if (owner != null)
			tag.setString("owner", owner);
	}

	@Override
	public void updateEntity() {
		if (getEnergyLevel() > 85)
			engineControl = false;
		if (getEnergyLevel() < 30)
			engineControl = true;
		super.updateEntity();
		if (selfDestruct) {
			fuse--;
			if (fuse % 20 == 0)
				destroyer.addChatComponentMessage(new ChatComponentText(Utils.localize("selfdestructCountdouwn") + ": " + fuse / 20));
		}
		if (fuse <= 0 && selfDestruct)
			byeBye();
		if (ConfigurationHandler.powerloss)
			energy = energy - loss;
		if (energy < 0)
			energy = 0;
		outputEnergy();
	}

	public void outputEnergy() {
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			for (EnumPriority priority : EnumPriority.PRIORITIES) {
				if (priorities[direction.ordinal()] != priority)
					continue;
				if (configuration[direction.ordinal()] != EnumSideStatus.OUTPUT && configuration[direction.ordinal()] != EnumSideStatus.BOTH)
					continue;
				Location location = new Location(worldObj, xCoord, yCoord, zCoord);
				location.move(direction);
				IEnergyReceiver energyHandler = null;
				if (location.getTileEntity() != null && location.getTileEntity() instanceof IEnergyReceiver)
					energyHandler = (IEnergyReceiver) location.getTileEntity();
				if (energyHandler != null) {
					int sendEnergy = energy;
					if (canSharePower(location.getTileEntity(), direction)) {
						TileKineticEnergyBufferTier1 keb = (TileKineticEnergyBufferTier1) location.getTileEntity();
						sendEnergy = ((energy + keb.energy) / 2) - keb.energy;
					}
					if (sendEnergy < 0)
						sendEnergy = 0;
					if (sendEnergy > maxOutput)
						sendEnergy = maxOutput;

					energy -= energyHandler.receiveEnergy(direction.getOpposite(), sendEnergy, false);
				}
			}
		}
	}

	private boolean canSharePower(TileEntity target, ForgeDirection outputSide) {
		if (configuration[outputSide.ordinal()] == EnumSideStatus.BOTH && target instanceof TileKineticEnergyBufferTier1) {
			TileKineticEnergyBufferTier1 keb = (TileKineticEnergyBufferTier1) target;
			if (keb.getStatus(outputSide.getOpposite()) == EnumSideStatus.BOTH)
				return true;
		}
		return false;
	}

	@Override
	public boolean canConnectEnergy(ForgeDirection from) {
		return configuration[from.ordinal()] != EnumSideStatus.DISSABLED;
	}

	public void sendConfigurationToSever() {
		PacketHandler.instance.sendToServer(new MessageConfiguration(this));
	}

	public void setOwner(String owner) {
		this.owner = owner;
	}

	public void activateSelfDestruct() {
		if (worldObj.isRemote) {
			PacketHandler.instance.sendToServer(new MessageSelfDestruct(xCoord, yCoord, zCoord));
			return;
		}
		selfDestruct = true;
		fuse = 100;
		destroyer.addChatComponentMessage(new ChatComponentText(Utils.localize("selfdestructActivated")));
		destroyer.closeScreen();
	}

	public void byeBye() {
		Explosion explosion = worldObj.createExplosion(destroyer, xCoord, yCoord, zCoord, (energy / 900000) + 5, true);
		explosion.doExplosionA();
		explosion.doExplosionB(true);
	}

	@Override
	public EnumSideStatus getStatus(ForgeDirection side) {
		return configuration[side.ordinal()];
	}

	@Override
	public void changeStatus(ForgeDirection side) {
		EnumSideStatus status = configuration[side.ordinal()];
		if (status == EnumSideStatus.INPUT)
			status = EnumSideStatus.OUTPUT;
		else if (status == EnumSideStatus.OUTPUT)
			status = EnumSideStatus.BOTH;
		else if (status == EnumSideStatus.BOTH)
			status = EnumSideStatus.DISSABLED;
		else if (status == EnumSideStatus.DISSABLED)
			status = EnumSideStatus.INPUT;
		configuration[side.ordinal()] = status;
	}

	@Override
	public void overrideConfiguration(EnumSideStatus[] newConfiguration) {
		configuration = newConfiguration;
	}

	@Override
	public ByteBuf writeToByteBuff(ByteBuf buf) {
		buf.writeInt(energy);
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			buf.writeInt(Utils.statusToInt(configuration[direction.ordinal()]));
			buf.writeInt(priorities[direction.ordinal()].ordinal());
		}
		int length = owner.length();
		buf.writeInt(length);
		char[] chars = owner.toCharArray();
		for (int t = 0; t < length; t++)
			buf.writeChar(chars[t]);
		return buf;
	}

	@Override
	public ByteBuf readFromByteBuff(ByteBuf buf) {
		energy = buf.readInt();
		for (ForgeDirection direction : ForgeDirection.VALID_DIRECTIONS) {
			configuration[direction.ordinal()] = Utils.intToStatus(buf.readInt());
			priorities[direction.ordinal()] = EnumPriority.PRIORITIES[buf.readInt()];
		}
		int length = buf.readInt();
		owner = "";
		for (int teller = 0; teller < length; teller++)
			owner += buf.readChar();
		return buf;
	}

	@Override
	public int getX() {
		return xCoord;
	}

	@Override
	public int getY() {
		return yCoord;
	}

	@Override
	public int getZ() {
		return zCoord;
	}

	public int getEnergyLevel() {
		return (energy * 100) / maxEnergy;
	}

	@Override
	public EnumPriority getPriority(ForgeDirection side) {
		return priorities[side.ordinal()];
	}

	@Override
	public void overridePriority(EnumPriority[] newPriorities) {
		priorities = newPriorities;
	}

	@Override
	public void changePriority(ForgeDirection side) {
		priorities[side.ordinal()] = priorities[side.ordinal()].getNextPriority();
	}
}
