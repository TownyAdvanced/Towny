package com.palmergames.bukkit.towny.regen.block;

import org.bukkit.inventory.ItemStack;

/**
 * @author ElgarL
 * 
 */
public class BlockInventoryHolder extends BlockObject {

	private ItemStack items[];

	/**
	 * Constructor for all Container objects
	 * 
	 * @param typeId
	 * @param items
	 */
	public BlockInventoryHolder(int typeId, ItemStack[] items) {

		super(typeId);
		setItems(items);
	}

	/**
	 * Constructor for all Container objects
	 * 
	 * @param typeId
	 * @param data
	 * @param items
	 */
	public BlockInventoryHolder(int typeId, byte data, ItemStack[] items) {

		super(typeId, data);
		setItems(items);
	}

	/**
	 * Get the list of items.
	 * 
	 * @return array of ItemStacks.
	 */
	public ItemStack[] getItems() {

		return this.items;
	}

	/**
	 * Clone the list of items so it's a snapshot of the inventory.
	 * 
	 * @param items
	 */
	public void setItems(ItemStack[] items) {

		this.items = items.clone();
	}
}
