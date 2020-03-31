package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerTeleportEvent;

public class NationSpawnEvent extends PlayerTeleportEvent {
	
	private Nation toNation;
	private Nation fromNation;
	private String cancelMessage = "Sorry, this event was canceled.";
	
	public NationSpawnEvent(Player player, Location from, Location to) {
		super(player, from, to);

		try {
			fromNation = WorldCoord.parseWorldCoord(from).getTownBlock().getTown().getNation();
		} catch (NotRegisteredException e) {}

		try {
			toNation = WorldCoord.parseWorldCoord(to).getTownBlock().getTown().getNation();
		} catch (NotRegisteredException ignored) {}
	}

	public Nation getToNation() {
		return toNation;
	}

	public Nation getFromNation() {
		return fromNation;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
