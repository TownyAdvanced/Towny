package com.palmergames.bukkit.towny.listeners;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyInventory;
import com.palmergames.bukkit.util.Colors;

public class TownyInventoryListener implements Listener {

	public TownyInventoryListener() {

	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		
		if (!(event.getInventory().getHolder() instanceof TownyInventory))
			return;
		event.setCancelled(true);
		Player player = (Player) event.getWhoClicked();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		
		if (resident == null)
			return;

		int currentPage = resident.getGUIPageNum();
		
		try {
			// If the pressed item was a nextpage button
			if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Next")) {
				// If there is no next page, don't do anything
				if (resident.getGUIPageNum() >= resident.getGUIPages().size() - 1) {
					return;
				} else {
					// Next page exists, flip the page
					resident.setGUIPageNum(++currentPage);
					new TownyInventory(resident, resident.getGUIPage(), event.getView().getTitle());
				}
				// if the pressed item was a previous page button
			} else if (event.getCurrentItem().getItemMeta().getDisplayName().equals(Colors.Gold + "Back")) {
				// If the page number is more than 0 (So a previous page exists)
				if (resident.getGUIPageNum() > 0) {
					// Flip to previous page
					resident.setGUIPageNum(--currentPage);
					new TownyInventory(resident, resident.getGUIPage(), event.getView().getTitle());
				}
			}
		} catch (Exception ignored) {}	
	}
}