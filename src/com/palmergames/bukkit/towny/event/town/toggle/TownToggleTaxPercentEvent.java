package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;

public class TownToggleTaxPercentEvent extends TownToggleStateEvent {
	
	public TownToggleTaxPercentEvent(CommandSender sender, Town town, boolean admin, boolean newState) {
		super(sender, town, admin, town.isTaxPercentage(), newState);
	}
}
