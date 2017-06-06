package com.palmergames.bukkit.towny;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

import org.bukkit.entity.Player;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;

public class TownyFormatter {

	// private static Towny plugin = null;

	public static final SimpleDateFormat lastOnlineFormat = new SimpleDateFormat("MMMMM dd '@' HH:mm");
	public static final SimpleDateFormat registeredFormat = new SimpleDateFormat("MMM d yyyy");

	/**
	 * 1 = Description 2 = Count
	 * 
	 * Colours: 3 = Description and : 4 = Count 5 = Colour for the start of the
	 * list
	 */
	public static final String residentListPrefixFormat = "%3$s%1$s %4$s[%2$d]%3$s:%5$s ";

	public static void initialize(Towny plugin) {

		// TownyFormatter.plugin = plugin;
	}

	public static List<String> getFormattedOnlineResidents(String prefix, ResidentList residentList, Player player) {

		List<Resident> onlineResidents = TownyUniverse.getOnlineResidentsViewable(player, residentList);
		return getFormattedResidents(prefix, onlineResidents);
	}

	public static List<String> getFormattedResidents(Town town) {

		List<String> out = new ArrayList<String>();

		String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));

		out.addAll(ChatTools.listArr(residents, Colors.Green + "Residents " + Colors.LightGreen + "[" + town.getNumResidents() + "]" + Colors.Green + ":" + Colors.White + " "));

		return out;

	}

	public static List<String> getFormattedOutlaws(Town town) {

		List<String> out = new ArrayList<String>();

		String[] residents = getFormattedNames(town.getOutlaws().toArray(new Resident[0]));

		out.addAll(ChatTools.listArr(residents, Colors.Green + "Outlaws: " + Colors.White + " "));

		return out;

	}
	
	public static List<String> getFormattedResidents(String prefix, List<Resident> residentList) {

		return ChatTools.listArr(getFormattedNames(residentList), String.format(residentListPrefixFormat, prefix, residentList.size(), Colors.Green, Colors.LightGreen, Colors.White));
	}

	public static String[] getFormattedNames(List<Resident> residentList) {

		return getFormattedNames(residentList.toArray(new Resident[0]));
	}

	public static String getTime() {

		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
		return sdf.format(System.currentTimeMillis());
	}

	/**
	 * 
	 * @param townBlock
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(TownBlock townBlock) {

		List<String> out = new ArrayList<String>();

		try {
			TownBlockOwner owner;
			Town town = townBlock.getTown();
			TownyWorld world = townBlock.getWorld();

			if (townBlock.hasResident()) {
				owner = townBlock.getResident();
			} else {
				owner = townBlock.getTown();
			}

			out.add(ChatTools.formatTitle(TownyFormatter.getFormattedName(owner) + ((BukkitTools.isOnline(owner.getName())) ? Colors.LightGreen + " (Online)" : "")));
			out.add(Colors.Green + " Perm: " + ((owner instanceof Resident) ? townBlock.getPermissions().getColourString() : townBlock.getPermissions().getColourString().replace("f", "r")));
			out.add(Colors.Green + "PvP: " + ((town.isPVP() || world.isForcePVP() || townBlock.getPermissions().pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((world.isForceExpl() || townBlock.getPermissions().explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((town.isFire() || world.isForceFire() || townBlock.getPermissions().fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((town.hasMobs() || world.isForceTownMobs() || townBlock.getPermissions().mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

		} catch (NotRegisteredException e) {
			out.add("Error: " + e.getMessage());
		}

		return out;
	}

	/**
	 * 
	 * @param resident
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(Resident resident, Player player) {

		List<String> out = new ArrayList<String>();

		// ___[ King Harlus ]___
		out.add(ChatTools.formatTitle(getFormattedName(resident) + ((BukkitTools.isOnline(resident.getName()) && (player != null) && (player.canSee(BukkitTools.getPlayer(resident.getName())))) ? Colors.LightGreen + " (Online)" : "")));

		// Registered: Sept 3 2009 | Last Online: March 7 @ 14:30
		out.add(Colors.Green + "Registered: " + Colors.LightGreen + registeredFormat.format(resident.getRegistered()) + Colors.Gray + " | " + Colors.Green + "Last Online: " + Colors.LightGreen + lastOnlineFormat.format(resident.getLastOnline()));

		// Owner of: 4 plots
		// Perm: Build = f-- Destroy = fa- Switch = fao Item = ---
		// if (resident.getTownBlocks().size() > 0) {
		out.add(Colors.Green + "Owner of: " + Colors.LightGreen + resident.getTownBlocks().size() + " plots");
		out.add(Colors.Green + "    Perm: " + resident.getPermissions().getColourString());
		out.add(Colors.Green + "PVP: " + ((resident.getPermissions().pvp) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Explosions: " + ((resident.getPermissions().explosion) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((resident.getPermissions().fire) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((resident.getPermissions().mobs) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));
		// }

		// Bank: 534 coins
		if (TownySettings.isUsingEconomy())
			if (TownyEconomyHandler.isActive())
				out.add(Colors.Green + "Bank: " + Colors.LightGreen + resident.getHoldingFormattedBalance());

		// Town: Camelot
		String line = Colors.Green + "Town: " + Colors.LightGreen;
		if (!resident.hasTown())
			line += "None";
		else
			try {
				line += getFormattedName(resident.getTown());
			} catch (TownyException e) {
				line += "Error: " + e.getMessage();
			}
		out.add(line);
		
		// Town ranks
		if (resident.hasTown()) {
			if (!resident.getTownRanks().isEmpty())
				out.add(Colors.Green + "Town Ranks: " + Colors.LightGreen + StringMgmt.join(resident.getTownRanks(), ","));
		}
		
		//Nation ranks
		if (resident.hasNation()) {
			if (!resident.getNationRanks().isEmpty())
				out.add(Colors.Green + "Nation Ranks: " + Colors.LightGreen + StringMgmt.join(resident.getNationRanks(), ","));
		}
		
		// Jailed: yes if they are jailed.
		if (resident.isJailed()){
			out.add(Colors.Green + "Jailed: Yes" + " in Town: " + resident.getJailTown());
		}
		
		// Friends [12]: James, Carry, Mason
		List<Resident> friends = resident.getFriends();
		out.addAll(getFormattedResidents("Friends", friends));
		

		return out;
	}

	/**
	 * Returns a Chat Formatted List of all town residents who hold a rank.
	 * 
	 * @param town
	 * @return a list containing formatted rank data.
	 */
	public static List<String> getRanks(Town town) {

		List<String> ranklist = new ArrayList<String>();

		String towntitle = getFormattedName(town);
		towntitle += Colors.Blue + " Rank List";
		ranklist.add(ChatTools.formatTitle(towntitle));
		ranklist.add(Colors.Green + "Mayor: " + Colors.LightGreen + getFormattedName(town.getMayor()));

		List<Resident> residents = town.getResidents();
		List<String> townranks = TownyPerms.getTownRanks();
		List<Resident> residentwithrank = new ArrayList<Resident>();

		for (String rank : townranks) {
			for (Resident r : residents) {

				if ((r.getTownRanks() != null) && (r.getTownRanks().contains(rank))) {
					residentwithrank.add(r);
				}
			}
			ranklist.addAll(getFormattedResidents(rank, residentwithrank));
			residentwithrank.clear();
		}
		return ranklist;
	}

	/**
	 * 
	 * @param town
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(Town town) {

		List<String> out = new ArrayList<String>();

		TownyWorld world = town.getWorld();

		// ___[ Raccoon City (PvP) (Open) ]___
		String title = getFormattedName(town);
		title += ((!town.isAdminDisabledPVP()) && ((town.isPVP() || town.getWorld().isForcePVP())) ? Colors.Red + " (PvP)" : "");
		title += (town.isOpen() ? Colors.LightBlue + " (Open)" : "");
		out.add(ChatTools.formatTitle(title));

		// Lord: Mayor Quimby
		// Board: Get your fried chicken
		try {
			out.add(Colors.Green + "Board: " + Colors.LightGreen + town.getTownBoard());
		} catch (NullPointerException e) {
		}

		// Town Size: 0 / 16 [Bought: 0/48] [Bonus: 0] [Home: 33,44]
		try {
			out.add(Colors.Green + "Town Size: " + Colors.LightGreen + town.getTownBlocks().size() + " / " + TownySettings.getMaxTownBlocks(town) + (TownySettings.isSellingBonusBlocks() ? Colors.LightBlue + " [Bought: " + town.getPurchasedBlocks() + "/" + TownySettings.getMaxPurchedBlocks() + "]" : "") + (town.getBonusBlocks() > 0 ? Colors.LightBlue + " [Bonus: " + town.getBonusBlocks() + "]" : "") + ((TownySettings.getNationBonusBlocks(town) > 0) ? Colors.LightBlue + " [NationBonus: " + TownySettings.getNationBonusBlocks(town) + "]" : "") + (town.isPublic() ? Colors.LightGray + " [Home: " + (town.hasHomeBlock() ? town.getHomeBlock().getCoord().toString() : "None") + "]" : ""));
		} catch (TownyException e) {
		}

		if (town.hasOutpostSpawn())
			out.add(Colors.Green + "Outposts: " + Colors.LightGreen + town.getMaxOutpostSpawn());

		// Permissions: B=rao D=--- S=ra-
		out.add(Colors.Green + "Permissions: " + town.getPermissions().getColourString().replace("f", "r"));
		out.add(Colors.Green + "Explosions: " + ((town.isBANG() || world.isForceExpl()) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Firespread: " + ((town.isFire() || world.isForceFire()) ? Colors.Red + "ON" : Colors.LightGreen + "OFF") + Colors.Green + "  Mob Spawns: " + ((town.hasMobs() || world.isForceTownMobs()) ? Colors.Red + "ON" : Colors.LightGreen + "OFF"));

		// | Bank: 534 coins
		String bankString = "";
		if (TownySettings.isUsingEconomy()) {
			if (TownyEconomyHandler.isActive()) {
				bankString = Colors.Green + "Bank: " + Colors.LightGreen + town.getHoldingFormattedBalance();
				if (town.hasUpkeep())
					bankString += Colors.Gray + " | " + Colors.Green + "Daily upkeep: " + Colors.Red + TownySettings.getTownUpkeepCost(town);
				bankString += Colors.Gray + " | " + Colors.Green + "Tax: " + Colors.Red + town.getTaxes() + (town.isTaxPercentage() ? "%" : "");
			}
			out.add(bankString);
		}

		// Mayor: MrSand | Bank: 534 coins
		out.add(Colors.Green + "Mayor: " + Colors.LightGreen + getFormattedName(town.getMayor()));

		// Assistants [2]: Sammy, Ginger
		// if (town.getAssistants().size() > 0)
		// out.addAll(getFormattedResidents("Assistants",
		// town.getAssistants()));

		List<String> ranklist = new ArrayList<String>();
		List<Resident> residentss = town.getResidents();
		List<String> townranks = TownyPerms.getTownRanks();
		List<Resident> residentwithrank = new ArrayList<Resident>();

		for (String rank : townranks) {
			for (Resident r : residentss) {

				if ((r.getTownRanks() != null) && (r.getTownRanks().contains(rank))) {
					residentwithrank.add(r);
				}
			}
			ranklist.addAll(getFormattedResidents(rank, residentwithrank));
			residentwithrank.clear();
		}

		out.addAll(ranklist);

		// Nation: Azur Empire
		try {
			out.add(Colors.Green + "Nation: " + Colors.LightGreen + getFormattedName(town.getNation()));
		} catch (TownyException e) {
		}

		// Residents [12]: James, Carry, Mason

		String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));
		if (residents.length > 34) {
			String[] entire = residents;
			residents = new String[36];
			System.arraycopy(entire, 0, residents, 0, 35);
			residents[35] = "and more...";
		}
		out.addAll(ChatTools.listArr(residents, Colors.Green + "Residents " + Colors.LightGreen + "[" + town.getNumResidents() + "]" + Colors.Green + ":" + Colors.White + " "));
		return out;
	}

	/**
	 * 
	 * @param nation
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(Nation nation) {

		List<String> out = new ArrayList<String>();

		// ___[ Azur Empire ]___
		out.add(ChatTools.formatTitle(getFormattedName(nation)));

		// Bank: 534 coins
		String line = "";
		if (TownySettings.isUsingEconomy())
			if (TownyEconomyHandler.isActive()) {
				line = Colors.Green + "Bank: " + Colors.LightGreen + nation.getHoldingFormattedBalance();

				if (TownySettings.getNationUpkeepCost(nation) > 0)
					line += (Colors.Gray + " | " + Colors.Green + "Daily upkeep: " + Colors.Red + TownySettings.getNationUpkeepCost(nation));

			}

		if (nation.isNeutral()) {
			if (line.length() > 0)
				line += Colors.Gray + " | ";
			line += Colors.LightGray + "Peaceful";
		}
		// Bank: 534 coins | Peaceful
		if (line.length() > 0)
			out.add(line);

		// King: King Harlus
		if (nation.getNumTowns() > 0 && nation.hasCapital() && nation.getCapital().hasMayor())
			out.add(Colors.Green + "King: " + Colors.LightGreen + getFormattedName(nation.getCapital().getMayor()) + Colors.Green + "  NationTax: " + Colors.Red + nation.getTaxes());
		// Assistants: Mayor Rockefel, Sammy, Ginger
		if (nation.getAssistants().size() > 0)
			out.addAll(ChatTools.listArr(getFormattedNames(nation.getAssistants().toArray(new Resident[0])), Colors.Green + "Assistants:" + Colors.White + " "));
		// Towns [44]: James City, Carry Grove, Mason Town
		out.addAll(ChatTools.listArr(getFormattedNames(nation.getTowns().toArray(new Town[0])), Colors.Green + "Towns " + Colors.LightGreen + "[" + nation.getNumTowns() + "]" + Colors.Green + ":" + Colors.White + " "));
		// Allies [4]: James Nation, Carry Territory, Mason Country
		out.addAll(ChatTools.listArr(getFormattedNames(nation.getAllies().toArray(new Nation[0])), Colors.Green + "Allies " + Colors.LightGreen + "[" + nation.getAllies().size() + "]" + Colors.Green + ":" + Colors.White + " "));
		// Enemies [4]: James Nation, Carry Territory, Mason Country
		out.addAll(ChatTools.listArr(getFormattedNames(nation.getEnemies().toArray(new Nation[0])), Colors.Green + "Enemies " + Colors.LightGreen + "[" + nation.getEnemies().size() + "]" + Colors.Green + ":" + Colors.White + " "));

		return out;
	}

	/**
	 * 
	 * @param world
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(TownyWorld world) {

		List<String> out = new ArrayList<String>();

		// ___[ World (PvP) ]___
		String title = getFormattedName(world);
		title += ((world.isPVP() || world.isForcePVP()) ? Colors.Red + " (PvP)" : "");
		title += (world.isClaimable() ? Colors.LightGreen + " Claimable" : Colors.Rose + " NoClaims");
		out.add(ChatTools.formatTitle(title));

		if (!world.isUsingTowny()) {
			out.add(TownySettings.getLangString("msg_set_use_towny_off"));
		} else {
			// ForcePvP: No | Fire: Off
			out.add(Colors.Green + "ForcePvP: " + (world.isForcePVP() ? Colors.Rose + "On" : Colors.LightGreen + "Off") + Colors.Gray + " | " + Colors.Green + "Fire: " + (world.isFire() ? Colors.Rose + "On" : Colors.LightGreen + "Off") + Colors.Gray + " | " + Colors.Green + "Force Fire: " + (world.isForceFire() ? Colors.Rose + "Forced" : Colors.LightGreen + "Adjustable"));

			out.add(Colors.Green + "Explosions: " + (world.isExpl() ? Colors.Rose + "On:" : Colors.LightGreen + "Off") + Colors.Gray + " | " + Colors.Green + " Force explosion: " + (world.isForceExpl() ? Colors.Rose + "Forced" : Colors.LightGreen + "Adjustable"));
			out.add(Colors.Green + "World Mobs: " + (world.hasWorldMobs() ? Colors.Rose + "On" : Colors.LightGreen + "Off") + Colors.Gray + " | " + Colors.Green + "Force TownMobs: " + (world.isForceTownMobs() ? Colors.Rose + "Forced" : Colors.LightGreen + "Adjustable"));
			// Using Default Settings: Yes
			// out.add(Colors.Green + "Using Default Settings: " +
			// (world.isUsingDefault() ? Colors.LightGreen + "Yes" : Colors.Rose
			// + "No"));

			out.add(Colors.Green + "Unclaim Revert: " + (world.isUsingPlotManagementRevert() ? Colors.LightGreen + "On" : Colors.Rose + "off") + Colors.Gray + " | " + Colors.Green + "Explosion Revert: " + (world.isUsingPlotManagementWildRevert() ? Colors.LightGreen + "On" : Colors.Rose + "off"));
			// Wilderness:
			// Build, Destroy, Switch
			// Ignored Blocks: 34, 45, 64
			out.add(Colors.Green + world.getUnclaimedZoneName() + ":");
			out.add("    " + (world.getUnclaimedZoneBuild() ? Colors.LightGreen : Colors.Rose) + "Build" + Colors.Gray + ", " + (world.getUnclaimedZoneDestroy() ? Colors.LightGreen : Colors.Rose) + "Destroy" + Colors.Gray + ", " + (world.getUnclaimedZoneSwitch() ? Colors.LightGreen : Colors.Rose) + "Switch" + Colors.Gray + ", " + (world.getUnclaimedZoneItemUse() ? Colors.LightGreen : Colors.Rose) + "ItemUse");
			out.add("    " + Colors.Green + "Ignored Blocks:" + Colors.LightGreen + " " + StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), ", "));
		}
		return out;
	}

	/**
	 * Returns the tax info this resident will have to pay at the next new day.
	 * 
	 * @param resident
	 * @return tax status message
	 */
	public static List<String> getTaxStatus(Resident resident) {

		List<String> out = new ArrayList<String>();
		Town town = null;

		double plotTax = 0.0;

		out.add(ChatTools.formatTitle(getFormattedName(resident) + ((BukkitTools.isOnline(resident.getName())) ? Colors.LightGreen + " (Online)" : "")));

		if (resident.hasTown()) {
			try {
				town = resident.getTown();
				out.add(Colors.Green + "Owner of: " + Colors.LightGreen + resident.getTownBlocks().size() + " plots");

				if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt")) {
					out.add(Colors.Green + "Staff are exempt from paying town taxes.");
				} else {
					if (town.isTaxPercentage()) {
						out.add(Colors.Green + "Town Tax: " + Colors.LightGreen + (resident.getHoldingBalance() * town.getTaxes() / 100));
					} else {
						out.add(Colors.Green + "Town Tax: " + Colors.LightGreen + town.getTaxes());

						if ((resident.getTownBlocks().size() > 0)) {

							for (TownBlock townBlock : new ArrayList<TownBlock>(resident.getTownBlocks())) {
								plotTax += townBlock.getType().getTax(townBlock.getTown());
							}

							out.add(Colors.Green + "Total Plot Taxes: " + Colors.LightGreen + plotTax);
						}
						out.add(Colors.Green + "Total Tax to pay: " + Colors.LightGreen + (town.getTaxes() + plotTax));
					}
				}

			} catch (NotRegisteredException e) {
				// Failed to fetch town
			} catch (EconomyException e) {
				// Economy failed
			}
		}

		return out;
	}

	public static String getNamePrefix(Resident resident) {

		if (resident == null)
			return "";
		if (resident.isKing())
			return TownySettings.getKingPrefix(resident);
		else if (resident.isMayor())
			return TownySettings.getMayorPrefix(resident);
		return "";
	}

	public static String getNamePostfix(Resident resident) {

		if (resident == null)
			return "";
		if (resident.isKing())
			return TownySettings.getKingPostfix(resident);
		else if (resident.isMayor())
			return TownySettings.getMayorPostfix(resident);
		return "";
	}

	public static String getFormattedName(TownyObject obj) {

		if (obj == null)
			return "Null";
		else if (obj instanceof Resident)
			return getFormattedResidentName((Resident) obj);
		else if (obj instanceof Town)
			return getFormattedTownName((Town) obj);
		else if (obj instanceof Nation)
			return getFormattedNationName((Nation) obj);
		// System.out.println("just name: " + obj.getName());
		return obj.getName().replaceAll("_", " ");
	}

	public static String getFormattedResidentName(Resident resident) {

		if (resident == null)
			return "null";
		if (resident.isKing())
			return TownySettings.getKingPrefix(resident) + resident.getName().replaceAll("_", " ") + TownySettings.getKingPostfix(resident);
		else if (resident.isMayor())
			return TownySettings.getMayorPrefix(resident) + resident.getName().replaceAll("_", " ") + TownySettings.getMayorPostfix(resident);
		return resident.getName().replaceAll("_", " ");
	}

	public static String getFormattedTownName(Town town) {

		if (town.isCapital())
			return TownySettings.getCapitalPrefix(town) + town.getName().replaceAll("_", " ") + TownySettings.getCapitalPostfix(town);
		return TownySettings.getTownPrefix(town) + town.getName().replaceAll("_", " ") + TownySettings.getTownPostfix(town);
	}

	public static String getFormattedNationName(Nation nation) {

		return TownySettings.getNationPrefix(nation) + nation.getName().replaceAll("_", " ") + TownySettings.getNationPostfix(nation);
	}

	public static String[] getFormattedNames(Resident[] residents) {

		List<String> names = new ArrayList<String>();
		for (Resident resident : residents)
			names.add(getFormattedName(resident));
		return names.toArray(new String[0]);
	}

	public static String[] getFormattedNames(Town[] towns) {

		List<String> names = new ArrayList<String>();
		for (Town town : towns)
			names.add(getFormattedName(town));
		return names.toArray(new String[0]);
	}

	public static String[] getFormattedNames(Nation[] nations) {

		List<String> names = new ArrayList<String>();
		for (Nation nation : nations)
			names.add(getFormattedName(nation));
		return names.toArray(new String[0]);
	}
}
