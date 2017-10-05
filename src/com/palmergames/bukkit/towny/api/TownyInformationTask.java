package com.palmergames.bukkit.towny.api;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.Hashtable;
//import java.util.Hashtable;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.scheduler.BukkitRunnable;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;

public class TownyInformationTask extends BukkitRunnable {

	private TownyAPI towny;

	public TownyInformationTask(TownyAPI instance) {
		this.towny = instance;
	}

	private int getNationPages() {
		int pages = 1;
		int n = towny.getNations();
		while ((n % 28 == 0) && n != 0) {
			n /= 28;
			pages++;
		}
		return pages;
	}

	private int getTownPages() {
		int pages = 1;
		int t = towny.getTowns();
		while ((t % 28 == 0) && t != 0) {
			t /= 28;
			pages++;
		}
		return pages;
	}

	private int getNationTownsPages(Nation nation) {
		int pages = 1;
		int towns = nation.getTowns().size();
		int t = towns;
		while ((t % 28 == 0) && t != 0) {
			t /= 28;
			pages++;
		}
		return pages;
	}
	
	private int[] slots = { 10, 11, 12, 13, 14, 15, 16, 19, 20, 21, 22, 23, 24, 25, 28, 29, 30, 31, 32, 33, 34, 37, 38, 39, 40, 41, 42, 43 };
	
	@SuppressWarnings("deprecation")
	@Override
	public void run() {		
		
		try {
			
			towny.setResidents(towny.getPlugin().getTownyUniverse().getResidents().size());
			
			towny.setTowns(towny.getPlugin().getTownyUniverse().getTowns().size());

			towny.setNations(towny.getPlugin().getTownyUniverse().getNations().size());
			
			if (towny.isMenusEnabled()==false) {
				towny.getPlugin().getLogger().info("Updated API Information. Residents: " + towny.getResidents() + " Towns: " + towny.getTowns() + " Nations: " + towny.getNations());
				return;
			}
			
			int townPages = this.getTownPages();
			
			int nationPages = this.getNationPages();

			List<Inventory> newTownMenus = new ArrayList<Inventory>();
						
			for (int page = 1; page <= townPages; page++) {
				newTownMenus.add(this.createTownMenu(page));
			}
			
			if (newTownMenus.size()==0) {
				newTownMenus.add(this.createTownMenu(1));
			}
			
			towny.setTownMenus(newTownMenus);
			
			List<Inventory> newNationMenus = new ArrayList<Inventory>();
			
			for (int paje = 1; paje <= nationPages; paje++) {
				newNationMenus.add(this.createNationMenu(paje));
			}
			
			if (newNationMenus.size()==0) {
				newNationMenus.add(this.createNationMenu(1));
			}

			towny.setNationMenus(newNationMenus);
			
			towny.setNationMenus(newNationMenus);
			
			Hashtable<Nation, List<Inventory>> newNationTownsMenus= new Hashtable<Nation, List<Inventory>>();
			Hashtable<Nation, Integer> newNationTowns = new Hashtable<Nation, Integer>();
			
			for (Nation nation:towny.getPlugin().getTownyUniverse().getNations()) {
				newNationTowns.put(nation, this.getNationTownsPages(nation));
				
				for (int payge = 1; payge <= newNationTowns.get(nation); payge++) {
					List<Inventory> pages = newNationTownsMenus.getOrDefault(nation, new ArrayList<Inventory>());
					pages.add(this.createNationsTownsMenu(nation, payge));
					newNationTownsMenus.put(nation, pages);
				}
				
				if (newNationTowns.get(nation)==0) {
					List<Inventory> pages = newNationTownsMenus.getOrDefault(nation, new ArrayList<Inventory>());
					pages.add(this.createNationsTownsMenu(nation, 1));
					newNationTownsMenus.put(nation, pages);
				}
			}
			
			towny.setNationTownsMenus(newNationTownsMenus);
			towny.setNationTownsCount(newNationTowns);
			
			towny.getPlugin().getLogger().info("Updated API Information. Residents: " + towny.getResidents() + " Towns: " + towny.getTowns() + " Nations: " + towny.getNations() + " Town Pages: " + townPages + " Nation Pages: " + nationPages);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private ItemStack getItemButtonForTown(Town town, boolean nationSpecific, Integer... rank) {
		ItemStack banner = town.getBanner();
		ItemMeta meta = banner.getItemMeta();
		try {
			meta.setDisplayName(town.hasNation() ? Colors.Yellow + town.getName() + ", " + town.getNation().getName() : Colors.Yellow + town.getName());
			
		} catch (NotRegisteredException e1) {
			meta.setDisplayName(Colors.Yellow + TownyFormatter.getFormattedTownName(town));
		}
		/*
		 * &eBerlin, Germany 
		 * &b• The Capital of Germany •
		 * 
		 * &7&oFor the Great Nation of Germany!
		 * 
		 * &2Mayor &aLord Wowserman 
		 * &2Bank: &a$1,000,000 
		 * &2Tax: &c1,000.0 
		 * &2Town Size: &a0 / 16 
		 * &2Residents &a[5]: &fWowserman, Trel26,
		 * &fWarHalo, HelloWorld365, pootsguy101
		 */
		List<String> lores = new ArrayList<String>();
		
		if (town.hasNation()) {
			try {
				if (town.getNation().isCapital(town)) {
					lores.add(Colors.LightBlue + "• The Capital of "
							+ town.getNation().getName() + " •");
				}
			} catch (NotRegisteredException e) {

			}
		}
		
		if (!nationSpecific || rank.length == 1) {
			lores.add("§b#" + rank[0] + " Town on the Server.");
		} else {
			lores.add("§b#" + rank[0] + " Town on the Server, #" + rank[1] + " in the Nation.");
		}
		lores.add("");

		try {
			lores.add(Colors.LightGray + ChatColor.ITALIC + town.getTownBoard());
			lores.add("");
		} catch (NullPointerException e) {

		}

		lores.add(
				Colors.Green + "Mayor " + Colors.LightGreen + TownyFormatter.getFormattedName(town.getMayor()));

		// | Bank: 534 coins
		if (TownySettings.isUsingEconomy()) {
			if (TownyEconomyHandler.isActive()) {
				lores.add(Colors.Green + "Bank: " + Colors.LightGreen + town.getHoldingFormattedBalance());
				if (town.getTaxes() > 0)
					lores.add(Colors.Green + "Tax: " + Colors.Red + town.getTaxes()
							+ (town.isTaxPercentage() ? "%" : ""));
			}
		}

		lores.add(Colors.Green + "Town Size: " + Colors.LightGreen + town.getTownBlocks().size() + " / "
				+ TownySettings.getMaxTownBlocks(town));

		if (town.getWarps().keySet().size() > 0) {
			lores.add(Colors.Green + "Town Warps:");
			
			Enumeration<String> warps = town.getWarps().keys();
			
			while (warps.hasMoreElements()) {
				lores.add(Colors.LightGreen + "- /t warp " + town.getName() + " " + warps.nextElement());
			}
		}
		
		
		String[] residents = TownyFormatter.getFormattedNames(town.getResidents().toArray(new Resident[0]));
		if (residents.length > 10) {
			String[] entire = residents;
			residents = new String[10];
			System.arraycopy(entire, 0, residents, 0, 10);
			residents[9] = "and more...";
		}
		
		List<String> resLores = ChatTools.listArr(residents, Colors.Green + "Residents " + Colors.LightGreen + "["
				+ town.getNumResidents() + "]" + Colors.Green + ":" + Colors.White + " ");
		
		for (int r = 0; r < resLores.size(); r++) {
			lores.add("§f" + resLores.get(r));
		}
		
		lores.add("");
		lores.add("§bClick to Teleport to Town.");
		
		meta.setLore(lores);
		meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
		meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		banner.setItemMeta(meta);
		return banner;
	}
	
	public int getNationRank(Town town) {
		if (town.hasNation()==false)
			return 0;
		
		List<Town> totalTowns;
		try {
			totalTowns = town.getNation().getTowns();
		} catch (NotRegisteredException e) {
			return -1;
		}
		
		Collections.sort(totalTowns, new Comparator<Object>() {
			@Override
			public int compare(Object t1, Object t2) {
				if (((Town) t2).getNumResidents() == ((Town) t1).getNumResidents())
					return 0;
				return (((Town) t2).getNumResidents() > ((Town) t1).getNumResidents()) ? 1 : -1;
			}
		});
		
		for (int i = 0; i<totalTowns.size(); i++)
			if (totalTowns.get(i).getName().equals(town.getName()))
				return i + 1;
		
		return -1;
	}
	
	public int getServerRank(Town town) {
		List<Town> totalTowns = TownyUniverse.getDataSource().getTowns();
		
		Collections.sort(totalTowns, new Comparator<Object>() {
			@Override
			public int compare(Object t1, Object t2) {
				if (((Town) t2).getNumResidents() == ((Town) t1).getNumResidents())
					return 0;
				return (((Town) t2).getNumResidents() > ((Town) t1).getNumResidents()) ? 1 : -1;
			}
		});
		
		for (int i = 0; i<totalTowns.size(); i++)
			if (totalTowns.get(i).getName().equals(town.getName()))
				return i + 1;
		
		return -1;
	}
	
	private Inventory createTownMenu(int page) {
		Inventory tl = Bukkit.createInventory(null, 54, "Town-List Page: " + page);

		List<Town> townsToSort = TownyUniverse.getDataSource().getTowns();

		if (townsToSort.size() > 28 * page) {
			// Show Next Button
			ItemStack next = new ItemStack(Material.FEATHER, 1);
			ItemMeta m = next.getItemMeta();
			m.setDisplayName("§eNext Page");
			m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			next.setItemMeta(m);
			tl.setItem(53, next);
		}
		
		if (page > 1) {
			// Show Back Button
			ItemStack back = new ItemStack(Material.FEATHER, 1);
			ItemMeta m = back.getItemMeta();
			m.setDisplayName("§eBack");
			m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			back.setItemMeta(m);
			tl.setItem(45, back);
		}
		
		ItemStack close = new ItemStack(Material.BARRIER, 1);
		ItemMeta m = close.getItemMeta();
		m.setDisplayName("§eClose");
		m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		close.setItemMeta(m);
		tl.setItem(49, close);
		
		Collections.sort(townsToSort, new Comparator<Object>() {
			@Override
			public int compare(Object t1, Object t2) {
				if (((Town) t2).getNumResidents() == ((Town) t1).getNumResidents())
					return 0;
				return (((Town) t2).getNumResidents() > ((Town) t1).getNumResidents()) ? 1 : -1;
			}
		});
		int startIndex = townsToSort.size() - 28 > 0 ? townsToSort.size() - (page * 28) : 0;

		for (int i = 0; i < 28; i++) {
			if (townsToSort.size() > startIndex + i) {
				Town town = townsToSort.get(startIndex + i);

				tl.setItem(slots[i], this.getItemButtonForTown(town, false, this.getServerRank(town), this.getNationRank(town)));
			}
		}
		return tl;
	}
	
	private Inventory createNationMenu(int page) {
		Inventory tl = Bukkit.createInventory(null, 54, "Nation-List Page: " + page);

		List<Nation> nationsToSort = TownyUniverse.getDataSource().getNations();

		if (nationsToSort.size() > 28 * page) {
			// Show Next Button
			ItemStack next = new ItemStack(Material.FEATHER, 1);
			ItemMeta m = next.getItemMeta();
			m.setDisplayName("§eNext Page");
			m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			next.setItemMeta(m);
			tl.setItem(53, next);
		}

		if (page > 1) {
			// Show Back Button
			ItemStack back = new ItemStack(Material.FEATHER, 1);
			ItemMeta m = back.getItemMeta();
			m.setDisplayName("§eBack");
			m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			back.setItemMeta(m);
			tl.setItem(45, back);
		}

		ItemStack close = new ItemStack(Material.BARRIER, 1);
		ItemMeta m = close.getItemMeta();
		m.setDisplayName("§eClose");
		m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		close.setItemMeta(m);
		tl.setItem(49, close);

		Collections.sort(nationsToSort, new Comparator<Object>() {
			@Override
			public int compare(Object t1, Object t2) {
				if (((Nation) t2).getNumResidents() == ((Nation) t1).getNumResidents())
					return 0;
				return (((Nation) t2).getNumResidents() > ((Nation) t1).getNumResidents()) ? 1 : -1;
			}
		});
		int startIndex = nationsToSort.size() - 28 > 0 ? nationsToSort.size() - (page * 28) : 0;

		for (int i = 0; i < 28; i++) {
			if (nationsToSort.size() > startIndex + i) {
				Nation nation = nationsToSort.get(startIndex + i);

				ItemStack banner = nationsToSort.get(startIndex + i).getBanner();
				ItemMeta meta = banner.getItemMeta();
				meta.setDisplayName(Colors.Yellow + nation.getName());
				List<String> lores = new ArrayList<String>();

				/*
				 * &eGermany
				 * 
				 * &2Leader &aLord Wowserman &2Bank: &a$1,000,000 &2Tax:
				 * &c1,000.0 &2Town Size: &a0 / 16 &2Residents &a[5]:
				 * &fWowserman, Trel26, &fWarHalo, HelloWorld365, pootsguy101
				 */

				lores.add("§b#" + (startIndex + i + 1) + " Nation on the Server.");
				
				lores.add(Colors.Green + "Leader " + Colors.LightGreen + nation.getCapital().getMayor());

				if (nation.getAssistants().size() > 0)
					lores.addAll(ChatTools.listArr(
							TownyFormatter.getFormattedNames(nation.getAssistants().toArray(new Resident[0])),
							Colors.Green + "Assistants:" + Colors.White + " "));

				// | Bank: 534 coins
				if (TownySettings.isUsingEconomy()) {
					if (TownyEconomyHandler.isActive()) {
						lores.add(Colors.Green + "Bank: " + Colors.LightGreen + nation.getHoldingFormattedBalance());
						if (nation.getTaxes() > 0)
							lores.add(Colors.Green + "Tax: " + Colors.Red + nation.getTaxes());
					}
				}
				
				String[] towns = TownyFormatter.getFormattedNames(nation.getTowns().toArray(new Town[0]));
				if (towns.length > 10) {
					String[] entire = towns;
					towns = new String[10];
					System.arraycopy(entire, 0, towns, 0, 10);
					towns[9] = "and more...";
				}
				
				List<String> townLores = ChatTools.listArr(towns, Colors.Green + "Towns " + Colors.LightGreen + "["
						+ nation.getNumTowns() + "]" + Colors.Green + ":" + Colors.White + " ");

				for (int r = 0; r < townLores.size(); r++) {
					lores.add("§f" + townLores.get(r));
				}
				
				String[] allies = TownyFormatter.getFormattedNames(nation.getAllies().toArray(new Nation[0]));
				if (allies.length > 10) {
					String[] entire = allies;
					allies = new String[10];
					System.arraycopy(entire, 0, allies, 0, 10);
					allies[9] = "and more...";
				}
				
				List<String> alliesLores = ChatTools.listArr(allies, Colors.Green + "Allies " + Colors.LightGreen + "["
						+ nation.getAllies().size() + "]" + Colors.Green + ":" + Colors.White + " ");

				for (int a = 0; a < alliesLores.size(); a++) {
					lores.add("§f" + alliesLores.get(a));
				}
				
				String[] enemies = TownyFormatter.getFormattedNames(nation.getEnemies().toArray(new Nation[0]));
				if (allies.length > 10) {
					String[] entire = enemies;
					enemies = new String[10];
					System.arraycopy(entire, 0, enemies, 0, 10);
					enemies[9] = "and more...";
				}
				
				List<String> enemiesLores = ChatTools.listArr(enemies, Colors.Green + "Enemies " + Colors.LightGreen + "["
						+ nation.getAllies().size() + "]" + Colors.Green + ":" + Colors.White + " ");

				for (int e = 0; e < enemiesLores.size(); e++) {
					lores.add("§f" + enemiesLores.get(e));
				}

				lores.add("");
				lores.add("§bClick to View Nation's Towns.");
				
				meta.setLore(lores);
				meta.addItemFlags(ItemFlag.HIDE_POTION_EFFECTS);
				meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
				banner.setItemMeta(meta);
				tl.setItem(slots[i], banner);
			}
		}
		return tl;
	}
	
	private Inventory createNationsTownsMenu(Nation nation, int page) {
		Inventory tl = Bukkit.createInventory(null, 54, nation.getName() + "-Towns-List Page: " + page);
				
		List<Town> townsToSort = nation.getTowns();

		if (townsToSort.size() > 28 * page) {
			// Show Next Button
			ItemStack next = new ItemStack(Material.FEATHER, 1);
			ItemMeta m = next.getItemMeta();
			m.setDisplayName("§eNext Page");
			m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			next.setItemMeta(m);
			tl.setItem(53, next);
		}

		if (page > 1) {
			// Show Back Button
			ItemStack back = new ItemStack(Material.FEATHER, 1);
			ItemMeta m = back.getItemMeta();
			m.setDisplayName("§eBack");
			m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
			back.setItemMeta(m);
			tl.setItem(45, back);
		}

		ItemStack close = new ItemStack(Material.BARRIER, 1);
		ItemMeta m = close.getItemMeta();
		m.setDisplayName("§eClose");
		m.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
		close.setItemMeta(m);
		tl.setItem(49, close);

		Collections.sort(townsToSort, new Comparator<Object>() {
			@Override
			public int compare(Object t1, Object t2) {
				if (((Town) t2).getNumResidents() == ((Town) t1).getNumResidents())
					return 0;
				return (((Town) t2).getNumResidents() > ((Town) t1).getNumResidents()) ? 1 : -1;
			}
		});
		
		int startIndex = townsToSort.size() - 28 > 0 ? townsToSort.size() - (page * 28) : 0;

		for (int i = 0; i < 28; i++) {
			if (townsToSort.size() > startIndex + i) {
				Town town = townsToSort.get(startIndex + i);

				tl.setItem(slots[i], this.getItemButtonForTown(town, true, this.getServerRank(town), this.getNationRank(town)));
			}
		}
		
		return tl;
	}
}
