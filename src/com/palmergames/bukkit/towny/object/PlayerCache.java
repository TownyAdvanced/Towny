package com.palmergames.bukkit.towny.object;

import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;

public class PlayerCache {

	private HashMap<Integer, HashMap<Byte, Boolean>> buildPermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
	private HashMap<Integer, HashMap<Byte, Boolean>> destroyPermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
	private HashMap<Integer, HashMap<Byte, Boolean>> switchPermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
	private HashMap<Integer, HashMap<Byte, Boolean>> itemUsePermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
	
	private WorldCoord lastWorldCoord;
	private String blockErrMsg;
	private Location lastLocation;

	//TODO: cache last entity attacked

	public PlayerCache(TownyWorld world, Player player) {

		this(new WorldCoord(world.getName(), Coord.parseCoord(player)));
		setLastLocation(player.getLocation());
	}

	public PlayerCache(WorldCoord worldCoord) {

		this.setLastTownBlock(worldCoord);
	}

	/**
	 * Update the cache with new coordinates.
	 * 
	 * @param worldCoord
	 */
	public void setLastTownBlock(WorldCoord worldCoord) {

		this.lastWorldCoord = worldCoord;
	}
	
	/**
	 * Reset the cache permissions and update the cache with new coordinates.
	 * 
	 * @param worldCoord
	 */
	public void resetAndUpdate(WorldCoord worldCoord) {
		
		reset();
		setLastTownBlock(worldCoord);
	}

	/**
	 * Retrieve the last cached WorldCoord
	 * 
	 * @return WorldCoord of the last acted upon TownBlock
	 */
	public WorldCoord getLastTownBlock() {

		return lastWorldCoord;
	}
	
	/**
	 * Update the players WorldCoord, resetting all permissions if it has changed.
	 * 
	 * @param pos
	 * @return true if changed.
	 */
	public boolean updateCoord(WorldCoord pos) {

		if (!getLastTownBlock().equals(pos)) {
			reset();
			setLastTownBlock(pos);
			return true;
		} else
			return false;
	}
	
	public boolean getCachePermission(Integer id, byte data, ActionType action) throws NullPointerException {

		switch (action) {

		case BUILD: // BUILD
			return getBuildPermission(id, data);

		case DESTROY: // DESTROY
			return getDestroyPermission(id, data);

		case SWITCH: // SWITCH
			return getSwitchPermission(id, data);

		case ITEM_USE: // ITEM_USE
			return getItemUsePermission(id, data);

		default:
			throw new NullPointerException();

		}

	}

	public void setBuildPermission(Integer id, byte data, Boolean value) {

		updateMaps(buildPermission, id, data, value);

	}
	public void setDestroyPermission(Integer id, byte data, Boolean value) {

		updateMaps(destroyPermission, id, data, value);
	}
	public void setSwitchPermission(Integer id, byte data, Boolean value) {

		updateMaps(switchPermission, id, data, value);

	}
	public void setItemUsePermission(Integer id, byte data, Boolean value) {

		updateMaps(itemUsePermission, id, data, value);
		
	}
	
	public boolean getBuildPermission(Integer id, byte data) throws NullPointerException {

		return getBlockPermission(buildPermission, id, data);

	}
	public boolean getDestroyPermission(Integer id, byte data) throws NullPointerException {

		return getBlockPermission(destroyPermission, id, data);
		
	}
	public boolean getSwitchPermission(Integer id, byte data) throws NullPointerException {

		return getBlockPermission(switchPermission, id, data);
		
	}
	public Boolean getItemUsePermission(Integer id, byte data) throws NullPointerException {

		return getBlockPermission(itemUsePermission, id, data);
		
	}
	
	private void updateMaps(HashMap<Integer, HashMap<Byte, Boolean>> blockMap, Integer id, byte data, Boolean value) {
		
		if (!blockMap.containsKey(id)) {
			/*
			 * We have no permissions cached for this block.
			 */
			HashMap<Byte, Boolean> map = new HashMap<Byte, Boolean>();
			map.put(data, value);
			blockMap.put(id, map);
		} else {
			/*
			 * We have cached permissions for this block type so just push updated data.
			 */
			blockMap.get(id).put(data, value);		
			
		}
	}
	
	private boolean getBlockPermission(HashMap<Integer, HashMap<Byte, Boolean>> blockMap, Integer id, byte data) throws NullPointerException {
		
		if (!blockMap.containsKey(id))
			throw new NullPointerException();
		
		HashMap<Byte, Boolean> map = blockMap.get(id);
		
		if (!map.containsKey(data))
			throw new NullPointerException();
		
		return map.get(data);
		
	}

	private void reset() {

		lastWorldCoord = null;
		townBlockStatus = null;
		blockErrMsg = null;
		
		buildPermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
		destroyPermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
		switchPermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
		itemUsePermission = new HashMap<Integer, HashMap<Byte,Boolean>>();
	}

	public enum TownBlockStatus {
		UNKOWN, NOT_REGISTERED, OFF_WORLD, // In a world untouched by towny.
		ADMIN,
		UNCLAIMED_ZONE,
		LOCKED,
		WARZONE,
		OUTSIDER,
		PLOT_OWNER,
		PLOT_FRIEND,
		PLOT_ALLY,
		TOWN_OWNER,
		TOWN_RESIDENT,
		TOWN_ALLY,
		ENEMY
	};

	private TownBlockStatus townBlockStatus = TownBlockStatus.UNKOWN;

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

	public void setLastLocation(Location lastLocation) {

		this.lastLocation = lastLocation.clone();
	}

	public Location getLastLocation() throws NullPointerException {

		if (lastLocation == null)
			throw new NullPointerException();
		else
			return lastLocation;
	}
}
