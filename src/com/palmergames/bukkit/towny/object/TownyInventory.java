package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public class TownyInventory implements InventoryHolder {
	
	private static final Sound clickSound = Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.stone_button.click_on"), Sound.Source.MASTER, 1.0f, 1.0f);

	private final Inventory inv;
	
	public TownyInventory(Resident res, Inventory inv, String name) {
		this.inv = Bukkit.createInventory(this, 54, name);
		this.inv.setContents(inv.getContents());
		res.getPlayer().openInventory(this.inv);
	}

	@Override
	public @NotNull Inventory getInventory() {
		return inv;
	}
	
	public void playClickSound(Player player) {
		Towny.getAdventure().player(player).playSound(clickSound);
	}
	
	public void tryPaginate(ItemStack clickedItem, Player player, Resident resident, InventoryView inventoryView) {
		int currentPage = resident.getGUIPageNum();

		try {
			// If the pressed item was a nextpage button
			if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Next")) {
				// If there is no next page, don't do anything
				if (resident.getGUIPageNum() >= resident.getGUIPages().size() - 1) {
					return;
				} else {
					// Next page exists, flip the page
					resident.setGUIPageNum(++currentPage);
					new TownyInventory(resident, resident.getGUIPage(), inventoryView.getTitle());
					playClickSound(player);
				}
				// if the pressed item was a previous page button
			} else if (clickedItem.getItemMeta().getDisplayName().equals(ChatColor.GOLD + "Back")) {
				// If the page number is more than 0 (So a previous page exists)
				if (resident.getGUIPageNum() > 0) {
					// Flip to previous page
					resident.setGUIPageNum(--currentPage);
					new TownyInventory(resident, resident.getGUIPage(), inventoryView.getTitle());
					playClickSound(player);
				}
			}
		} catch (Exception ignored) {}
	}
}
