package com.palmergames.bukkit.towny.event.town.toggle;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.command.CommandSender;

public class TownToggleSnowEvent extends TownToggleStateEvent {

	public TownToggleSnowEvent(CommandSender sender, Town town, boolean admin, boolean newState) {
		super(sender, town, admin, town.isSnow(), newState);
	}

}
