package com.palmergames.bukkit.towny;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;

public class PlayerCache {
	private WorldCoord lastTownBlock;
	private Boolean buildPermission, destroyPermission, switchPermission, itemUsePermission;
	private String blockErrMsg;
	private Location lastLocation;
	//TODO: cache last entity attacked

	public PlayerCache(TownyWorld world, Player player) {
		this(new WorldCoord(world, Coord.parseCoord(player)));
		setLastLocation(player.getLocation());
	}
	
	public PlayerCache(WorldCoord lastTownBlock) {
		this.setLastTownBlock(lastTownBlock);
	}

	/**
	 * Update the cache with new coordinates. Reset the other cached permissions.
	 * @param lastTownBlock
	 */
	
	public void setLastTownBlock(WorldCoord lastTownBlock) {
		reset();
		this.lastTownBlock = lastTownBlock;
	}

	public WorldCoord getLastTownBlock() {
		return lastTownBlock;
	}
	
	public boolean getCachePermission(ActionType action) throws NullPointerException {
		
		switch(action.ordinal()){
		
		case 0: // BUILD
			if (buildPermission == null)
				throw new NullPointerException();
			else
				return buildPermission;
			
		case 1: // DESTROY
			if (destroyPermission == null)
				throw new NullPointerException();
			else
				return destroyPermission;			
			
		case 2: // SWITCH
			if (switchPermission == null)
				throw new NullPointerException();
			else
				return switchPermission;			
			
		case 3: // ITEM_USE
			if (itemUsePermission == null)
				throw new NullPointerException();
			else
				return itemUsePermission;
			
		default:
			throw new NullPointerException();
			
		}
		
	}

	public void setBuildPermission(boolean buildPermission) {
		this.buildPermission = buildPermission;
	}

	public boolean getBuildPermission() throws NullPointerException {
		if (buildPermission == null)
			throw new NullPointerException();
		else
			return buildPermission;
	}

	public void setDestroyPermission(boolean destroyPermission) {
		this.destroyPermission = destroyPermission;
	}

	public boolean getDestroyPermission() throws NullPointerException {
		if (destroyPermission == null)
			throw new NullPointerException();
		else
			return destroyPermission;
	}
	
	public void setSwitchPermission(boolean switchPermission) {
		this.switchPermission = switchPermission;
	}
	
	public boolean getSwitchPermission() throws NullPointerException {
		if (switchPermission == null)
			throw new NullPointerException();
		else
			return switchPermission;
	}
	
	public boolean updateCoord(WorldCoord pos) {
		if (!getLastTownBlock().equals(pos)) {
			setLastTownBlock(pos);
			return true;
		} else
			return false;
	}
	
	private void reset() {
		lastTownBlock = null;
		buildPermission = null;
		destroyPermission = null;
		townBlockStatus = null;
		switchPermission = null;
		itemUsePermission = null;
		blockErrMsg = null;
	}
	
	public enum TownBlockStatus {
		UNKOWN,
		NOT_REGISTERED,
		OFF_WORLD, // In a world untouched by towny.
		ADMIN,
		UNCLAIMED_ZONE,
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

	public void setItemUsePermission(Boolean itemUsePermission) {
		this.itemUsePermission = itemUsePermission;
	}

	public Boolean getItemUsePermission() throws NullPointerException {
		if (itemUsePermission == null)
			throw new NullPointerException();
		else
			return itemUsePermission;
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
