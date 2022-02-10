package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownTrustAddEvent extends Event implements Cancellable {
	private static final HandlerList handlers = new HandlerList();
	private final Town town;
	private final Resident trustedResident;
	private final CommandSender sender;
	private String cancelMessage = Translation.of("msg_err_command_disable");
	private boolean cancelled = false;
	
	public TownTrustAddEvent(CommandSender sender, Resident trustedResident, Town town) {
		super(!Bukkit.isPrimaryThread());
		this.town = town;
		this.trustedResident = trustedResident;
		this.sender = sender;
	}

	/**
	 * @return The town where the resident is being added as trusted.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return The resident that is being added as trusted.
	 */
	public Resident getTrustedResident() {
		return trustedResident;
	}

	/**
	 * @return The player that is adding the resident as trusted.
	 * @deprecated As of 0.97.5.17, please use {@link #getSender()} instead.
	 */
	@Deprecated
	public @Nullable Player getPlayer() {
		return sender instanceof Player player ? player : null;
	}
	
	@NotNull
	public CommandSender getSender() {
		return sender;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
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
