package com.palmergames.bukkit.towny.object.gui;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyInventory;
import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.text.Component;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

public class PermissionGUI extends TownyInventory {
	private final TownBlock townBlock;
	private final boolean canEdit;
	
	public PermissionGUI(Resident res, Inventory inv, Component name, TownBlock townBlock, boolean canEdit) {
		super(res, inv, name);
		this.townBlock = townBlock;
		this.canEdit = canEdit;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public boolean canEdit() {
		return canEdit;
	}

	@Override
	public void tryPaginate(ItemStack clickedItem, Player player, Resident resident, InventoryView inventoryView) {
		int currentPage = resident.getGUIPageNum();

		try {
			// If the pressed item was a nextpage button
			if (clickedItem.getItemMeta().getDisplayName().equals(Colors.Gold + "Next")) {
				if (resident.getGUIPageNum() <= resident.getGUIPages().size() - 1) {
					// Next page exists, flip the page
					resident.setGUIPageNum(++currentPage);
					new PermissionGUI(resident, resident.getGUIPage(), inventoryView.title(), townBlock, canEdit);
					playClickSound(player);
				}
				// if the pressed item was a previous page button
			} else if (clickedItem.getItemMeta().getDisplayName().equals(Colors.Gold + "Back")) {
				// If the page number is more than 0 (So a previous page exists)
				if (resident.getGUIPageNum() > 0) {
					// Flip to previous page
					resident.setGUIPageNum(--currentPage);
					new PermissionGUI(resident, resident.getGUIPage(), inventoryView.title(), townBlock, canEdit);
					playClickSound(player);
				}
			}
		} catch (Exception ignored) {}
	}
}
