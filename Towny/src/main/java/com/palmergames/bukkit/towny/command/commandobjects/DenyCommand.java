package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.command.InviteCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class DenyCommand extends Command implements PluginIdentifiableCommand {
	private final Towny plugin;
	
	public DenyCommand(Towny plugin, String name) {
		super(name);
		this.plugin = plugin;
		this.description = "Deny command for Towny";
		this.usageMessage = "/" + name + " <Town>";
	}

	@Override
	public boolean execute(final @NotNull CommandSender sender, final @NotNull String alias, final @NotNull String[] args) {
		if (sender instanceof Player player) {
			InviteCommand.parseDeny(player, args);
			return true;
		} else {
			return true;
		}
	}

	@NotNull
	@Override
	public Plugin getPlugin() {
		return this.plugin;
	}
}
