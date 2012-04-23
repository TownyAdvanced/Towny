package com.palmergames.bukkit.towny.object;

import java.util.HashMap;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;

public class PlayerCache {

	private Map<Integer, Boolean> buildPermission = new HashMap<Integer, Boolean>();
	private Map<Integer, Boolean> destroyPermission = new HashMap<Integer, Boolean>();
	private Map<Integer, Boolean> switchPermission = new HashMap<Integer, Boolean>();
	private Map<Integer, Boolean> itemUsePermission = new HashMap<Integer, Boolean>();
	
	private WorldCoord lastTownBlock;
	private String blockErrMsg;
	private Location lastLocation;

	//TODO: cache last entity attacked

	public PlayerCache(TownyWorld world, Player player) {

		this(new WorldCoord(world.getName(), Coord.parseCoord(player)));
		setLastLocation(player.getLocation());
	}

	public PlayerCache(WorldCoord lastTownBlock) {

		this.setLastTownBlock(lastTownBlock);
	}

	/**
	 * Update the cache with new coordinates. Reset the other cached
	 * permissions if the TownBlock has changed
	 * 
	 * @param lastTownBlock
	 */

	public void setLastTownBlock(WorldCoord lastTownBlock) {

		if ((getLastTownBlock() != null)  && !getLastTownBlock().equals(lastTownBlock))
			reset();
		
		this.lastTownBlock = lastTownBlock;
	}

	public WorldCoord getLastTownBlock() {

		return lastTownBlock;
	}
	
	public boolean updateCoord(WorldCoord pos) {

		if (!getLastTownBlock().equals(pos)) {
			setLastTownBlock(pos);
			return true;
		} else
			return false;
	}


	public boolean getCachePermission(Integer id, ActionType action) throws NullPointerException {

		switch (action) {

		case BUILD: // BUILD
			if (!buildPermission.containsKey(id))
				throw new NullPointerException();
			else
				return buildPermission.get(id);

		case DESTROY: // DESTROY
			if (!destroyPermission.containsKey(id))
				throw new NullPointerException();
			else
				return destroyPermission.get(id);

		case SWITCH: // SWITCH
			if (!switchPermission.containsKey(id))
				throw new NullPointerException();
			else
				return switchPermission.get(id);

		case ITEM_USE: // ITEM_USE
			if (!itemUsePermission.containsKey(id))
				throw new NullPointerException();
			else
				return itemUsePermission.get(id);

		default:
			throw new NullPointerException();

		}

	}

	public void setBuildPermission(Integer id, Boolean value) {

		buildPermission.put(id, value);
	}

	public boolean getBuildPermission(Integer id) throws NullPointerException {

		if (!buildPermission.containsKey(id))
			throw new NullPointerException();
		else
			return buildPermission.get(id);
	}

	public void setDestroyPermission(Integer id, Boolean value) {

		destroyPermission.put(id, value);
	}

	public boolean getDestroyPermission(Integer id) throws NullPointerException {

		if (!destroyPermission.containsKey(id))
			throw new NullPointerException();
		else
			return destroyPermission.get(id);
	}

	public void setSwitchPermission(Integer id, Boolean value) {

		switchPermission.put(id, value);
	}

	public boolean getSwitchPermission(Integer id) throws NullPointerException {

		if (!switchPermission.containsKey(id))
			throw new NullPointerException();
		else
			return switchPermission.get(id);
	}
	
	public void setItemUsePermission(Integer id, Boolean value) {

		itemUsePermission.put(id, value);
	}

	public Boolean getItemUsePermission(Integer id) throws NullPointerException {

		if (!itemUsePermission.containsKey(id))
			throw new NullPointerException();
		else
			return itemUsePermission.get(id);
	}

	private void reset() {

		lastTownBlock = null;
		townBlockStatus = null;
		blockErrMsg = null;
		
		buildPermission = new HashMap<Integer, Boolean>();
		destroyPermission = new HashMap<Integer, Boolean>();
		switchPermission = new HashMap<Integer, Boolean>();
		itemUsePermission = new HashMap<Integer, Boolean>();
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
