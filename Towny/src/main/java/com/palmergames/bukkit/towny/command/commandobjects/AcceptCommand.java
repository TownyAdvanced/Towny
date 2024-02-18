package com.palmergames.bukkit.towny.command.commandobjects;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.command.InviteCommand;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginIdentifiableCommand;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

public class AcceptCommand extends Command implements PluginIdentifiableCommand {
	private final Towny plugin;
	
	public AcceptCommand(Towny plugin, String name) {
		super(name);
		this.plugin = plugin;
		this.description = "Accept command for Towny";
		this.usageMessage = "/" + name + " <Town>";
	}

	@Override
	public boolean execute(final @NotNull CommandSender sender, final @NotNull String alias, final @NotNull String[] args) {
		if (sender instanceof Player player) {
			InviteCommand.parseAccept(player, args);
		}
		return true;
	}

	@NotNull
	@Override
	public Plugin getPlugin() {
		return this.plugin;
	}
}
