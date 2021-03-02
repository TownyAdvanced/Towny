package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.TownyInventory;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

public class ResidentUtil {
	
	/** 
	 * Return a list of Residents that can be seen (not vanished) by the viewer.
	 * 
	 * @param viewer - Player who is looking.
	 * @param residentList - List of Residents which could be viewed.
	 * @return - List of residents that can actually be seen.
	 */
	public static List<Resident> getOnlineResidentsViewable(Player viewer, ResidentList residentList) {
		
		List<Resident> onlineResidents = new ArrayList<>();
		for (Player player : BukkitTools.getOnlinePlayers()) {
			if (player != null) {
				/*
				 * Loop town/nation resident list
				 */
				for (Resident resident : residentList.getResidents()) {
					if (resident.getName().equalsIgnoreCase(player.getName()))
						if ((viewer == null) || (viewer.canSee(BukkitTools.getPlayerExact(resident.getName())))) {
							onlineResidents.add(resident);
						}
				}
			}
		}
		
		return onlineResidents;
	}
	
	/**
	 * Transforms a String[] of names to a list of Residents.
	 * Uses the BukkitTools.matchPlayer() rather than BukkitTools.getPlayerExact();
	 * Used for:
	 *  - Inviting
	 *  - Kicking
	 * 
	 * @param sender - CommandSender.
	 * @param names - Names to be converted.
	 * @return - List of residents to be used later.
	 */
	public static List<Resident> getValidatedResidents(Object sender, String[] names) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Resident> residents = new ArrayList<>();
		for (String name : names) {
			List<Player> matches = BukkitTools.matchPlayer(name);
			if (matches.size() > 1) {
				StringBuilder line = new StringBuilder("Multiple players selected: ");
				for (Player p : matches)
					line.append(", ").append(p.getName());
				TownyMessaging.sendErrorMsg(sender, line.toString());
			} else {
				String targetName = !matches.isEmpty() ? matches.get(0).getName() : name;
				Resident target = townyUniverse.getResident(targetName);
				if (target != null) {
					residents.add(target);
				}
				else {
					TownyMessaging.sendErrorMsg(sender, Translation.of("msg_err_not_registered_1", targetName));
				}
			}
		}
		return residents;
	}
	
	/**
	 * Opens a basic inventory GUI with pagination.
	 * GUI displays either switch or itemuse materials via an
	 * ArrayList of strings is converted to valid Materials
	 * and sent to be shown to the player.
	 *  
	 * @param resident The {@link Resident} to show the inventory to
	 * @param list ArrayList of strings that will be converted to valid Materials.
	 * @param name Name of the inventory window.
	 */
	public static void openGUIInventory(Resident resident, List<String> list, String name) {
		ArrayList<ItemStack> items = new ArrayList<>();
		for (String item : list) {
			Material mat = Material.getMaterial(item);
			if (mat != null) {
				items.add(new ItemStack(mat));
			}
		}
		createTownyGUI(resident, items, name);
	}
	
	/*
	 * Big credit goes to Hex_27 for the guidance following his ScrollerInventory
	 * https://www.spigotmc.org/threads/infinite-inventory-with-pages.178964/
	 * 
	 * Nice and simple.
	 */
	private static void createTownyGUI(Resident resident, ArrayList<ItemStack> items, String name) {

		Inventory page = getBlankPage(name);
		ArrayList<Inventory> pages = new ArrayList<Inventory>();
		
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
		resident.setGUIPages(pages);
		resident.setGUIPageNum(0);
		new TownyInventory(resident, pages.get(0), name);
	}

	// This creates a blank page with the next and prev buttons
	private static Inventory getBlankPage(String name) {
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
