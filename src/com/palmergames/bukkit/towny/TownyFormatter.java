package com.palmergames.bukkit.towny; /* Localized on 2014-05-04 by Neder */

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

	public static final SimpleDateFormat lastOnlineFormat = new SimpleDateFormat("MM월 dd일 '@' HH:mm"); // May 02 @ 21:02 => 05월 02일 @ 21:02
	public static final SimpleDateFormat registeredFormat = new SimpleDateFormat("yyyy년 MM월 dd일"); // 2014 May 02 => 2014년 05월 02일

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

		out.addAll(ChatTools.listArr(residents, Colors.Green + "주민 수 " + Colors.LightGreen + "[" + town.getNumResidents() + "]" + Colors.Green + ":" + Colors.White + " "));

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

			out.add(ChatTools.formatTitle(TownyFormatter.getFormattedName(owner) + ((BukkitTools.isOnline(owner.getName())) ? Colors.LightGreen + " (온라인)" : "")));
			out.add(Colors.Green + " 권한: " + ((owner instanceof Resident) ? townBlock.getPermissions().getColourString() : townBlock.getPermissions().getColourString().replace("f", "r")));
			out.add(Colors.Green + "PvP: " + ((town.isPVP() || world.isForcePVP() || townBlock.getPermissions().pvp) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  폭발: " + ((world.isForceExpl() || townBlock.getPermissions().explosion) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  불번짐: " + ((town.isFire() || world.isForceFire() || townBlock.getPermissions().fire) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  몹 스폰: " + ((town.hasMobs() || world.isForceTownMobs() || townBlock.getPermissions().mobs) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐"));

		} catch (NotRegisteredException e) {
			out.add("오류: " + e.getMessage());
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
		out.add(ChatTools.formatTitle(getFormattedName(resident) + ((BukkitTools.isOnline(resident.getName()) && (player != null) && (player.canSee(BukkitTools.getPlayer(resident.getName())))) ? Colors.LightGreen + " (온라인)" : "")));

		// Registered: Sept 3 2009 | Last Online: March 7 @ 14:30
		out.add(Colors.Green + "가입일: " + Colors.LightGreen + registeredFormat.format(resident.getRegistered()) + Colors.Gray + " | " + Colors.Green + "최근 접속: " + Colors.LightGreen + lastOnlineFormat.format(resident.getLastOnline()));

		// Owner of: 4 plots
		// Perm: Build = f-- Destroy = fa- Switch = fao Item = ---
		// if (resident.getTownBlocks().size() > 0) {
		out.add(Colors.Green + "소유함: " + Colors.LightGreen + resident.getTownBlocks().size() + " 개의 토지");
		out.add(Colors.Green + "    권한: " + resident.getPermissions().getColourString());
		out.add(Colors.Green + "PVP: " + ((resident.getPermissions().pvp) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  폭발: " + ((resident.getPermissions().explosion) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  불번짐: " + ((resident.getPermissions().fire) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  몹 스폰: " + ((resident.getPermissions().mobs) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐"));
		// }

		// Bank: 534 coins
		if (TownySettings.isUsingEconomy())
			if (TownyEconomyHandler.isActive())
				out.add(Colors.Green + "소지금: " + Colors.LightGreen + resident.getHoldingFormattedBalance());

		// Town: Camelot
		String line = Colors.Green + "마을: " + Colors.LightGreen;
		if (!resident.hasTown())
			line += "없음";
		else
			try {
				line += getFormattedName(resident.getTown());
			} catch (TownyException e) {
				line += "오류: " + e.getMessage();
			}
		out.add(line);
		
		// Town ranks
		if (resident.hasTown()) {
			if (!resident.getTownRanks().isEmpty())
				out.add(Colors.Green + "마을 등급: " + Colors.LightGreen + StringMgmt.join(resident.getTownRanks(), ","));
		}
		
		//Nation ranks
		if (resident.hasNation()) {
			if (!resident.getNationRanks().isEmpty())
				out.add(Colors.Green + "국가 등급: " + Colors.LightGreen + StringMgmt.join(resident.getNationRanks(), ","));
		}
		
		// Jailed: yes if they are jailed.
		if (resident.isJailed()){
			out.add(Colors.Green + "Jailed: Yes" + " in Town: " + resident.getJailTown());
		}
		
		// Friends [12]: James, Carry, Mason
		List<Resident> friends = resident.getFriends();
		out.addAll(getFormattedResidents("친구", friends));

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
		towntitle += Colors.Blue + " 등급 목록";
		ranklist.add(ChatTools.formatTitle(towntitle));
		ranklist.add(Colors.Green + "촌장: " + Colors.LightGreen + getFormattedName(town.getMayor()));

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
		title += (town.isOpen() ? Colors.LightBlue + " (개방)" : "");
		out.add(ChatTools.formatTitle(title));

		// Lord: Mayor Quimby
		// Board: Get your fried chicken
		try {
			out.add(Colors.Green + "공지: " + Colors.LightGreen + town.getTownBoard());
		} catch (NullPointerException e) {
		}

		// Town Size: 0 / 16 [Bought: 0/48] [Bonus: 0] [Home: 33,44]
		try {
			out.add(Colors.Green + "마을 크기: " + Colors.LightGreen + town.getTownBlocks().size() + " / " + TownySettings.getMaxTownBlocks(town) + (TownySettings.isSellingBonusBlocks() ? Colors.LightBlue + " [구매함: " + town.getPurchasedBlocks() + "/" + TownySettings.getMaxPurchedBlocks() + "]" : "") + (town.getBonusBlocks() > 0 ? Colors.LightBlue + " [보너스: " + town.getBonusBlocks() + "]" : "") + ((TownySettings.getNationBonusBlocks(town) > 0) ? Colors.LightBlue + " [국가보너스: " + TownySettings.getNationBonusBlocks(town) + "]" : "") + (town.isPublic() ? Colors.LightGray + " [홈블록: " + (town.hasHomeBlock() ? town.getHomeBlock().getCoord().toString() : "없음") + "]" : ""));
		} catch (TownyException e) {
		}

		if (town.hasOutpostSpawn())
			out.add(Colors.Green + "전초기지: " + Colors.LightGreen + town.getMaxOutpostSpawn());

		// Permissions: B=rao D=--- S=ra-
		out.add(Colors.Green + "권한: " + town.getPermissions().getColourString().replace("f", "r"));
		out.add(Colors.Green + "폭발: " + ((town.isBANG() || world.isForceExpl()) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  불번짐: " + ((town.isFire() || world.isForceFire()) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Green + "  몹 스폰: " + ((town.hasMobs() || world.isForceTownMobs()) ? Colors.Red + "켜짐" : Colors.LightGreen + "꺼짐"));

		// | Bank: 534 coins
		String bankString = "";
		if (TownySettings.isUsingEconomy()) {
			if (TownyEconomyHandler.isActive()) {
				bankString = Colors.Green + "금고 잔액: " + Colors.LightGreen + town.getHoldingFormattedBalance();
				if (town.hasUpkeep())
					bankString += Colors.Gray + " | " + Colors.Green + "유지비: " + Colors.Red + TownySettings.getTownUpkeepCost(town);
				bankString += Colors.Gray + " | " + Colors.Green + "세금: " + Colors.Red + town.getTaxes() + (town.isTaxPercentage() ? "%" : "");
			}
			out.add(bankString);
		}

		// Mayor: MrSand | Bank: 534 coins
		out.add(Colors.Green + "촌장: " + Colors.LightGreen + getFormattedName(town.getMayor()));

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
			out.add(Colors.Green + "소속된 국가: " + Colors.LightGreen + getFormattedName(town.getNation()));
		} catch (TownyException e) {
		}

		// Residents [12]: James, Carry, Mason

		String[] residents = getFormattedNames(town.getResidents().toArray(new Resident[0]));
		if (residents.length > 34) {
			String[] entire = residents;
			residents = new String[36];
			System.arraycopy(entire, 0, residents, 0, 35);
			residents[35] = "등등...";
		}
		out.addAll(ChatTools.listArr(residents, Colors.Green + "주민 " + Colors.LightGreen + "[" + town.getNumResidents() + "]" + Colors.Green + ":" + Colors.White + " "));
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
				line = Colors.Green + "금고 잔액: " + Colors.LightGreen + nation.getHoldingFormattedBalance();

				if (TownySettings.getNationUpkeepCost(nation) > 0)
					line += (Colors.Gray + " | " + Colors.Green + "유지비: " + Colors.Red + TownySettings.getNationUpkeepCost(nation));

			}

		if (nation.isNeutral()) {
			if (line.length() > 0)
				line += Colors.Gray + " | ";
			line += Colors.LightGray + "중립";
		}
		// Bank: 534 coins | Neutral
		if (line.length() > 0)
			out.add(line);

		// King: King Harlus
		if (nation.getNumTowns() > 0 && nation.hasCapital() && nation.getCapital().hasMayor())
			out.add(Colors.Green + "왕: " + Colors.LightGreen + getFormattedName(nation.getCapital().getMayor()) + Colors.Green + "  국가세금: " + Colors.Red + nation.getTaxes());
		// Assistants: Mayor Rockefel, Sammy, Ginger
		if (nation.getAssistants().size() > 0)
			out.addAll(ChatTools.listArr(getFormattedNames(nation.getAssistants().toArray(new Resident[0])), Colors.Green + "신하:" + Colors.White + " "));
		// Towns [44]: James City, Carry Grove, Mason Town
		out.addAll(ChatTools.listArr(getFormattedNames(nation.getTowns().toArray(new Town[0])), Colors.Green + "소속된 마을 " + Colors.LightGreen + "[" + nation.getNumTowns() + "]" + Colors.Green + ":" + Colors.White + " "));
		// Allies [4]: James Nation, Carry Territory, Mason Country
		out.addAll(ChatTools.listArr(getFormattedNames(nation.getAllies().toArray(new Nation[0])), Colors.Green + "동맹국 " + Colors.LightGreen + "[" + nation.getAllies().size() + "]" + Colors.Green + ":" + Colors.White + " "));
		// Enemies [4]: James Nation, Carry Territory, Mason Country
		out.addAll(ChatTools.listArr(getFormattedNames(nation.getEnemies().toArray(new Nation[0])), Colors.Green + "적국 " + Colors.LightGreen + "[" + nation.getEnemies().size() + "]" + Colors.Green + ":" + Colors.White + " "));

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
		title += (world.isClaimable() ? Colors.LightGreen + " 점유가능" : Colors.Rose + " 점유불가");
		out.add(ChatTools.formatTitle(title));

		if (!world.isUsingTowny()) {
			out.add(TownySettings.getLangString("msg_set_use_towny_off"));
		} else {
			// ForcePvP: No | Fire: Off
			out.add(Colors.Green + "강제적 마을 PVP: " + (world.isForcePVP() ? Colors.Rose + "활성" : Colors.LightGreen + "비활성") + Colors.Gray + " | " + Colors.Green + "불번짐: " + (world.isFire() ? Colors.Rose + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Gray + " | " + Colors.Green + "강제적 불번짐: " + (world.isForceFire() ? Colors.Rose + "활성" : Colors.LightGreen + "비활성"));

			out.add(Colors.Green + "폭발: " + (world.isExpl() ? Colors.Rose + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Gray + " | " + Colors.Green + " 강제적 폭발 켜짐: " + (world.isForceExpl() ? Colors.Rose + "활성" : Colors.LightGreen + "비활성"));
			out.add(Colors.Green + "월드 몹: " + (world.hasWorldMobs() ? Colors.Rose + "켜짐" : Colors.LightGreen + "꺼짐") + Colors.Gray + " | " + Colors.Green + "강제적 마을 몹 스폰: " + (world.isForceTownMobs() ? Colors.Rose + "활성" : Colors.LightGreen + "비활성"));
			// Using Default Settings: Yes
			// out.add(Colors.Green + "Using Default Settings: " +
			// (world.isUsingDefault() ? Colors.LightGreen + "Yes" : Colors.Rose
			// + "No"));

			out.add(Colors.Green + "점유해제시 지형 복구: " + (world.isUsingPlotManagementRevert() ? Colors.LightGreen + "켜짐" : Colors.Rose + "꺼짐") + Colors.Gray + " | " + Colors.Green + "폭발 되돌리기: " + (world.isUsingPlotManagementWildRevert() ? Colors.LightGreen + "켜짐" : Colors.Rose + "꺼짐"));
			// Wilderness:
			// Build, Destroy, Switch
			// Ignored Blocks: 34, 45, 64
			out.add(Colors.Green + world.getUnclaimedZoneName() + ":");
			out.add("    " + (world.getUnclaimedZoneBuild() ? Colors.LightGreen : Colors.Rose) + "건축" + Colors.Gray + ", " + (world.getUnclaimedZoneDestroy() ? Colors.LightGreen : Colors.Rose) + "파괴" + Colors.Gray + ", " + (world.getUnclaimedZoneSwitch() ? Colors.LightGreen : Colors.Rose) + "스위치" + Colors.Gray + ", " + (world.getUnclaimedZoneItemUse() ? Colors.LightGreen : Colors.Rose) + "아이템사용");
			out.add("    " + Colors.Green + "예외 블록:" + Colors.LightGreen + " " + StringMgmt.join(world.getUnclaimedZoneIgnoreMaterials(), ", "));
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
				out.add(Colors.Green + "소유함: " + Colors.LightGreen + resident.getTownBlocks().size() + " 토지");

				if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt")) {
					out.add(Colors.Green + "마을의 스탭들은 세금이 면제됩니다.");
				} else {
					if (town.isTaxPercentage()) {
						out.add(Colors.Green + "마을 세금: " + Colors.LightGreen + (resident.getHoldingBalance() * town.getTaxes() / 100));
					} else {
						out.add(Colors.Green + "마을 세금: " + Colors.LightGreen + town.getTaxes());

						if ((resident.getTownBlocks().size() > 0)) {

							for (TownBlock townBlock : new ArrayList<TownBlock>(resident.getTownBlocks())) {
								plotTax += townBlock.getType().getTax(townBlock.getTown());
							}

							out.add(Colors.Green + "총 토지 세금: " + Colors.LightGreen + plotTax);
						}
						out.add(Colors.Green + "내야할 세금: " + Colors.LightGreen + (town.getTaxes() + plotTax));
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
