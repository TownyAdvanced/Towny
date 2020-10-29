package com.palmergames.bukkit.towny.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyInventory;
import com.palmergames.bukkit.util.Colors;

public class TownyInventoryListener implements Listener {

	public TownyInventoryListener() {

	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if (!event.getView().getTitle().equalsIgnoreCase("Towny Switch List") && !event.getView().getTitle().equalsIgnoreCase("Towny ItemUse List"))
			return;

		Player player = (Player) event.getWhoClicked();
		try {
		Resident resident = null;
		try {
			resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
		} catch (NotRegisteredException e1) {
			event.setCancelled(true);
			return;
		}

		TownyInventory inv = resident.getGUIInventory();

		
			// If the pressed item was a nextpage button
			if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Next")) {
				event.setCancelled(true);
				// If there is no next page, don't do anything
				if (inv.currentPage >= inv.pages.size() - 1) {
					return;
				} else {
					// Next page exists, flip the page
					inv.currentPage += 1;
					player.openInventory(inv.pages.get(inv.currentPage));
				}
				// if the pressed item was a previous page button
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Back")) {
				event.setCancelled(true);
				// If the page number is more than 0 (So a previous page exists)
				if (inv.currentPage > 0) {
					// Flip to previous page
					inv.currentPage -= 1;
					player.openInventory(inv.pages.get(inv.currentPage));
				}
			}
		} catch (Exception e) {
			event.setCancelled(true);
			return;
		}
		event.setCancelled(true);
	}
	
}