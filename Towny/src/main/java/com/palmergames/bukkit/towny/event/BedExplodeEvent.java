package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Called when a {@link Player} tries to set their spawn point at a bed or respawn anchor in a dimension that doesn't support them.
 */
public class BedExplodeEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	
	private final Player player;
	private final Location location1;
	private final Location location2;
	private final Material material;
	
	public BedExplodeEvent(Player player, Location loc1, Location loc2, Material mat) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.location1 = loc1;
		this.location2 = loc2;
		this.material = mat;
	}
	
	/**
	 * @return The location of the part of the bed or respawn anchor that the player clicked.
	 */
	@NotNull
	public Location getLocation() {
		return location1;
	}
	
	/**
	 * 
	 * @return The bed's second block, or {@code null} if this was a respawn anchor explosion.
	 */
	@Nullable
	public Location getLocation2() {
		return location2;
	}
	
	/**
	 * @return The material of the block that blew up.
	 */
	@NotNull
	public Material getMaterial() {
		return material;
	}
	
	@NotNull
	public Player getPlayer() {
		return player;
	}

	@Override
	@NotNull
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
