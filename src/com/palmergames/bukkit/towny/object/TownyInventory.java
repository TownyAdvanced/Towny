package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.palmergames.bukkit.util.Colors;

/**
 * Big credit goes to Hex_27 for the guidance following his ScrollerInventory
 * https://www.spigotmc.org/threads/infinite-inventory-with-pages.178964/
 * 
 * Nice and simple.
 */
public class TownyInventory {

	public ArrayList<Inventory> pages = new ArrayList<Inventory>();
	public int currentPage = 0;
	public static HashMap<Player, TownyInventory> players = new HashMap<Player, TownyInventory>();

	public TownyInventory(Player player, List<String> list, String name) {

		ArrayList<ItemStack> items = new ArrayList<>();
		for (String item : list) {
			Material mat = Material.getMaterial(item);
			if (mat != null) {
				items.add(new ItemStack(mat));
			}
		}
		Inventory page = getBlankPage(name);
		
		for (int i = 0; i < items.size(); i++) {
			if (page.firstEmpty() == 46) {
				pages.add(page);
				page = getBlankPage(name);
				page.addItem(items.get(i));
			} else {
				//Add the item to the current page as per normal
				page.addItem(items.get(i));
			}
		}
		pages.add(page);
		player.openInventory(pages.get(currentPage));
		players.put(player, this);
	}

	// This creates a blank page with the next and prev buttons
	private Inventory getBlankPage(String name) {
		Inventory page = Bukkit.createInventory(null, 54, name);

		ItemStack nextpage = new ItemStack(Material.PAPER);
		ItemMeta meta = nextpage.getItemMeta();
		meta.setDisplayName(Colors.Gold + "Next");
		nextpage.setItemMeta(meta);

		ItemStack prevpage = new ItemStack(Material.PAPER);
		meta = prevpage.getItemMeta();
		meta.setDisplayName(Colors.Gold + "Back");
		prevpage.setItemMeta(meta);

		page.setItem(53, nextpage);
		page.setItem(45, prevpage);
		return page;
	}
}
