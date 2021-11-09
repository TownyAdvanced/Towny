package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player uses /town set spawn
 */
public class TownSetSpawnEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	
	private final Town town;
	private final Player player;
	private final Location oldSpawn;
	private Location newSpawn;
	
	private boolean cancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	
	public TownSetSpawnEvent(Town town, Player player, Location newSpawn) {
		this.town = town;
		this.player = player;
		this.oldSpawn = town.getSpawnOrNull();
		this.newSpawn = newSpawn;
	}

	/**
	 * @return The town for which this spawn is being set.
	 */
	@NotNull
	public Town getTown() {
		return town;
	}

	/**
	 * @return The player that ran the command
	 */
	@NotNull
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return The town's old spawnpoint location.
	 */
	@Nullable
	public Location getOldSpawn() {
		return oldSpawn;
	}

	/**
	 * @return The new spawn location.
	 */
	@NotNull
	public Location getNewSpawn() {
		return newSpawn;
	}

	public void setNewSpawn(@NotNull Location newSpawn) {
		this.newSpawn = newSpawn;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
