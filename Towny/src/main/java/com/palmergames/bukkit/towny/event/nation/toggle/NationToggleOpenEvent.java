package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Nation;

public class NationToggleOpenEvent extends NationToggleStateEvent {
	
	public NationToggleOpenEvent(CommandSender sender, Nation nation, boolean admin, boolean newState) {
		super(sender, nation, admin, nation.isOpen(), newState);
	}
	
}
