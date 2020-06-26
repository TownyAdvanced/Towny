package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Residence;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import org.bukkit.entity.Player;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class TownyFormatter {

	// private static Towny plugin = null;

	public static final SimpleDateFormat lastOnlineFormat = new SimpleDateFormat("MMMMM dd '@' HH:mm");
	public static final SimpleDateFormat lastOnlineFormatIncludeYear = new SimpleDateFormat("MMMMM dd yyyy");
	public static final SimpleDateFormat registeredFormat = new SimpleDateFormat("MMM d yyyy");

	/**
	 * 1 = Description 2 = Count
	 * 
	 * Colours: 3 = Description and : 4 = Count 5 = Colour for the start of the
	 * list
	 */
	public static final String residentListPrefixFormat = "%3$s%1$s %4$s[%2$d]%3$s:%5$s ";
    public static final String embassyTownListPrefixFormat = "%3$s%1$s:%5$s ";

	public static void initialize() {}

	public static List<String> getFormattedOnlineResidents(String prefix, Residence residence, Player player) {
		List<Resident> onlineResidents = ResidentUtil.getOnlineResidentsViewable(player, residence);
		return getFormattedResidents(prefix, onlineResidents);
	}

	public static List<String> getFormattedResidents(Town town) {
		String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));

		return new ArrayList<>(ChatTools.listArr(residents, Colors.Green + Translation.of("res_list") + " " + Colors.LightGreen + "[" + town.getNumResidents() + "]" + Colors.Green + ":" + Colors.White + " "));

	}

	public static List<String> getFormattedOutlaws(Town town) {

		String[] residents = getFormattedNames(town.getOutlaws().toArray(new Resident[0]));

		return new ArrayList<>(ChatTools.listArr(residents, Translation.of("outlaws") + " "));

	}
	
	public static List<String> getFormattedResidents(String prefix, List<Resident> residentList) {

		return ChatTools.listArr(getFormattedNames(residentList), String.format(residentListPrefixFormat, prefix, residentList.size(), Translation.of("res_format_list_1"), Translation.of("res_format_list_2"), Translation.of("res_format_list_3")));
	}
	
	public static List<String> getFormattedTowns(String prefix, List<Town> townList) {
		
		Town[] arrayTowns = townList.toArray(new Town[0]);

		return ChatTools.listArr(getFormattedNames(arrayTowns), String.format(embassyTownListPrefixFormat, prefix, townList.size(), Translation.of("res_format_list_1"), Translation.of("res_format_list_2"), Translation.of("res_format_list_3")));
	}

	public static String[] getFormattedNames(List<Resident> residentList) {

		return getFormattedNames(residentList.toArray(new Resident[0]));
	}

	public static String getTime() {

		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
		return sdf.format(System.currentTimeMillis());
	}

	/**
	 * Gets the status screen of a TownBlock
	 * 
	 * @param townBlock the TownBlock to check
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(TownBlock townBlock) {

		List<String> out = new ArrayList<>();
		
		try {
			TownyObject owner;
			Town town = townBlock.getTown();
			TownyWorld world = townBlock.getWorld();
			boolean preventPVP = CombatUtil.preventPvP(world, townBlock);

			if (townBlock.hasResident()) {
				owner = townBlock.getResident();
			} else {
				owner = townBlock.getTown();
			}
			

			out.add(ChatTools.formatTitle(owner.getFormattedName() + ((BukkitTools.isOnline(owner.getName())) ? Translation.of("online") : "")));
			if (!townBlock.getType().equals(TownBlockType.RESIDENTIAL))
				out.add(Translation.of("status_plot_type") + townBlock.getType().toString());							
			out.add(Translation.of("status_perm") + ((owner instanceof Resident) ? townBlock.getPermissions().getColourString().replace("n", "t") : townBlock.getPermissions().getColourString().replace("f", "r")));
			out.add(Translation.of("status_perm") + ((owner instanceof Resident) ? townBlock.getPermissions().getColourString2().replace("n", "t") : townBlock.getPermissions().getColourString2().replace("f", "r")));
			out.add(Translation.of("status_pvp") + ((!preventPVP) ? Translation.of("status_on"): Translation.of("status_off")) + 
					Translation.of("explosions") + ((world.isForceExpl() || townBlock.getPermissions().explosion) ? Translation.of("status_on"): Translation.of("status_off")) + 
					Translation.of("firespread") + ((town.isFire() || world.isForceFire() || townBlock.getPermissions().fire) ? Translation.of("status_on"):Translation.of("status_off")) + 
					Translation.of("mobspawns") + ((world.isForceTownMobs() || townBlock.getPermissions().mobs) ?  Translation.of("status_on"): Translation.of("status_off")));

			if (townBlock.hasPlotObjectGroup())
				out.add(Translation.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size()));
			out.addAll(getExtraFields(townBlock));
		} catch (NotRegisteredException e) {
			out.add("Error: " + e.getMessage());
		}
		out = formatStatusScreens(out);
		return out;
	}

	/**
	 *  Gets the status screen of a Resident
	 *  
	 * @param resident the resident to check the status of
	 * @param player make sure the resident is an online player
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(Resident resident, Player player) {

		List<String> out = new ArrayList<>();

		// ___[ King Harlus ]___
		out.add(ChatTools.formatTitle(resident.getFormattedName() + ((BukkitTools.isOnline(resident.getName()) && (player != null) && (player.canSee(BukkitTools.getPlayer(resident.getName())))) ? Translation.of("online2") : "")));

		// First used if last online is this year, 2nd used if last online is early than this year.
		// Registered: Sept 3 2009 | Last Online: March 7 @ 14:30
		// Registered: Sept 3 2009 | Last Online: March 7 2009
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(resident.getLastOnline());
		int currentYear = cal.get(Calendar.YEAR);
		cal.setTimeInMillis(System.currentTimeMillis());
		int lastOnlineYear = cal.get(Calendar.YEAR);
		if (currentYear == lastOnlineYear)
			out.add(Translation.of("registered_last_online", registeredFormat.format(resident.getRegistered()), lastOnlineFormat.format(resident.getLastOnline())));
		else 
			out.add(Translation.of("registered_last_online", registeredFormat.format(resident.getRegistered()), lastOnlineFormatIncludeYear.format(resident.getLastOnline())));

		// Owner of: 4 plots
		// Perm: Build = f-- Destroy = fa- Switch = fao Item = ---
		// if (resident.getTownBlocks().size() > 0) {
		out.add(Translation.of("owner_of_x_plots", resident.getTownBlocks().size()));
		out.add(Translation.of("status_perm") + resident.getPermissions().getColourString().replace("n", "t"));
		out.add(Translation.of("status_perm") + resident.getPermissions().getColourString2().replace("n", "t"));
		out.add(Translation.of("status_pvp") + ((resident.getPermissions().pvp) ? Translation.of("status_on"): Translation.of("status_off")) + 
				Translation.of("explosions") + ((resident.getPermissions().explosion) ? Translation.of("status_on"): Translation.of("status_off")) + 
				Translation.of("firespread") + ((resident.getPermissions().fire) ? Translation.of("status_on"): Translation.of("status_off")) + 
				Translation.of("mobspawns") + ((resident.getPermissions().mobs) ? Translation.of("status_on"): Translation.of("status_off")));
		// }

		// Bank: 534 coins
		if (TownySettings.isUsingEconomy())
			if (TownyEconomyHandler.isActive())
				out.add(Translation.of("status_bank", resident.getAccount().getHoldingFormattedBalance()));

		// Town: Camelot
		String line = Translation.of("status_town");
		if (!resident.hasTown())
			line += Translation.of("status_no_town");
		else
			try {
				line += resident.getTown().getFormattedName();
			} catch (TownyException e) {
				line += "Error: " + e.getMessage();
			}
		out.add(line);
		
		// Embassies in: Camelot, London, Tokyo
		List<Town> townEmbassies = new ArrayList<>();
		try {
			
			String actualTown = resident.hasTown() ? resident.getTown().getName() : "";
			
			for(TownBlock tB : resident.getTownBlocks()) {
				if(!actualTown.equals(tB.getTown().getName()) && !townEmbassies.contains(tB.getTown())) {
					
					townEmbassies.add(tB.getTown());
				
				}
				
			}
		} catch (NotRegisteredException ignored) {}
		
		if (townEmbassies.size() > 0) {
			out.addAll(getFormattedTowns(Translation.of("status_embassy_town"), townEmbassies));
		}
			
		// Town ranks
		if (resident.hasTown()) {
			if (!resident.getTownRanks().isEmpty())
				out.add(Translation.of("status_town_ranks") + StringMgmt.capitalize(StringMgmt.join(resident.getTownRanks(), ", ")));
		}
		
		//Nation ranks
		if (resident.hasNation()) {
			if (!resident.getNationRanks().isEmpty())
				out.add(Translation.of("status_nation_ranks") + StringMgmt.capitalize(StringMgmt.join(resident.getNationRanks(), ", ")));
		}
		
		// Jailed: yes if they are jailed.
		if (resident.isJailed()){
			out.add(Translation.of("jailed_in_town", resident.getJailTown()) + ( resident.hasJailDays() ? Translation.of("msg_jailed_for_x_days", resident.getJailDays()) :  ""));
		}
		
		// Friends [12]: James, Carry, Mason
		List<Resident> friends = resident.getFriends();
		out.addAll(getFormattedResidents(Translation.of("status_friends"), friends));
		
		out.addAll(getExtraFields(resident));
		
		out = formatStatusScreens(out);
		return out;
	}

	/**
	 * Returns a Chat Formatted List of all town residents who hold a rank.
	 * 
	 * @param town the town for which to check against.
	 * @return a list containing formatted rank data.
	 */
	public static List<String> getRanks(Town town) {

		List<String> ranklist = new ArrayList<>();

		String towntitle = town.getFormattedName();
		towntitle += Translation.of("rank_list_title");
		ranklist.add(ChatTools.formatTitle(towntitle));
		ranklist.add(Translation.of("rank_list_mayor", town.getMayor().getFormattedName()));

		getRanks(town, ranklist);
		return ranklist;
	}

	private static void getRanks(Town town, List<String> ranklist) {
		List<Resident> residents = town.getResidents();
		List<String> townRanks = TownyPerms.getTownRanks();
		List<Resident> residentWithRank = new ArrayList<>();

		for (String rank : townRanks) {
			for (Resident r : residents) {

				if ((r.getTownRanks() != null) && (r.getTownRanks().contains(rank))) {
					residentWithRank.add(r);
				}
			}
			ranklist.addAll(getFormattedResidents(StringMgmt.capitalize(rank), residentWithRank));
			residentWithRank.clear();
		}
	}

	/**
	 * 
	 * @param out - List&lt;String&gt;
	 * @return a string list with all lines split at a middle-ish " " and shortened so that they fit equally into 80 characters chat line.
	 * Please do not pass this anything longer than 159 characters. 
	 */
	public static List<String> formatStatusScreens(List<String> out) {
		
		List<String> formattedOut = new ArrayList<>();
		for (String line: out) {
			if (line.length() > 80) {
				int middle = (line.length()/2);
				int before = line.lastIndexOf(' ', middle);
				int after = line.lastIndexOf(' ', middle + 1);
				if (middle - before < after - middle) 
					middle = before;
				else
					middle = after;
				
				String first = line.substring(0, middle);
				String second = line.substring(middle + 1);
				formattedOut.add(first);
				formattedOut.add(second);					
			} else {
				formattedOut.add(line);
			}
		}
		return formattedOut;
	}	
	
	/**
	 * Gets the status screen of a Town
	 * 
	 * @param town the town in which to check
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(Town town) {

		List<String> out = new ArrayList<>();

		TownyWorld world;
		try {
			world = town.getHomeblockWorld();
		} catch (NullPointerException e) {
			// Some towns can have no homeblock, causing getWorld() to return null.
			// We're going to supplant the first TownyWorld so that the forceexpl/forcefire/forcepvp tests below do not have trouble.
			// While this is a bit of a dirty hack, the same commit as this one also stops players from unclaiming their homeblock,
			// without moving it first so this should not occur too often.
			world = TownyUniverse.getInstance().getDataSource().getWorlds().get(0);
		}

		// ___[ Raccoon City (PvP) (Open) ]___
		String title = town.getFormattedName();
		title += ((!town.isAdminDisabledPVP()) && ((town.isPVP() || town.getHomeblockWorld().isForcePVP())) ? Translation.of("status_title_pvp") : "");
		title += (town.isOpen() ? Translation.of("status_title_open") : "");
		out.add(ChatTools.formatTitle(title));

		// Lord: Mayor Quimby
		// Board: Get your fried chicken
		try {
			out.add(Translation.of("status_town_board", town.getBoard()));
		} catch (NullPointerException ignored) {
		}
		// Created Date
		long registered= town.getRegistered();
		if (registered != 0) {
			out.add(Translation.of("status_founded", registeredFormat.format(town.getRegistered())));
		}


		// Town Size: 0 / 16 [Bought: 0/48] [Bonus: 0] [Home: 33,44]
		try {
			out.add(Translation.of("status_town_size_part_1", town.getTownBlocks().size(), TownySettings.getMaxTownBlocks(town)) +  
		            (TownySettings.isSellingBonusBlocks(town) ? Translation.of("status_town_size_part_2", town.getPurchasedBlocks(), TownySettings.getMaxPurchedBlocks(town)) : "") + 
		            (town.getBonusBlocks() > 0 ? Translation.of("status_town_size_part_3", town.getBonusBlocks()) : "") + 
		            (TownySettings.getNationBonusBlocks(town) > 0 ? Translation.of("status_town_size_part_4", TownySettings.getNationBonusBlocks(town)) : "") + 
		            (town.isPublic() ? Translation.of("status_town_size_part_5") + 
		            		(TownySettings.getTownDisplaysXYZ() ? (town.hasSpawn() ? BukkitTools.convertCoordtoXYZ(town.getSpawn()) : Translation.of("status_no_town"))  + "]" 
		            				: (town.hasHomeBlock() ? town.getHomeBlock().getCoord().toString() : Translation.of("status_no_town")) + "]") : "")
		           );
		} catch (TownyException ignored) {}

		if (TownySettings.isAllowingOutposts()) {
			if (TownySettings.isOutpostsLimitedByLevels()) {
				if (town.hasOutpostSpawn())
					if (!town.hasNation())
						out.add(Translation.of("status_town_outposts", town.getMaxOutpostSpawn(), town.getOutpostLimit()));
					else {
						int nationBonus = 0;
						try {
							nationBonus =  (Integer) TownySettings.getNationLevel(town.getNation()).get(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT);
						} catch (NotRegisteredException ignored) {}
						out.add(Translation.of("status_town_outposts", town.getMaxOutpostSpawn(), town.getOutpostLimit()) + 
								(nationBonus > 0 ? Translation.of("status_town_outposts2", nationBonus) : "")
							   );
						}
					
				else 
					out.add(Translation.of("status_town_outposts3", town.getOutpostLimit()));
			} else if (town.hasOutpostSpawn()) {
				out.add(Translation.of("status_town_outposts4", town.getMaxOutpostSpawn()));
			}
		}

		// Permissions: B=rao D=--- S=ra-
		out.add(Translation.of("status_perm") + town.getPermissions().getColourString().replace("f", "r"));
		out.add(Translation.of("status_perm") + town.getPermissions().getColourString2().replace("f", "r"));
		out.add(Translation.of("explosions2") + ((town.isBANG() || world.isForceExpl()) ? Translation.of("status_on"): Translation.of("status_off")) + 
				Translation.of("firespread") + ((town.isFire() || world.isForceFire()) ? Translation.of("status_on"): Translation.of("status_off")) + 
				Translation.of("mobspawns") + ((town.hasMobs() || world.isForceTownMobs()) ? Translation.of("status_on"): Translation.of("status_off")));

		// | Bank: 534 coins
		if (TownySettings.isUsingEconomy() && TownyEconomyHandler.isActive()) {
			String bankString = "";
			boolean bankrupt = false;

			try {
				bankrupt = town.getAccount().isBankrupt();
			} catch (EconomyException e) {}

			if(bankrupt) {
				bankString = String.format(TownySettings.getLangString("status_bank_bankrupt"), town.getAccount().getHoldingFormattedBalance());
			} else {
				bankString = String.format(TownySettings.getLangString("status_bank"), town.getAccount().getHoldingFormattedBalance());
				if (town.hasUpkeep())
					bankString += Translation.of("status_bank_town2", BigDecimal.valueOf(TownySettings.getTownUpkeepCost(town)).setScale(2, RoundingMode.HALF_UP).doubleValue());
				if (TownySettings.getUpkeepPenalty() > 0 && town.isOverClaimed())
					bankString += Translation.of("status_bank_town_penalty_upkeep", TownySettings.getTownPenaltyUpkeepCost(town));
				bankString += Translation.of("status_bank_town3", town.getTaxes()) + (town.isTaxPercentage() ? "%" : "");
			}
			out.add(bankString);
		}

		// Mayor: MrSand | Bank: 534 coins
		out.add(Translation.of("rank_list_mayor", town.getMayor().getFormattedName()));

		// Assistants [2]: Sammy, Ginger
		List<String> ranklist = new ArrayList<>();
		getRanks(town, ranklist);

		out.addAll(ranklist);

		// Nation: Azur Empire
		try {
			out.add(Translation.of("status_town_nation", town.getNation().getFormattedName()) + (town.isConquered() ? Translation.of("msg_conquered") : "" ));
		} catch (TownyException ignored) {}

		// Residents [12]: James, Carry, Mason

		String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));
		if (residents.length > 34) {
			String[] entire = residents;
			residents = new String[36];
			System.arraycopy(entire, 0, residents, 0, 35);
			residents[35] = Translation.of("status_town_reslist_overlength");
		}
		out.addAll(ChatTools.listArr(residents, Translation.of("status_town_reslist", town.getNumResidents())));		

		out.addAll(getExtraFields(town));
		
		out = formatStatusScreens(out);
		return out;
	}

	/**
	 * Gets the status screen of a Nation
	 * 
	 * @param nation the nation to check against
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(Nation nation) {

		List<String> out = new ArrayList<>();

		// ___[ Azur Empire (Open)]___
		String title = nation.getFormattedName();
		title += (nation.isOpen() ? Translation.of("status_title_open") : "");
		out.add(ChatTools.formatTitle(title));

		// Created Date
		long registered = nation.getRegistered();
		if (registered != 0) {
			out.add(Translation.of("status_founded", registeredFormat.format(nation.getRegistered())));
		}
		// Bank: 534 coins
		String line = "";
		if (TownySettings.isUsingEconomy())
			if (TownyEconomyHandler.isActive()) {
				line = Translation.of("status_bank", nation.getAccount().getHoldingFormattedBalance());

				if (TownySettings.getNationUpkeepCost(nation) > 0)
					line += Translation.of("status_bank_town2", TownySettings.getNationUpkeepCost(nation));

			}

		if (nation.isNeutral()) {
			if (line.length() > 0)
				line += Colors.Gray + " | ";
			line += Translation.of("status_nation_peaceful");
		}
		
		if (nation.isPublic()) {
			if (line.length() > 0)
				line += Colors.Gray + " | ";
			try {
				line += (nation.isPublic() ? Translation.of("status_town_size_part_5") + (nation.hasSpawn() ? Coord.parseCoord(nation.getSpawn()).toString() : Translation.of("status_no_town")) + "]" : "");
			} catch (TownyException ignored) {
			}
		}		
		// Bank: 534 coins | Peaceful | Public
		
		if (line.length() > 0)
			out.add(line);

		// King: King Harlus
		if (nation.getNumTowns() > 0 && nation.hasCapital() && nation.getCapital().hasMayor())
			out.add(Translation.of("status_nation_king", nation.getCapital().getMayor().getFormattedName()) + 
					Translation.of("status_nation_tax", nation.getTaxes())
				   );
		// Assistants [2]: Sammy, Ginger
		List<String> ranklist = new ArrayList<>();
		List<Town> towns = nation.getTowns();
		List<Resident> residents = new ArrayList<>();
		
		for (Town town: towns) {
			 residents.addAll(town.getResidents());
		}
		
		List<String> nationranks = TownyPerms.getNationRanks();
		List<Resident> residentwithrank = new ArrayList<>();

		for (String rank : nationranks) {
			for (Resident r : residents) {
				if ((r.getNationRanks() != null) && (r.getNationRanks().contains(rank))) {
					residentwithrank.add(r);
				}
			}
			ranklist.addAll(getFormattedResidents(StringMgmt.capitalize(rank), residentwithrank));
			residentwithrank.clear();
		}
		out.addAll(ranklist);
		
		// Towns [44]: James City, Carry Grove, Mason Town
		String[] towns2 = getFormattedNames(nation.getTowns().toArray(new Town[0]));
		if (towns2.length > 10) {
			String[] entire = towns2;
			towns2 = new String[12];
			System.arraycopy(entire, 0, towns2, 0, 11);
			towns2[11] = Translation.of("status_town_reslist_overlength");
		}		
		out.addAll(ChatTools.listArr(towns2, Translation.of("status_nation_towns", nation.getNumTowns())));
		
		// Allies [4]: James Nation, Carry Territory, Mason Country
		String[] allies = getFormattedNames(nation.getAllies().toArray(new Nation[0]));
		if (allies.length > 10) {
			String[] entire = allies;
			allies = new String[12];
			System.arraycopy(entire, 0, allies, 0, 11);
			allies[11] = Translation.of("status_town_reslist_overlength");
		}
		out.addAll(ChatTools.listArr(allies, Translation.of("status_nation_allies", nation.getAllies().size())));
		// Enemies [4]: James Nation, Carry Territory, Mason Country
		String[] enemies = getFormattedNames(nation.getEnemies().toArray(new Nation[0]));
		if (enemies.length > 10) {
			String[] entire = enemies;
			enemies = new String[12];
			System.arraycopy(entire, 0, enemies, 0, 11);
			enemies[11] = Translation.of("status_town_reslist_overlength");
		}
        out.addAll(ChatTools.listArr(enemies, Translation.of("status_nation_enemies", nation.getEnemies().size())));

		out.addAll(getExtraFields(nation));
		
		out = formatStatusScreens(out);
		return out;
	}

	/**
	 * Gets the status screen for a World
	 * 
	 * @param world the world to check
	 * @return a string list containing the results.
	 */
	public static List<String> getStatus(TownyWorld world) {

		List<String> out = new ArrayList<>();

		// ___[ World (PvP) ]___
		String title = world.getFormattedName();
		title += ((world.isPVP() || world.isForcePVP()) ? Translation.of("status_title_pvp") : "");
		title += (world.isClaimable() ? Translation.of("status_world_claimable") : Translation.of("status_world_noclaims"));
		out.add(ChatTools.formatTitle(title));

		if (!world.isUsingTowny()) {
			out.add(Translation.of("msg_set_use_towny_off"));
		} else {
			// ForcePvP: No | Fire: Off
			out.add(Translation.of("status_world_forcepvp") + (world.isForcePVP() ? Translation.of("status_on") : Translation.of("status_off")) + Colors.Gray + " | " + 
					Translation.of("status_world_fire") + (world.isFire() ? Translation.of("status_on") : Translation.of("status_off")) + Colors.Gray + " | " + 
					Translation.of("status_world_forcefire") + (world.isForceFire() ? Translation.of("status_forced") : Translation.of("status_adjustable"))
				   );
			out.add(Translation.of("explosions2") + ": " + (world.isExpl() ? Translation.of("status_on") : Translation.of("status_off")) + Colors.Gray + " | " + 
				    Translation.of("status_world_forceexplosion") + (world.isForceExpl() ? Translation.of("status_forced") : Translation.of("status_adjustable"))
				   );
			out.add(Translation.of("status_world_worldmobs") + (world.hasWorldMobs() ? Translation.of("status_on") : Translation.of("status_off")) + Colors.Gray + " | " + 
				    Translation.of("status_world_forcetownmobs") + (world.isForceTownMobs() ? Translation.of("status_forced") : Translation.of("status_adjustable"))
				   );
			out.add(Colors.Green + (world.isWarAllowed() ? Translation.of("msg_set_war_allowed_on") : Translation.of("msg_set_war_allowed_off")));
			// Using Default Settings: Yes
			// out.add(Colors.Green + "Using Default Settings: " +
			// (world.isUsingDefault() ? Colors.LightGreen + "Yes" : Colors.Rose
			// + "No"));

			out.add(Translation.of("status_world_unclaimrevert") + (world.isUsingPlotManagementRevert() ? Translation.of("status_on_good") : Translation.of("status_off_bad")) + Colors.Gray + " | " + 
			        Translation.of("status_world_explrevert") + (world.isUsingPlotManagementWildRevert() ? Translation.of("status_on_good") : Translation.of("status_off_bad")));
			// Wilderness:
			// Build, Destroy, Switch
			// Ignored Blocks: 34, 45, 64
			out.add(Colors.Green + world.getUnclaimedZoneName() + ":");
			out.add("    " + (world.getUnclaimedZoneBuild() ? Colors.LightGreen : Colors.Rose) + "Build" + Colors.Gray + ", " + (world.getUnclaimedZoneDestroy() ? Colors.LightGreen : Colors.Rose) + "Destroy" + Colors.Gray + ", " + (world.getUnclaimedZoneSwitch() ? Colors.LightGreen : Colors.Rose) + "Switch" + Colors.Gray + ", " + (world.getUnclaimedZoneItemUse() ? Colors.LightGreen : Colors.Rose) + "ItemUse");
			out.add("    " + Translation.of("status_world_ignoredblocks") + Colors.LightGreen + " " + StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), ", "));

			out.addAll(getExtraFields(world));
		}
		
		out = formatStatusScreens(out);
		return out;
	}

	/**
	 * Returns the tax info this resident will have to pay at the next new day.
	 * 
	 * @param resident the resident to check
	 * @return tax status message
	 */
	public static List<String> getTaxStatus(Resident resident) {

		List<String> out = new ArrayList<>();
		Town town;

		double plotTax = 0.0;

		out.add(ChatTools.formatTitle(resident.getFormattedName() + ((BukkitTools.isOnline(resident.getName())) ? Colors.LightGreen + " (Online)" : "")));

		if (resident.hasTown()) {
			try {
				town = resident.getTown();
				out.add(Translation.of("owner_of_x_plots", resident.getTownBlocks().size()));

				if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt")) {
					out.add(Translation.of("status_res_taxexempt"));
				} else {
					if (town.isTaxPercentage()) {
						out.add(Translation.of("status_res_tax", resident.getAccount().getHoldingBalance() * town.getTaxes() / 100));
					} else {
						out.add(Translation.of("status_res_tax", town.getTaxes()));

						if ((resident.getTownBlocks().size() > 0)) {

							for (TownBlock townBlock : new ArrayList<>(resident.getTownBlocks())) {
								plotTax += townBlock.getType().getTax(townBlock.getTown());
							}

							out.add(Translation.of("status_res_plottax") + plotTax);
						}
						out.add(Translation.of("status_res_totaltax") + (town.getTaxes() + plotTax));
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

	/**
	 * @deprecated Since 0.96.0.0 use {@link TownyObject#getFormattedName()} instead.
	 * 
	 * @param obj The {@link TownyObject} to get the formatted name from.
	 * @return The formatted name of the object.
	 */
	@Deprecated
	public static String getFormattedName(TownyObject obj) {
		return obj.getFormattedName();
	}

	/**
	 * @deprecated Since 0.96.0.0 use {@link Resident#getFormattedName()} instead.
	 *
	 * @param resident The {@link Resident} to get the formatted name from.
	 * @return The formatted name of the object.
	 */
	@Deprecated
	public static String getFormattedResidentName(Resident resident) {
		return resident.getFormattedName();
	}

	/**
	 * @deprecated Since 0.96.0.0 use {@link Town#getFormattedName()} instead.
	 *
	 * @param town The {@link Town} to get the formatted name from.
	 * @return The formatted name of the object.
	 */
	@Deprecated
	public static String getFormattedTownName(Town town) {
		return town.getFormattedName();
	}

	/**
	 * @deprecated Since 0.96.0.0 use {@link Nation#getFormattedName()} instead.
	 *
	 * @param nation The {@link Nation} to get the formatted name from.
	 * @return The formatted name of the object.
	 */
	@Deprecated
	public static String getFormattedNationName(Nation nation) {
		return nation.getFormattedName();
	}

	/**
	 * @deprecated Since 0.96.0.0 use {@link Resident#getFormattedTitleName()} instead.
	 *
	 * @param resident The {@link Resident} to get the formatted title name from.
	 * @return The formatted title name of the resident.
	 */
	@Deprecated
	public static String getFormattedResidentTitleName(Resident resident) {
		return resident.getFormattedTitleName();
	}
	
	public static String[] getFormattedNames(TownyObject[] objs) {
		List<String> names = new ArrayList<>();
		for (TownyObject obj : objs) {
			names.add(obj.getFormattedName() + Colors.White);
		}
		
		return names.toArray(new String[0]);
	}
	
	public static List<String> getExtraFields(TownyObject to) {
		if (!to.hasMeta())
			return new ArrayList<>();
		
		List<String> extraFields = new ArrayList<>();
		
		String field = "";
		
		for (CustomDataField cdf : to.getMetadata()) {
			if (!cdf.hasLabel())
				continue;
			
			if (extraFields.contains(field))
				field = Colors.Green + cdf.getLabel() + ": ";
			else
				field += Colors.Green + cdf.getLabel() + ": ";
			
			switch (cdf.getType()) {
				case IntegerField:
					int ival = (int) cdf.getValue();
					field += (ival <= 0 ? Colors.Red : Colors.LightGreen) + ival;
					break;
				case StringField:
					field += Colors.White + cdf.getValue();
					break;
				case BooleanField:
					boolean bval = (boolean) cdf.getValue();
					field += (bval ? Colors.LightGreen : Colors.Red) + bval;
					break;
				case DecimalField:
					double dval = (double) cdf.getValue();
					field += (dval <= 0 ? Colors.Red : Colors.LightGreen) + dval;
					break;
			}
			
			field += "  ";
			
			if (field.length() > 40)
				extraFields.add(field);
		}
		
		extraFields.add(field);
		
		return extraFields;
	}
	
	/**
	 * @deprecated Since 0.96.0.0 use {@link Resident#getNamePrefix()} instead.
	 *
	 * @param resident The {@link Resident} to get the king or mayor prefix from.
	 * @return The king or mayor prefix of the resident.
	 */
	@Deprecated	
	public static String getNamePrefix(Resident resident) {
		return resident.getNamePrefix();	
	}	

	/**
	 * @deprecated Since 0.96.0.0 use {@link Resident#getNamePostfix()} instead.
	 *
	 * @param resident The {@link Resident} to get the king or mayor postfix from.
	 * @return The king or mayor postfix of the resident.
	 */
	@Deprecated	
	public static String getNamePostfix(Resident resident) {
		return resident.getNamePostfix();
	}	

}
