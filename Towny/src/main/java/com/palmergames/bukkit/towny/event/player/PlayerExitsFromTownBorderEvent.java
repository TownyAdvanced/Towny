package com.palmergames.bukkit.towny.event.player;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.bukkit.event.player.PlayerMoveEvent;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;

public class PlayerExitsFromTownBorderEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	private final Town leftTown;
	private final PlayerMoveEvent pme;
	private final WorldCoord from;
	private final Player player;
	private final WorldCoord to;

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public PlayerExitsFromTownBorderEvent(Player player, WorldCoord to, WorldCoord from, Town leftTown, PlayerMoveEvent pme) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.leftTown = leftTown;
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

	public Town getLeftTown() {
		return leftTown;
	}

	public WorldCoord getFrom() {
		return from;
	}

	public WorldCoord getTo() {
		return to;
	}
}
