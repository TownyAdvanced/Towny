package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;

public class TownTogglePublicEvent extends TownToggleStateEvent {
	
	public TownTogglePublicEvent(CommandSender sender, Town town, boolean admin, boolean newState) {
		super(sender, town, admin, town.isPublic(), newState);
	}

}
