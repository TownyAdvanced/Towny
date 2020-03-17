package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class TownSpawnEvent extends PlayerTeleportEvent {
	
	Town fromTown;
	private Town toTown;
	
	public TownSpawnEvent(Player player, Location from, Location to) {
		super(player, from, to);
		
		try {
			fromTown = WorldCoord.parseWorldCoord(from).getTownBlock().getTown();
		} catch (NotRegisteredException e) {
			return;
		}
		
		try {
			toTown = WorldCoord.parseWorldCoord(to).getTownBlock().getTown();
		} catch (NotRegisteredException ignored) {}
		
	}

	public Town getToTown() {
		return toTown;
	}

	public Town getFromTown() {
		return fromTown;
	}
}
