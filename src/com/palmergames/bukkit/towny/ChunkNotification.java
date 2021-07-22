package com.palmergames.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

public class ChunkNotification {

	// Example:
	// ~ Wak Town - Lord Jebus - [Home] [For Sale: 50 Beli] [Shop]

	public static String notificationFormat = Colors.Gold + " ~ %s";
	public static String notificationSplitter = Colors.LightGray + " - ";
	public static String areaWildernessNotificationFormat = Colors.Green + "%s";
	public static String areaWildernessPvPNotificationFormat = Colors.Green + "%s";
	public static String areaTownNotificationFormat = Colors.Green + "%s";
	public static String areaTownPvPNotificationFormat = Colors.Green + "%s";
	public static String ownerNotificationFormat = Colors.LightGreen + "%s";
	public static String noOwnerNotificationFormat = Colors.LightGreen + "%s";
	public static String plotNotificationSplitter = " ";
	public static String plotNotificationFormat = "%s";
	public static String homeBlockNotification = Colors.LightBlue + "[Home]";
	public static String outpostBlockNotification = Colors.LightBlue + "[Outpost]";
	public static String forSaleNotificationFormat = Colors.Yellow + "[For Sale: %s]";
	public static String notForSaleNotificationFormat = Colors.Yellow + "[Not For Sale]";
	public static String plotTypeNotificationFormat = Colors.Gold + "[%s]";	
	public static String groupNotificationFormat = Colors.White + "[%s]";

	/**
	 * Called on Config load.
	 * Specifically: TownySettings.loadConfig()
	 */
	public static void loadFormatStrings() {

		notificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_FORMAT));
		notificationSplitter = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_SPLITTER));
		areaWildernessNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_AREA_WILDERNESS));
		areaWildernessPvPNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_AREA_WILDERNESS_PVP));
		areaTownNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_AREA_TOWN));
		areaTownPvPNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_AREA_TOWN_PVP));
		ownerNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_OWNER));
		noOwnerNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_NO_OWNER));
		plotNotificationSplitter = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_SPLITTER));
		plotNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_FORMAT));
		homeBlockNotification = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_HOMEBLOCK));
		outpostBlockNotification = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_OUTPOSTBLOCK));
		forSaleNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_FORSALE));
		plotTypeNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_TYPE));
		groupNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_GROUP));
	}

	WorldCoord from, to;
	boolean fromWild = false, toWild = false, toForSale = false, fromForSale = false,
			toHomeBlock = false, toOutpostBlock = false, toPlotGroupBlock = false;
	TownBlock fromTownBlock, toTownBlock = null;
	Town fromTown = null, toTown = null;
	Resident fromResident = null, toResident = null;
	TownBlockType fromPlotType = null, toPlotType = null;
	PlotGroup fromPlotGroup = null, toPlotGroup = null;

	public ChunkNotification(WorldCoord from, WorldCoord to) {

		this.from = from;
		this.to = to;

		if (from.hasTownBlock()) {
			fromTownBlock = from.getTownBlockOrNull();
			fromPlotType = fromTownBlock.getType();
			fromForSale = fromTownBlock.getPlotPrice() != -1;
			if (fromTownBlock.hasPlotObjectGroup()) {
				fromPlotGroup = fromTownBlock.getPlotObjectGroup();
				fromForSale = fromPlotGroup.getPrice() != -1;
			}
			fromTown = fromTownBlock.getTownOrNull();
			fromResident = fromTownBlock.getResidentOrNull();
			
			
		} else {
			fromWild = true;
		}

		if (to.hasTownBlock()) {
			toTownBlock = to.getTownBlockOrNull();
			toPlotType = toTownBlock.getType();
			toTown = toTownBlock.getTownOrNull();
			toResident = toTownBlock.getResidentOrNull();
			toForSale = toTownBlock.getPlotPrice() != -1;
			toHomeBlock = toTownBlock.isHomeBlock();
			toOutpostBlock = toTownBlock.isOutpost();
			toPlotGroupBlock = toTownBlock.hasPlotObjectGroup();
			if (toPlotGroupBlock) {
				toPlotGroup = toTownBlock.getPlotObjectGroup();
				toForSale = toPlotGroup.getPrice() != -1;
			}
			
		} else {
			toWild = true;
		}

	}

	public String getNotificationString(Resident resident) {

		if (notificationFormat.length() == 0)
			return null;
		List<String> outputContent = getNotificationContent(resident);
		if (outputContent.size() == 0)
			return null;
		return String.format(notificationFormat, StringMgmt.join(outputContent, notificationSplitter));
	}

	public List<String> getNotificationContent(Resident resident) {

		List<String> out = new ArrayList<String>();
		String output;

		output = getAreaNotification(resident);
		if (output != null && output.length() > 0)
			out.add(output);
		
		// Only adds this if entering the wilderness
		output = getAreaPvPNotification();
		if (output != null && output.length() > 0)
			out.add(output);
		
		// Only show the owner of individual plots if they do not have this mode applied		
		if (!resident.hasMode("ignoreplots")) {
			output = getOwnerOrPlotNameNotification();
			if (output != null && output.length() > 0)
				out.add(output);
		}
	
		// Only adds this IF in town.
		output = getTownPVPNotification();
		if (output != null && output.length() > 0)
			out.add(output);

		// Only show the names of plots if they do not have this mode applied
		if (!resident.hasMode("ignoreplots")) {
			output = getPlotNotification();
			if (output != null && output.length() > 0)
				out.add(output);
		}

		return out;
	}

	public String getAreaNotification(Resident resident) {

		if (fromWild ^ toWild || !fromWild && !toWild && fromTown != null && toTown != null && fromTown != toTown) {
			if (toWild) {
				if (TownySettings.getNationZonesEnabled() && TownySettings.getNationZonesShowNotifications()) {
					Player player = BukkitTools.getPlayer(resident.getName());
					TownyWorld toWorld = to.getTownyWorldOrNull();
					if (PlayerCacheUtil.getTownBlockStatus(player, this.to).equals(TownBlockStatus.NATION_ZONE)) {
						Town nearestTown = null; 
						nearestTown = toWorld.getClosestTownWithNationFromCoord(this.to.getCoord(), nearestTown);
						return String.format(areaWildernessNotificationFormat, Translation.of("nation_zone_this_area_under_protection_of", toWorld.getUnclaimedZoneName(), nearestTown.getNationOrNull().getName()));
					}
				}
				
				return String.format(areaWildernessNotificationFormat, to.getTownyWorldOrNull().getUnclaimedZoneName());
			
			} else if (TownySettings.isNotificationsTownNamesVerbose())
				return String.format(areaTownNotificationFormat, toTown.getFormattedName());
			else 
				return String.format(areaTownNotificationFormat, toTown);
			
		} else if (fromWild && toWild)
			if (TownySettings.getNationZonesEnabled() && TownySettings.getNationZonesShowNotifications()) {
				Player player = BukkitTools.getPlayer(resident.getName());
				TownyWorld toWorld = this.to.getTownyWorldOrNull();
				if (PlayerCacheUtil.getTownBlockStatus(player, this.to).equals(TownBlockStatus.NATION_ZONE) && PlayerCacheUtil.getTownBlockStatus(player, this.from).equals(TownBlockStatus.UNCLAIMED_ZONE)) {
					Town nearestTown = null; 
					nearestTown = toWorld.getClosestTownWithNationFromCoord(this.to.getCoord(), nearestTown);
					return String.format(areaWildernessNotificationFormat, Translation.of("nation_zone_this_area_under_protection_of", toWorld.getUnclaimedZoneName(), nearestTown.getNationOrNull().getName()));
				} else if (PlayerCacheUtil.getTownBlockStatus(player, this.to).equals(TownBlockStatus.UNCLAIMED_ZONE) && PlayerCacheUtil.getTownBlockStatus(player, this.from).equals(TownBlockStatus.NATION_ZONE)) {
					return String.format(areaWildernessNotificationFormat, to.getTownyWorldOrNull().getUnclaimedZoneName());
				}
			}
		return null;
	}
	
	public String getAreaPvPNotification() {

		if (fromWild ^ toWild || !fromWild && !toWild && fromTown != null && toTown != null && fromTown != toTown) {
			if (toWild)
				return String.format(areaWildernessPvPNotificationFormat, ((to.getTownyWorldOrNull().isPVP() && testWorldPVP()) ? Colors.Red + " (PvP)" : ""));
		}
		return null;
	}

	public String getOwnerOrPlotNameNotification() {

		if (toWild) return null;
		
		if (fromResident != toResident  // Not owned by the same resident.
		|| (fromTownBlock != null && !fromTownBlock.getName().equalsIgnoreCase(toTownBlock.getName())) // Townblock not named the same.
		|| (fromTownBlock != null && fromTownBlock.hasPlotObjectGroup() && !toTownBlock.hasPlotObjectGroup())) // Left a plot group and entered to a regular plot. 
		{
			if (toResident != null) {
				String resName = (TownySettings.isNotificationOwnerShowingNationTitles() ? toResident.getFormattedTitleName() : toResident.getFormattedName());
				return String.format(ownerNotificationFormat, (toTownBlock.getName().isEmpty()) ? resName : toTownBlock.getName());
			} else
				return  String.format(noOwnerNotificationFormat, (toTownBlock.getName().isEmpty()) ? Translation.of("UNCLAIMED_PLOT_NAME") : toTownBlock.getName());

		}
		return null;
	}

	public String getTownPVPNotification() {

		if (!toWild && ((fromWild) || (toTownBlock.getPermissions().pvp != fromTownBlock.getPermissions().pvp))) {
			return String.format(areaTownPvPNotificationFormat, ( !CombatUtil.preventPvP(to.getTownyWorldOrNull(), toTownBlock) ? Colors.Red + "(PvP)" : Colors.Green + "(No PVP)"));
		}
		return null;
	}

	private boolean testWorldPVP() {

		return to.getTownyWorldOrNull().isPVP();
	}

	public String getPlotNotification() {

		if (plotNotificationFormat.length() == 0)
			return null;
		List<String> outputContent = getPlotNotificationContent();
		if (outputContent.size() == 0)
			return null;
		return String.format(plotNotificationFormat, StringMgmt.join(outputContent, plotNotificationSplitter));
	}

	public List<String> getPlotNotificationContent() {

		List<String> out = new ArrayList<String>();
		String output;

		output = getHomeblockNotification();
		if (output != null && output.length() > 0)
			out.add(output);

		output = getOutpostblockNotification();
		if (output != null && output.length() > 0)
			out.add(output);

		output = getForSaleNotification();
		if (output != null && output.length() > 0)
			out.add(output);

		output = getPlotTypeNotification();
		if (output != null && output.length() > 0)
			out.add(output);
		
		output = getGroupNotification();
		if (output != null && output.length() > 0)
			out.add(output);

		return out;
	}

	public String getHomeblockNotification() {

		if (toHomeBlock)
			return homeBlockNotification;
		return null;
	}

	public String getOutpostblockNotification() {

		if (toOutpostBlock)
			return outpostBlockNotification;
		return null;
	}

	public String getForSaleNotification() {

		// Were heading to a plot group do some things differently
		if (toForSale && toPlotGroupBlock && (fromPlotGroup != toPlotGroup))
			return String.format(forSaleNotificationFormat, TownyEconomyHandler.isActive() ? TownyEconomyHandler.getFormattedBalance(toTownBlock.getPlotObjectGroup().getPrice()) : "$ 0");
		
		if (toForSale && !toPlotGroupBlock)
			return String.format(forSaleNotificationFormat, TownyEconomyHandler.isActive() ? TownyEconomyHandler.getFormattedBalance(toTownBlock.getPlotPrice()): "$ 0");
		
		if (!toForSale && fromForSale && !toWild)
			return notForSaleNotificationFormat;
		
		return null;
	}
	
	public String getGroupNotification() {
		if (toPlotGroupBlock && (fromPlotGroup != toPlotGroup))
			return String.format(groupNotificationFormat, toTownBlock.getPlotObjectGroup().getName());
		return null;
	}

	public String getPlotTypeNotification() {

		if (fromPlotType != toPlotType && toPlotType != null && toPlotType != TownBlockType.RESIDENTIAL)
			return String.format(plotTypeNotificationFormat, toPlotType.toString());
		return null;
	}
}
