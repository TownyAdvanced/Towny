package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

public class TownToggleUnknownEvent extends TownToggleEvent {

	private final String[] args;
	
	/**
	 * An event to be used by other plugins to simulate a /nation toggle {arguments} command.
	 * 
	 * @param sender CommandSender which has sent the commant.
	 * @param town Town being toggled.
	 * @param args String[] Subcommands following the /nation toggle portion of the command. 
	 * @param admin boolean whether this was sent by the console or someone with townyadmin priviledges
	 */
	public TownToggleUnknownEvent(CommandSender sender, Town town, boolean admin, String[] args) {
		super(sender, town, admin);
		this.args = args;
		setCancelled(true);
		setCancellationMsg(Translation.of("msg_err_invalid_property", args[0]));
	}

	/**
	 * @return args a String[] representing the words following the /town toggle command which fired this event.
	 */
	public String[] getArgs() {
		return args;
	}
}
