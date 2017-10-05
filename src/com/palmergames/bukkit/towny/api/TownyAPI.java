package com.palmergames.bukkit.towny.api;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;
import java.util.regex.PatternSyntaxException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.util.BukkitTools.BukkitVersion;

/**
 * 
 * TownyAPI 
 * 
 * Used to get information from Towny SAFELY.
 * 
 * @author Wowserman
 *
 */
public class TownyAPI {

	private Towny plugin;
	
	private TownyInformationTask task;
	
	private boolean menusEnabled = false;
	
	private int residents, towns, nations;
	
	private List<Inventory> townMenus = new ArrayList<Inventory>();
	private List<Inventory> nationMenus = new ArrayList<Inventory>();
	
	
	/**
	 * Store Pages of Menus for each Nation.
	 */
	private Hashtable<Nation, List<Inventory>> nationTownsMenus = new Hashtable<Nation, List<Inventory>>(); 
	/**
	 * Store How many Pages each Nation has.
	 */
	private Hashtable<Nation, Integer> nationTownsCount = new Hashtable<Nation, Integer>();
	
	public int getResidents() {
		return residents;
	}

	public int getTowns() {
		return towns;
	}

	public int getNations() {
		return nations;
	}

	public int getNationTownsPages(Nation nation) {
		return nationTownsCount.getOrDefault(nation, 0);
	}
	
	public Inventory getTownMenu(int page) {
		return townMenus.get(page-1);
	}
	
	public Inventory getNationMenu(int page) {
		return nationMenus.get(page-1);
	}
	
	public Inventory getNationTownsMenu(Nation nation, int page) {
		return this.nationTownsMenus.get(nation).get(page-1);
	}
	
	public void setPlugin(Towny plugin) {
		this.plugin = plugin;
	}

	public void setTask(TownyInformationTask task) {
		this.task = task;
	}

	public void setResidents(int residents) {
		this.residents = residents;
	}

	public void setTowns(int towns) {
		this.towns = towns;
	}

	public void setNations(int nations) {
		this.nations = nations;
	}

	public void setTownMenus(List<Inventory> townMenus) {
		this.townMenus = townMenus;
	}

	public void setNationMenus(List<Inventory> nationMenus) {
		this.nationMenus = nationMenus;
	}

	public void setNationTownsMenus(Hashtable<Nation, List<Inventory>> nationTownsMenus) {
		this.nationTownsMenus = nationTownsMenus;
	}

	public void setNationTownsCount(Hashtable<Nation, Integer> nationTowns) {
		this.nationTownsCount = nationTowns;
	}

	public int getPageOfMenu(Inventory menu) {
		try {
			return Integer.parseInt(menu.getName().split(" ")[2]);
		} catch (NumberFormatException e) {
			return 1;
		} catch (PatternSyntaxException e) {
			return 1;
		} catch (IndexOutOfBoundsException e) {
			return 1;
		}
	}
	
	public String getTownOfBanner(ItemStack banner) {
		try {
			return ChatColor.stripColor(banner.getItemMeta().getDisplayName()).contains(", ") ? 
									ChatColor.stripColor(banner.getItemMeta().getDisplayName()).split(", ")[0]:
										ChatColor.stripColor(banner.getItemMeta().getDisplayName());
		} catch (Exception e) {
			return "";
		}
	}
	
	public String getNationOfBanner(ItemStack banner) {
		try {
			return ChatColor.stripColor(banner.getItemMeta().getDisplayName()).contains(", ") ? ChatColor.stripColor(banner.getItemMeta().getDisplayName()).split(", ")[1]: ChatColor.stripColor(banner.getItemMeta().getDisplayName());
		} catch (Exception e) {
			return "";
		}
	}
	
	public void openTownList(Player player, int page) {
		player.openInventory(this.getTownMenu(page));
	}
	
	public void openNationList(Player player, int page) {
		player.openInventory(this.getNationMenu(page));
	}
	
	public void openNationTownsList(Player player, Nation nation, int page) {
		player.openInventory(this.getNationTownsMenu(nation, page));
	}
	
	public boolean isMenusEnabled() {
		return menusEnabled;
	}
	
	public TownyAPI(Towny instance) {
		this.plugin = instance;
		this.menusEnabled = plugin.getBukkitVersion().isOlder(BukkitVersion.v1_7);
		this.task = new TownyInformationTask(this);
		this.task.runTaskTimerAsynchronously(plugin, 0, 1200);
	}

	public Towny getPlugin() {
		return plugin;
	}
}
