package com.palmergames.bukkit.towny.event.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class PlayerCacheGetTownBlockStatusEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private TownBlockStatus townBlockStatus;
	private final Player player;
	private final WorldCoord worldCoord;
	
	public PlayerCacheGetTownBlockStatusEvent(Player player, WorldCoord worldCoord, TownBlockStatus townBlockStatus) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.worldCoord = worldCoord;
		this.townBlockStatus = townBlockStatus;
		
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
	
	/**
	 * @return player {@link Player} which is having a TownBlockStatus generated for their PlayerCache.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return worldCoord {@link WorldCoord} where this TownBlockStatus is being gotten for.
	 */
	public WorldCoord getWorldCoord() {
		return worldCoord;
	}

	/**
	 * @return the {@link TownBlockStatus} which has been pre-determined by Towny.
	 */
	public TownBlockStatus getTownBlockStatus() {
		return townBlockStatus;
	}

	/**
	 * @param townBlockStatus the {@link TownBlockStatus} to use.
	 */
	public void setTownBlockStatus(TownBlockStatus townBlockStatus) {
		this.townBlockStatus = townBlockStatus;
	}
}
