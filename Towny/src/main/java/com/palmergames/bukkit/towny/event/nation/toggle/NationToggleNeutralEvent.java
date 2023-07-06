package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.command.CommandSender;
import com.palmergames.bukkit.towny.object.Nation;

public class NationToggleNeutralEvent extends NationToggleStateEvent {
	
	public NationToggleNeutralEvent(CommandSender sender, Nation nation, boolean admin, boolean newState) {
		super(sender, nation, admin, nation.isNeutral(), newState);
	}

}
