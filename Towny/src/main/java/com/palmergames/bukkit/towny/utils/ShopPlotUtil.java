package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;

/**
 * A util for Shop Plugin Developers to use,
 * given the player and location, the Util will return true if the 
 * player should be able to create a shop at the given location.  
 * 
 * @author LlmDl
 */
public class ShopPlotUtil {

	/**
	 * This tests that the player owns the plot at the location in question personally,
	 * as well as the plot being a shop plot type. This is a simpler, probably more likely scenario
	 * than the {@link #doesPlayerHaveAbilityToEditShopPlot(Player, Location)} test.
	 * 
	 * @param player - {@link Player} to test
	 * @param location - {@link Location} to test at
	 * @return true if the player owns the plot, and it is a Shop plot
	 */
	public static boolean doesPlayerOwnShopPlot(Player player, Location location) {
		boolean owner = false;
		if (TownyAPI.getInstance().isWilderness(location))
			return false;
		TownBlock townBlock = TownyAPI.getInstance().getTownBlock(location);
		if (townBlock.hasResident()) {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident != null) 
				owner = townBlock.getResidentOrNull().equals(resident);
		}
		
		return owner && isShopPlot(location);
	}

	/**
	 * This tests if a player has the ability to build at the location,
	 * as well as the plot being a shop plot type. This would be used for 
	 * plots that are not personally owned by the player, a public shop space
	 * in a town. This is a more complicated, but more permissive test than 
	 * the {@link #doesPlayerOwnShopPlot(Player, Location)} test.
	 * 
	 * @param player - {@link Player} to test
	 * @param location - {@link Location} to test at
	 * @return true if the player can build and the plot is a shop
	 */
	public static boolean doesPlayerHaveAbilityToEditShopPlot(Player player, Location location) {
		if (TownyActionEventExecutor.canBuild(player, location, Material.DIRT) && isShopPlot(location))
			return true;
		else return false;
	}

	/**
	 * Use this to determine if a {@link Location} is a shop plot.
	 * 
	 * @param location - {@link Location} to be tested for shop plot type. 
	 * @return true if the location is a shop plot.
	 */
	public static boolean isShopPlot(Location location) {
		if (!TownyAPI.getInstance().isWilderness(location)) {
			TownBlock townblock = TownyAPI.getInstance().getTownBlock(location);
			return isShopPlot(townblock);
		} else return false;
	}

	/**
	 * Use this to determine if a {@link TownBlock} is a shop plot. 
	 * 
	 * @param townblock - {@link TownBlock} to be tested for shop type.
	 * @return true if the townblock is a shop plot. 
	 */
	public static boolean isShopPlot(TownBlock townblock) {
		if (townblock != null) {
			if (townblock.getType().equals(TownBlockType.COMMERCIAL))
				return true;
			else return false;
		} else return false;
	}
}
