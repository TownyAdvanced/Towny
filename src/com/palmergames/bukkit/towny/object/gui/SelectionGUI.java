package com.palmergames.bukkit.towny.object.gui;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyInventory;
import org.bukkit.inventory.Inventory;

/**
 * A GUI used for selecting 
 */
public class SelectionGUI extends TownyInventory {
	private final SelectionType type;	
	
	public SelectionGUI(Resident res, Inventory inv, String name, SelectionType type) {
		super(res, inv, name);
		this.type = type;
	}

	public SelectionType getType() {
		return type;
	}

	public enum SelectionType {
		SWITCHES, ALLOWEDBLOCKS, ITEMUSE
	}
}
