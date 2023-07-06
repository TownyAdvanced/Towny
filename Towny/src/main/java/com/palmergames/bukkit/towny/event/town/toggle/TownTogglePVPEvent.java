package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;

public class TownTogglePVPEvent extends TownToggleStateEvent {
	
	public TownTogglePVPEvent(CommandSender sender, Town town, boolean admin, boolean newState) {
		super(sender, town, admin, town.isPVP(), newState);
	}

}
