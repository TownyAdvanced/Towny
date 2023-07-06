package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;

public class TownToggleMobsEvent extends TownToggleStateEvent {
	
	public TownToggleMobsEvent(CommandSender sender, Town town, boolean admin, boolean newState) {
		super(sender, town, admin, town.hasMobs(), newState);
	}

}
