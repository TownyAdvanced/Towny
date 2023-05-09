package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PreNewTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Player player;
	private final String townName;
	private final Location spawnLocation;
	private final WorldCoord worldCoord;

	public PreNewTownEvent(Player player, String townName, Location spawnLocation) {
		this.player = player;
		this.townName = townName;
		this.spawnLocation = spawnLocation;
		this.worldCoord = WorldCoord.parseWorldCoord(spawnLocation);
	}

	public Player getPlayer() {
		return player;
	}

	public String getTownName() {
		return townName;
	}
	
	public Location getTownLocation() {
		return this.spawnLocation;
	}
	
	public WorldCoord getTownWorldCoord() {
		return this.worldCoord;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
