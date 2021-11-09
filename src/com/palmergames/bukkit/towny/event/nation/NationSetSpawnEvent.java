package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player uses /nation set spawn
 */
public class NationSetSpawnEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	
	private final Nation nation;
	private final Player player;
	private final Location oldSpawn;
	private Location newSpawn;
	
	private boolean cancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	
	public NationSetSpawnEvent(Nation nation, Player player, Location newSpawn) {
		this.nation = nation;
		this.player = player;
		this.oldSpawn = nation.getSpawnOrNull();
		this.newSpawn = newSpawn;
	}

	/**
	 * @return The nation for which this spawn is being set.
	 */
	@NotNull
	public Nation getNation() {
		return nation;
	}

	/**
	 * @return The player that is changing the spawn.
	 */
	@NotNull
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return The old spawn, or {@code null} if none has been set.
	 */
	@Nullable
	public Location getOldSpawn() {
		return oldSpawn;
	}

	/**
	 * @return The location where the spawn is being set to.
	 */
	@NotNull
	public Location getNewSpawn() {
		return newSpawn;
	}

	/**
	 * @param newSpawn Sets the location where the new spawn will be set to.
	 */
	public void setNewSpawn(@NotNull Location newSpawn) {
		this.newSpawn = newSpawn;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
}
