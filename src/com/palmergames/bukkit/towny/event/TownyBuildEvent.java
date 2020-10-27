package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownyBuildEvent extends Event implements Cancellable {

	private Player player;
	private Location loc;
	private Material mat;
	private boolean cancelled;
	private static final HandlerList handlers = new HandlerList();
	
	public TownyBuildEvent(Player player, Location loc, Material mat, boolean cancelled) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.loc = loc;
		this.mat = mat;
		setCancelled(cancelled);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public Material getMaterial() {
		return mat;
	}

	public Location getLocation() {
		return loc;
	}

	public Player getPlayer() {
		return player;
	}

}
