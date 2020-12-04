package com.palmergames.bukkit.towny.event.town;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;

public class TownPreSetHomeBlockEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private boolean cancelled;
	private String cancelMessage = Translation.of("msg_err_homeblock_has_not_been_set");
	private final Town town;
	private final TownBlock townBlock;
	private final Player player;
	
	public TownPreSetHomeBlockEvent(Town town, TownBlock townBlock, Player player) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.town = town;
		this.townBlock = townBlock;
		this.player = player;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}

	@Override
	public void setCancelled(boolean cancel) {
		this.cancelled = cancel;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	/**
	 * 
	 * @return Town which is about to set their homeblock.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * 
	 * @return TownBlock which will become the homeblock.
	 */
	public TownBlock getTownBlock() {
		return townBlock;
	}

	/**
	 * 
	 * @return Player which is setting the town's homeblock.
	 */
	public Player getPlayer() {
		return player;
	}

	/**
	 * 
	 * @return String which is the error message shown to the player when this event is cancelled.
	 */
	public String getCancelMessage() {
		return cancelMessage;
	}

	/**
	 * Set a custom error message show to the player when the event is cancelled.
	 * 
	 * @param cancelMessage String which will be the error message.
	 */
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
