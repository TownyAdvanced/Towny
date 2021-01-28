package com.palmergames.bukkit.towny.object;

import org.bukkit.Bukkit;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;

public class TownyInventory implements InventoryHolder {

	private final Inventory inv;
	
	public TownyInventory(Resident res, Inventory inv, String name) {
		this.inv = Bukkit.createInventory(this, 54, name);
		this.inv.setContents(inv.getContents());
		res.getPlayer().openInventory(this.inv);
	}

	@Override
	public Inventory getInventory() {
		return inv;
	}
}
