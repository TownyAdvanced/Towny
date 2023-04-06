package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownTrustAddEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Town town;
	private final Resident trustedResident;
	private final CommandSender sender;

	public TownTrustAddEvent(CommandSender sender, Resident trustedResident, Town town) {
		this.town = town;
		this.trustedResident = trustedResident;
		this.sender = sender;
		setCancelMessage(Translation.of("msg_err_command_disable"));
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

	@NotNull
	public CommandSender getSender() {
		return sender;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
