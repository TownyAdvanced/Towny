package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownTrustRemoveEvent extends CancellableTownyEvent {
	private final Town town;
	private final Resident trustedResident;
	private final CommandSender sender;

	public TownTrustRemoveEvent(CommandSender sender, Resident trustedResident, Town town) {
		this.town = town;
		this.trustedResident = trustedResident;
		this.sender = sender;
		setCancelMessage(Translation.of("msg_err_command_disable"));
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
	 * @deprecated As of 0.97.5.17, please use {@link #getSender()} instead.
	 */
	@Deprecated
	public @Nullable Player getPlayer() {
		return sender instanceof Player player ? player : null;
	}

	public @NotNull CommandSender getSender() {
		return sender;
	}
}
