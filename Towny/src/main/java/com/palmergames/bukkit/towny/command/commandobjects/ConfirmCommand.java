package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.confirmations.ConfirmationHandler;

import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class ConfirmCommand extends Command implements PluginIdentifiableCommand {
	private final Towny plugin;
	
	public ConfirmCommand(Towny plugin, String name) {
		super(name);
		this.plugin = plugin;
		this.description = "Confirm command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(final @NotNull CommandSender sender, final @NotNull String alias, final @NotNull String[] args) {
		
		// Check if confirmation is available.
		if (!ConfirmationHandler.hasConfirmation(sender)) {
			TownyMessaging.sendMsg(sender, Translatable.of("no_confirmations_open"));
			return true;
		}
		
		// Handle the confirmation.
		ConfirmationHandler.acceptConfirmation(sender);
		return true;
	}

	@NotNull
	@Override
	public Plugin getPlugin() {
		return this.plugin;
	}
}
