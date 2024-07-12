package com.palmergames.bukkit.towny.event.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.District;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.WorldCoord;

/**
 * Thrown when a player crosses into Town border.
 */
public class PlayerEntersIntoDistrictEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final District enteredDistrict;
	private final PlayerMoveEvent pme;
	private final WorldCoord from;
	private final WorldCoord to;
	private final Player player;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public PlayerEntersIntoDistrictEvent(Player player, WorldCoord to, WorldCoord from, District enteredDistrict, PlayerMoveEvent pme) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.enteredDistrict = enteredDistrict;
		this.player = player;
		this.from = from;
		this.pme = pme;
		this.to = to;
	}

	public Player getPlayer() {
		return player;
	}

	@Nullable
	public Resident getResident() {
		return TownyAPI.getInstance().getResident(player);
	}

	public PlayerMoveEvent getPlayerMoveEvent() {
		return pme;
	}

	public District getEnteredDistrict() {
		return enteredDistrict;
	}

	public WorldCoord getFrom() {
		return from;
	}

	public WorldCoord getTo() {
		return to;
	}
}
