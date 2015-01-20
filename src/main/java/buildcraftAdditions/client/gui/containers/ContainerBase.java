package buildcraftAdditions.client.gui.containers;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.Slot;
import net.minecraft.item.ItemStack;

import buildcraftAdditions.client.gui.SlotPhantom;

/**
 * Copyright (c) 2014, AEnterprise
 * http://buildcraftadditions.wordpress.com/
 * Eureka is distributed under the terms of GNU GPL v3.0
 * Please check the contents of the license located in
 * http://buildcraftadditions.wordpress.com/wiki/licensing-stuff/
 */
public class ContainerBase extends Container {

	public void addPlayerInventory(InventoryPlayer invPlayer, int x, int y) {
		for (int inventoryRowIndex = 0; inventoryRowIndex < 3; ++inventoryRowIndex)
			for (int inventoryColumnIndex = 0; inventoryColumnIndex < 9; ++inventoryColumnIndex)
				addSlotToContainer(new Slot(invPlayer, 9 + inventoryColumnIndex + inventoryRowIndex * 9, x + inventoryColumnIndex * 18, y + inventoryRowIndex * 18));
		for (int hotBarIndex = 0; hotBarIndex < 9; ++hotBarIndex)
			addSlotToContainer(new Slot(invPlayer, hotBarIndex, 8 + hotBarIndex * 18, y + 58));
	}

	@Override
	public ItemStack transferStackInSlot(EntityPlayer player, int slotIndex) {
		ItemStack originalStack = null;
		Slot slot = (Slot) inventorySlots.get(slotIndex);
		int numSlots = inventorySlots.size();
		if (slot != null && slot.getHasStack()) {
			ItemStack stackInSlot = slot.getStack();
			originalStack = stackInSlot.copy();
			if (slotIndex >= numSlots - 9 * 4 && tryShiftItem(stackInSlot, numSlots)) {
				// NOOP
			} else if (slotIndex >= numSlots - 9 * 4 && slotIndex < numSlots - 9) {
				if (!shiftItemStack(stackInSlot, numSlots - 9, numSlots)) {
					return null;
				}
			} else if (slotIndex >= numSlots - 9 && slotIndex < numSlots) {
				if (!shiftItemStack(stackInSlot, numSlots - 9 * 4, numSlots - 9)) {
					return null;
				}
			} else if (!shiftItemStack(stackInSlot, numSlots - 9 * 4, numSlots)) {
				return null;
			}
			slot.onSlotChange(stackInSlot, originalStack);
			if (stackInSlot.stackSize <= 0) {
				slot.putStack(null);
			} else {
				slot.onSlotChanged();
			}
			if (stackInSlot.stackSize == originalStack.stackSize) {
				return null;
			}
			slot.onPickupFromSlot(player, stackInSlot);
		}
		return originalStack;
	}

	private boolean tryShiftItem(ItemStack stackToShift, int numSlots) {
		for (int machineIndex = 0; machineIndex < numSlots - 9 * 4; machineIndex++) {
			Slot slot = (Slot) inventorySlots.get(machineIndex);
			if (!slot.isItemValid(stackToShift)) {
				continue;
			}
			if (shiftItemStack(stackToShift, machineIndex, machineIndex + 1)) {
				return true;
			}
		}
		return false;
	}

	protected boolean shiftItemStack(ItemStack stackToShift, int start, int end) {
		boolean changed = false;
		if (stackToShift.isStackable()) {
			for (int slotIndex = start; stackToShift.stackSize > 0 && slotIndex < end; slotIndex++) {
				Slot slot = (Slot) inventorySlots.get(slotIndex);
				ItemStack stackInSlot = slot.getStack();
				if (stackInSlot != null && canStacksMerge(stackInSlot, stackToShift)) {
					int resultingStackSize = stackInSlot.stackSize + stackToShift.stackSize;
					int max = Math.min(stackToShift.getMaxStackSize(), slot.getSlotStackLimit());
					if (resultingStackSize <= max) {
						stackToShift.stackSize = 0;
						stackInSlot.stackSize = resultingStackSize;
						slot.onSlotChanged();
						changed = true;
					} else if (stackInSlot.stackSize < max) {
						stackToShift.stackSize -= max - stackInSlot.stackSize;
						stackInSlot.stackSize = max;
						slot.onSlotChanged();
						changed = true;
					}
				}
			}
		}
		if (stackToShift.stackSize > 0) {
			for (int slotIndex = start; stackToShift.stackSize > 0 && slotIndex < end; slotIndex++) {
				Slot slot = (Slot) inventorySlots.get(slotIndex);
				ItemStack stackInSlot = slot.getStack();
				if (stackInSlot == null) {
					int max = Math.min(stackToShift.getMaxStackSize(), slot.getSlotStackLimit());
					stackInSlot = stackToShift.copy();
					stackInSlot.stackSize = Math.min(stackToShift.stackSize, max);
					stackToShift.stackSize -= stackInSlot.stackSize;
					slot.putStack(stackInSlot);
					slot.onSlotChanged();
					changed = true;
				}
			}
		}
		return changed;
	}

	private static boolean canStacksMerge(ItemStack stack1, ItemStack stack2) {
		if (stack1 == null || stack2 == null) {
			return false;
		}
		if (!stack1.isItemEqual(stack2)) {
			return false;
		}
		if (!ItemStack.areItemStackTagsEqual(stack1, stack2)) {
			return false;
		}
		return true;

	}

	@Override
	public boolean canInteractWith(EntityPlayer player) {
		return true;
	}

	@Override
	public ItemStack slotClick(int slotNum, int mouseButton, int modifier, EntityPlayer player) {
		if (slotNum < 0 || inventorySlots.size() == 0 || inventorySlots == null)
			return super.slotClick(slotNum, mouseButton, modifier, player);
		Slot slot = (Slot) inventorySlots.get(slotNum);
		if (slot instanceof SlotPhantom)
			return clickPhantom(slot, mouseButton, player);
		return super.slotClick(slotNum, mouseButton, modifier, player);
	}

	protected ItemStack clickPhantom(Slot slot, int mouseButton, EntityPlayer player) {
		ItemStack playerStack = player.inventory.getItemStack();
		ItemStack slotStack = slot.getStack();

		if (mouseButton == 0 && playerStack != null && slot.isItemValid(playerStack)) {
			if (slotStack != null)
				slot.putStack(null);

			fillPhantomStack(slot, playerStack);
			slot.onSlotChanged();
		}
		else if (mouseButton == 1 || mouseButton == 2) {
			if (slotStack != null) {
				slot.putStack(null);
				slot.onSlotChanged();
			}
		}

		return null;
	}

	protected void fillPhantomStack(Slot slot, ItemStack stack) {
		ItemStack phantomStack = stack.copy();
		phantomStack.stackSize = 1;
		slot.putStack(phantomStack);
	}
}
