package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownTrustRemoveEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final Resident trustedResident;
	private final Player player;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	private boolean cancelled = false;
	
	public TownTrustRemoveEvent(Player player, Resident trustedResident, Town town) {
		super(!Bukkit.isPrimaryThread());
		this.town = town;
		this.trustedResident = trustedResident;
		this.player = player;
	}

	/**
	 * @return The town where the player is being removed as trusted.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return The resident that is being removed as trusted.
	 */
	public Resident getTrustedResident() {
		return trustedResident;
	}

	/**
	 * @return The player is removing the resident as trusted.
	 */
	public Player getPlayer() {
		return player;
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

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
