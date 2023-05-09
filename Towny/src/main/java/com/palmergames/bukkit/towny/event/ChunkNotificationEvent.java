package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.WorldCoord;

public class ChunkNotificationEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private String message;
	private final WorldCoord toCoord;
	private final WorldCoord fromCoord;
	private boolean isCancelled = false;

	/**
	 * A cancellable event that lets other plugins change or disable a
	 * ChunkNotification shown to a player when they move between plots.
	 * 
	 * @param player    Player moving between townblocks/plots/chunks.
	 * @param message   Message Towny will show as a Chunk Notification.
	 * @param toCoord   WorldCoord they are moving into.
	 * @param fromCoord WorldCoord they have left.
	 * @since 0.98.3.2
	 */
	public ChunkNotificationEvent(Player player, String message, WorldCoord toCoord, WorldCoord fromCoord) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.setMessage(message);
		this.toCoord = toCoord;
		this.fromCoord = fromCoord;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		isCancelled = cancelled;
	}

	/**
	 * @return Player which will be shown a Chunk Notification.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return The message the player will see.
	 */
	public String getMessage() {
		return message;
	}

	/**
	 * @param message The ChunkNotification message that will be shown to the player.
	 */
	public void setMessage(String message) {
		this.message = message;
	}

	/**
	 * @return The WorldCoord that the player has moved into.
	 */
	public WorldCoord getToCoord() {
		return toCoord;
	}

	/**
	 * @return The WorldCoord that the player has left.
	 */
	public WorldCoord getFromCoord() {
		return fromCoord;
	}

}
