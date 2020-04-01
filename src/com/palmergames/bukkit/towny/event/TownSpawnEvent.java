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
	private String cancelMessage = "Sorry, this event was canceled.";
	
	public TownSpawnEvent(Player player, Location from, Location to) {
		super(player, from, to);
		
		try {
			fromTown = WorldCoord.parseWorldCoord(from).getTownBlock().getTown();
		} catch (NotRegisteredException ignored) {}
		
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

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
