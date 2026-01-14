package com.palmergames.bukkit.towny;

import java.util.ArrayList;
import java.util.List;

import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.PlayerCacheUtil;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;

public class ChunkNotification {

	// Example:
	// ~ Wak Town - Lord Jebus - [Home] [For Sale: 50 Beli] [Shop]

	public static String notificationFormat = Colors.GOLD + " ~ %s";
	public static String notificationSplitter = Colors.GRAY + " - ";
	public static String areaWildernessNotificationFormat = Colors.DARK_GREEN + "%s";
	public static String areaWildernessPvPNotificationFormat = Colors.DARK_GREEN + "%s";
	public static String areaTownNotificationFormat = Colors.DARK_GREEN + "%s";
	public static String areaTownPvPNotificationFormat = Colors.DARK_GREEN + "%s";
	public static String ownerNotificationFormat = Colors.GREEN + "%s";
	public static String noOwnerNotificationFormat = Colors.GREEN + "%s";
	public static String plotNotificationSplitter = " ";
	public static String plotNotificationFormat = "%s";
	public static String homeBlockNotification = Colors.BLUE + "[Home]";
	public static String outpostBlockNotification = Colors.BLUE + "[Outpost]";
	public static String forSaleNotificationFormat = Colors.YELLOW + "[For Sale by %s: %s]";
	public static String notForSaleNotificationFormat = Colors.YELLOW + "[Not For Sale]";
	public static String plotTypeNotificationFormat = Colors.GOLD + "[%s]";	
	public static String groupNotificationFormat = Colors.WHITE + "[%s]";
	public static String districtNotificationFormat = Colors.DARK_GREEN + "[%s]";

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
		forSaleNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_FORSALEBY));
		notForSaleNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_NOTFORSALE));
		plotTypeNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_PLOT_TYPE));
		groupNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_GROUP));
		districtNotificationFormat = Colors.translateColorCodes(TownySettings.getString(ConfigNodes.NOTIFICATION_DISTRICT));
	}

	WorldCoord from, to;
	boolean fromWild = false, toWild = false, toForSale = false, fromForSale = false,
			toHomeBlock = false, toOutpostBlock = false, toPlotGroupBlock = false,  toDistrictBlock = false;
	TownBlock fromTownBlock, toTownBlock = null;
	Town fromTown = null, toTown = null;
	Resident fromResident = null, toResident = null;
	TownBlockType fromPlotType = null, toPlotType = null;
	PlotGroup fromPlotGroup = null, toPlotGroup = null;
	District fromDistrict = null, toDistrict = null;
	Resident viewerResident = null;

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
			if (fromTownBlock.hasDistrict()) {
				fromDistrict = fromTownBlock.getDistrict();
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
			toDistrictBlock = toTownBlock.hasDistrict();
			if (toDistrictBlock) {
				toDistrict = toTownBlock.getDistrict();
			}
		} else {
			toWild = true;
		}

	}

	public String getNotificationString(Resident resident) {

		if (notificationFormat.length() == 0)
			return null;
		viewerResident = resident;
		List<String> outputContent = getNotificationContent(resident);
		if (outputContent.size() == 0)
			return null;
		return String.format(notificationFormat, StringMgmt.join(outputContent, notificationSplitter));
	}

	public List<String> getNotificationContent(Resident resident) {

		List<String> out = new ArrayList<String>();
		String output;

		// Show nothing if the world doesn't use Towny.
		if (!to.getTownyWorld().isUsingTowny())
			return out;

		output = getAreaNotification(resident);
		if (output != null && output.length() > 0)
			out.add(output);
		
		// Only adds this if entering the wilderness
		output = getAreaPvPNotification(resident);
		if (output != null && output.length() > 0)
			out.add(output);
		
		// Only show the owner of individual plots if they do not have this mode applied		
		if (!resident.hasMode("ignoreplots")) {
			output = getOwnerOrPlotNameNotification(resident);
			if (output != null && output.length() > 0)
				out.add(output);
		}
	
		// Only adds this IF in town.
		output = getTownPVPNotification(resident);
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
					Player player = resident.getPlayer();
					TownyWorld toWorld = to.getTownyWorld();
					if (PlayerCacheUtil.fetchTownBlockStatus(player, this.to).equals(TownBlockStatus.NATION_ZONE)) {
						Town nearestTown = null; 
						nearestTown = toWorld.getClosestTownWithNationFromCoord(this.to.getCoord(), nearestTown);
						return String.format(areaWildernessNotificationFormat, Translatable.of("nation_zone_this_area_under_protection_of", toWorld.getFormattedUnclaimedZoneName(), nearestTown.getNationOrNull().getName()).forLocale(resident));
					}
				}
				
				return String.format(areaWildernessNotificationFormat, to.getTownyWorld().getFormattedUnclaimedZoneName());
			
			} else if (TownySettings.isNotificationsTownNamesVerbose())
				return String.format(areaTownNotificationFormat, toTown.getFormattedName());
			else 
				return String.format(areaTownNotificationFormat, toTown);
			
		} else if (fromWild && toWild)
			if (TownySettings.getNationZonesEnabled() && TownySettings.getNationZonesShowNotifications()) {
				Player player = resident.getPlayer();
				TownyWorld toWorld = this.to.getTownyWorld();
				if (PlayerCacheUtil.fetchTownBlockStatus(player, this.to).equals(TownBlockStatus.NATION_ZONE) && PlayerCacheUtil.fetchTownBlockStatus(player, this.from).equals(TownBlockStatus.UNCLAIMED_ZONE)) {
					Town nearestTown = null; 
					nearestTown = toWorld.getClosestTownWithNationFromCoord(this.to.getCoord(), nearestTown);
					return String.format(areaWildernessNotificationFormat, Translatable.of("nation_zone_this_area_under_protection_of", toWorld.getFormattedUnclaimedZoneName(), nearestTown.getNationOrNull().getName()).forLocale(resident));
				} else if (PlayerCacheUtil.fetchTownBlockStatus(player, this.to).equals(TownBlockStatus.UNCLAIMED_ZONE) && PlayerCacheUtil.fetchTownBlockStatus(player, this.from).equals(TownBlockStatus.NATION_ZONE)) {
					return String.format(areaWildernessNotificationFormat, to.getTownyWorld().getFormattedUnclaimedZoneName());
				}
			}
		return null;
	}
	
	public String getAreaPvPNotification(Resident resident) {

		if (fromWild ^ toWild || !fromWild && !toWild && fromTown != null && toTown != null && fromTown != toTown) {
			if (toWild)
				return String.format(areaWildernessPvPNotificationFormat, (to.getTownyWorld().isPVP() && testWorldPVP()) ? Translatable.of("status_title_pvp").forLocale(resident) : "");
		}
		return null;
	}

	public String getOwnerOrPlotNameNotification(Resident resident) {

		if (toWild) return null;
		
		if (fromResident != toResident  // Not owned by the same resident.
		|| (fromTownBlock != null && !fromTownBlock.getName().equalsIgnoreCase(toTownBlock.getName())) // Townblock not named the same.
		|| (fromTownBlock != null && fromTownBlock.hasPlotObjectGroup() && !toTownBlock.hasPlotObjectGroup())) // Left a plot group and entered to a regular plot. 
		{
			if (toResident != null) {
				String resName = (TownySettings.isNotificationOwnerShowingVerboseName() ? toResident.getFormattedName() : toResident.getName());
				return String.format(ownerNotificationFormat, Colors.translateColorCodes(toTownBlock.getName().isEmpty() ? resName : StringMgmt.remUnderscore(toTownBlock.getName())));
			} else
				return String.format(noOwnerNotificationFormat, (toTownBlock.getName().isEmpty()) ? Translatable.of("UNCLAIMED_PLOT_NAME").forLocale(resident) : Colors.translateColorCodes(StringMgmt.remUnderscore(toTownBlock.getName())));

		}
		return null;
	}

	public String getTownPVPNotification(Resident resident) {

		if (!toWild && ((fromWild) || (toTownBlock.getPermissions().pvp != fromTownBlock.getPermissions().pvp))) {
			return String.format(areaTownPvPNotificationFormat, !CombatUtil.preventPvP(to.getTownyWorld(), toTownBlock) ? Translatable.of("status_title_pvp").forLocale(resident) : Translatable.of("status_title_nopvp").forLocale(resident));
		}
		return null;
	}

	private boolean testWorldPVP() {

		return to.getTownyWorld().isPVP();
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

		output = getDistrictNotification();
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
			return getCostNotification(toTownBlock.getPlotObjectGroup().getPrice());
		
		if (toForSale && !toPlotGroupBlock)
			return getCostNotification(toTownBlock.getPlotPrice());
		
		if (!toForSale && fromForSale && !toWild)
			return notForSaleNotificationFormat;
		
		return null;
	}

	private String getCostNotification(double price) {
		String forSaleSlug = String.format(forSaleNotificationFormat, getOwner(), getCost(price));
		if (viewerResident.getTownBlocks().isEmpty())
			forSaleSlug += Translatable.of("chunknotification_plot_claim_help_message").forLocale(viewerResident);
		return forSaleSlug;
	}

	private String getOwner() {
		return toTownBlock.hasResident() ? toTownBlock.getResidentOrNull().getName() : toTownBlock.getTownOrNull().getName();
	}

	private String getCost(double cost) {
		return TownyEconomyHandler.isActive() ? TownyEconomyHandler.getFormattedBalance(cost) : "$ 0";
	}

	public String getGroupNotification() {
		if (toPlotGroupBlock && (fromPlotGroup != toPlotGroup))
			return String.format(groupNotificationFormat, StringMgmt.remUnderscore(toTownBlock.getPlotObjectGroup().getName()));
		return null;
	}

	public String getDistrictNotification() {
		if (toDistrictBlock && (fromDistrict != toDistrict))
			return String.format(districtNotificationFormat, StringMgmt.remUnderscore(toDistrict.getName()));
		return null;
	}

	public String getPlotTypeNotification() {

		if (toPlotType != null && !toPlotType.equals(fromPlotType) && !TownBlockType.RESIDENTIAL.equals(toPlotType))
			return String.format(plotTypeNotificationFormat, StringMgmt.capitalize(toPlotType.getName()));
		return null;
	}
}
