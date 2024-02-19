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

public class CancelCommand extends Command implements PluginIdentifiableCommand {
	private final Towny plugin;
	
	public CancelCommand(Towny plugin, String name) {
		super(name);
		this.plugin = plugin;
		this.description = "Cancel command for Towny";
		this.usageMessage = "/" + name;
	}

	@Override
	public boolean execute(final @NotNull CommandSender sender, final @NotNull String alias, final @NotNull String[] args) {
		if (!ConfirmationHandler.hasConfirmation(sender)) {
			TownyMessaging.sendErrorMsg(sender, Translatable.of("no_confirmations_open"));
			return true;
		}
		
		ConfirmationHandler.revokeConfirmation(sender);
		return true;
	}

	@NotNull
	@Override
	public Plugin getPlugin() {
		return this.plugin;
	}
}
