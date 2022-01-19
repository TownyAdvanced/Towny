package com.palmergames.bukkit.towny.utils;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.Translatable;

import com.palmergames.bukkit.towny.object.gui.SelectionGUI;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
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
	public static List<Resident> getValidatedResidents(CommandSender sender, String[] names) {
		List<Resident> residents = new ArrayList<>();

		for (String name : names) {
			List<Player> matches = BukkitTools.matchPlayer(name);
			if (matches.size() > 1) {
				TownyMessaging.sendErrorMsg(sender, "Multiple players selected: " + matches.stream().map(Player::getName).collect(Collectors.joining(", ")));
			} else {
				String targetName = !matches.isEmpty() ? matches.get(0).getName() : name;
				Resident target = TownyUniverse.getInstance().getResident(targetName);

				if (target != null)
					residents.add(target);
				else
					TownyMessaging.sendErrorMsg(sender, Translatable.of("msg_err_not_registered_1", targetName));
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
	
	public static void openGUIInventory(Resident resident, Set<Material> set, String name) {
		ArrayList<ItemStack> items = new ArrayList<>();
		for (Material material : set)
			items.add(new ItemStack(material));
		
		createTownyGUI(resident, items, name);
	}
	
	public static void openSelectionGUI(Resident resident, SelectionGUI.SelectionType selectionType) {
		String inventoryName = Translatable.of("gui_title_select_plot_type").forLocale(resident);
		Inventory page = getBlankPage(inventoryName);
		ArrayList<Inventory> pages = new ArrayList<>();
		
		for (TownBlockType townBlockType : TownBlockTypeHandler.getTypes().values()) {
			ItemStack item = new ItemStack(Material.GRASS_BLOCK);
			
			ItemMeta meta = item.getItemMeta();
			meta.setDisplayName(Colors.Gold + townBlockType.getFormattedName());
			item.setItemMeta(meta);

			if (page.firstEmpty() == 46) {
				pages.add(page);
				page = getBlankPage(inventoryName);
			}
			
			page.addItem(item);
		}
		
		pages.add(page);
		resident.setGUIPageNum(0);
		resident.setGUIPages(pages);
		
		new SelectionGUI(resident, pages.get(0), inventoryName, selectionType);
	}
	
	/*
	 * Big credit goes to Hex_27 for the guidance following his ScrollerInventory
	 * https://www.spigotmc.org/threads/infinite-inventory-with-pages.178964/
	 * 
	 * Nice and simple.
	 */
	private static void createTownyGUI(Resident resident, ArrayList<ItemStack> items, String name) {

		Inventory page = getBlankPage(name);
		ArrayList<Inventory> pages = new ArrayList<>();
		
		for (ItemStack item : items) {
			if (page.firstEmpty() == 46) {
				pages.add(page);
				page = getBlankPage(name);
			}

			page.addItem(item);
		}

		pages.add(page);
		resident.setGUIPages(pages);
		resident.setGUIPageNum(0);
		new TownyInventory(resident, pages.get(0), name);
	}

	// This creates a blank page with the next and prev buttons
	public static Inventory getBlankPage(String name) {
		Inventory page = Bukkit.createInventory(null, 54, name);

		ItemStack nextpage = new ItemStack(Material.ARROW);
		ItemMeta meta = nextpage.getItemMeta();
		meta.setDisplayName(Colors.Gold + "Next");
		nextpage.setItemMeta(meta);

		ItemStack prevpage = new ItemStack(Material.ARROW);
		meta = prevpage.getItemMeta();
		meta.setDisplayName(Colors.Gold + "Back");
		prevpage.setItemMeta(meta);

		page.setItem(53, nextpage);
		page.setItem(45, prevpage);
		return page;
	}
	
	
	public static Resident createAndGetNPCResident() {
		Resident npc = null;
		try {
			String name = nextNpcName();
			final UUID npcUUID = UUID.randomUUID();
			TownyUniverse.getInstance().getDataSource().newResident(name, npcUUID);
			npc = TownyUniverse.getInstance().getResident(npcUUID);
			npc.setRegistered(System.currentTimeMillis());
			npc.setLastOnline(0);
			npc.setNPC(true);
			npc.save();
		} catch (TownyException e) {
			e.printStackTrace();
		}
		
		return npc;
	}
	
	public static String nextNpcName() throws TownyException {

		String name;
		int i = 0;
		do {
			name = TownySettings.getNPCPrefix() + ++i;
			if (!TownyUniverse.getInstance().hasResident(name))
				return name;
			if (i > 100000)
				throw new TownyException(Translatable.of("msg_err_too_many_npc"));
		} while (true);
	}
	
	/**
	 * Method to remove the newest residents in order to bring a town's population
	 * low enough to meet the population cap.
	 * 
	 * @param town The Town to reduce the population of.
	 */
	public static void reduceResidentCountToFitTownMaxPop(Town town) {
		if (TownySettings.getMaxResidentsPerTown() == 0)
			return;
		
		int max = TownySettings.getMaxResidentsForTown(town);
		if (town.getNumResidents() <= max)
			return;
		
		int i = 1;
		List<Resident> toRemove = new ArrayList<Resident>(town.getNumResidents() - max);
		for (Resident res : town.getResidents()) {
			if (i > max)
				toRemove.add(res);
			i++;
		}
		
		if (!toRemove.isEmpty())
			toRemove.stream().forEach(res -> res.removeTown());
	}
}
