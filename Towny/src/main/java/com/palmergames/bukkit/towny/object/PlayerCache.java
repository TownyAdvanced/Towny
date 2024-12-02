package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import java.util.HashMap;
import java.util.Map;

public class PlayerCache {

	private final Map<Material, Boolean> buildMatPermission = new HashMap<>();
	private final Map<Material, Boolean> destroyMatPermission = new HashMap<>();
	private final Map<Material, Boolean> switchMatPermission = new HashMap<>();
	private final Map<Material, Boolean> itemUseMatPermission = new HashMap<>();

	private WorldCoord lastWorldCoord;
	private String blockErrMsg;
	public PlayerCache(Player player) {
		this.lastWorldCoord = WorldCoord.parseWorldCoord(player);
	}

	/**
	 * Update the cache with new coordinates.
	 * 
	 * @param worldCoord - World Coordinate to set as the lastWorldCoord
	 */
	public void setLastTownBlock(@NotNull WorldCoord worldCoord) {

		this.lastWorldCoord = worldCoord;
	}
	
	/**
	 * Reset the cache permissions and update the cache with new coordinates.
	 * 
	 * @param worldCoord - World Coordinate to setLastTownBlock
	 */
	public void resetAndUpdate(@NotNull WorldCoord worldCoord) {
		
		reset(worldCoord);
	}

	/**
	 * Retrieve the last cached WorldCoord
	 * 
	 * @return WorldCoord of the last acted upon TownBlock
	 */
	@NotNull
	public WorldCoord getLastTownBlock() {

		return lastWorldCoord;
	}
	
	/**
	 * Update the players WorldCoord, resetting all permissions if it has changed.
	 * 
	 * @param pos - WorldCoord to setLastTownBlock
	 * @return true if changed.
	 */
	public boolean updateCoord(@NotNull WorldCoord pos) {

		if (!getLastTownBlock().equals(pos)) {
			reset(pos);
			return true;
		} else
			return false;
	}

	/**
	 * Checks from cache if a certain ActionType can be performed on a given Material
	 * 
	 * @param material - Material to check
	 * @param action - ActionType to check
	 * @return true if permission to perform an ActionType based on the material is granted
	 * @throws NullPointerException if passed an invalid or NULL ActionType
	 */
	public boolean getCachePermission(Material material, ActionType action) throws NullPointerException {

		return switch (action) {
		case BUILD -> getBlockPermission(buildMatPermission, material);
		case DESTROY -> getBlockPermission(destroyMatPermission, material);
		case SWITCH -> getBlockPermission(switchMatPermission, material);
		case ITEM_USE -> getBlockPermission(itemUseMatPermission, material);
		};

	}

	public void setBuildPermission(Material material, Boolean value) {
		updateMaps(buildMatPermission, material, value);
	}

	public void setDestroyPermission(Material material, Boolean value) {
		updateMaps(destroyMatPermission, material, value);
	}

	public void setSwitchPermission(Material material, Boolean value) {
		updateMaps(switchMatPermission, material, value);
	}

	public void setItemUsePermission(Material material, Boolean value) {
		updateMaps(itemUseMatPermission, material, value);
	}

	public boolean getBuildPermission(Material material) throws NullPointerException {
		return getBlockPermission(buildMatPermission, material);
	}

	public boolean getDestroyPermission(Material material) throws NullPointerException {
		return getBlockPermission(destroyMatPermission, material);
	}

	public boolean getSwitchPermission(Material material) throws NullPointerException {
		return getBlockPermission(switchMatPermission, material);
	}

	public boolean getItemUsePermission(Material material) throws NullPointerException {
		return getBlockPermission(itemUseMatPermission, material);
	}

	private void updateMaps(Map<Material, Boolean> blockMap, Material material, Boolean value) {
		blockMap.putIfAbsent(material, value);
	}
	
	private boolean getBlockPermission(Map<Material, Boolean> blockMap, Material material) throws NullPointerException {
		return blockMap.get(material);
		
	}

	private void reset(WorldCoord wc) {

		lastWorldCoord = wc;
		townBlockStatus = null;
		blockErrMsg = null;
		
		// Clear all maps
		buildMatPermission.clear();
		destroyMatPermission.clear();
		switchMatPermission.clear();
		itemUseMatPermission.clear();
	}

	public enum TownBlockStatus {
		UNKNOWN, NOT_REGISTERED, OFF_WORLD, // In a world untouched by towny.
		ADMIN,
		UNCLAIMED_ZONE,
		NATION_ZONE,
		WARZONE,
		OUTSIDER,
		PLOT_OWNER,
		PLOT_FRIEND,
		PLOT_TOWN,
		PLOT_ALLY,
		TOWN_OWNER,
		TOWN_RESIDENT,
		TOWN_ALLY,
		TOWN_NATION,
		ENEMY,
		TOWN_TRUSTED,
		PLOT_TRUSTED,
	}

	private TownBlockStatus townBlockStatus = TownBlockStatus.UNKNOWN;

	public void setStatus(TownBlockStatus townBlockStatus) {

		this.townBlockStatus = townBlockStatus;
	}

	public TownBlockStatus getStatus() throws NullPointerException {

		if (townBlockStatus == null)
			throw new NullPointerException();
		else
			return townBlockStatus;
	}

	public void setBlockErrMsg(String blockErrMsg) {

		this.blockErrMsg = blockErrMsg;
	}

	public String getBlockErrMsg() {

		String temp = blockErrMsg;
		setBlockErrMsg(null); // Delete error msg after reading it.
		return temp;
	}

	public boolean hasBlockErrMsg() {

		return blockErrMsg != null;
	}
}
