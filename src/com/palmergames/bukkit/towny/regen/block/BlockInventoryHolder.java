/**
 * 
 */
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
	 * @param typeID
	 * @param inventorySize
	 */
	public BlockInventoryHolder(int typeID, ItemStack[] items) {

		super(typeID);
		setItems(items);
	}

	/**
	 * Constructor for all Container objects
	 * 
	 * @param typeID
	 * @param data
	 * @param inventorySize
	 */
	public BlockInventoryHolder(int typeID, byte data, ItemStack[] items) {

		super(typeID, data);
		setItems(items);
	}

	/**
	 * Get the list of items.
	 * 
	 * @return
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
