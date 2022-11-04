package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;

public class TownTrustTownRemoveEvent extends CancellableTownyEvent {
	private final Town town;
	private final Town trustTown;
	private final CommandSender sender;

	public TownTrustTownRemoveEvent(CommandSender sender, Town trustTown, Town town) {
		this.town = town;
		this.trustTown = trustTown;
		this.sender = sender;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	/**
	 * @return The town where the town is being removed as trusted.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return The town that is being removed as trusted.
	 */
	public Town getTrustedTown() {
		return trustTown;
	}
	
	public @NotNull CommandSender getSender() {
		return sender;
	}
}
