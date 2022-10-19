package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;

public class PreNewTownEvent extends CancellableTownyEvent {

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
}
