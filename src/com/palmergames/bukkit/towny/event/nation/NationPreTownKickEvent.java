package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

public class NationPreTownKickEvent extends NationPreTownLeaveEvent {

	public NationPreTownKickEvent(Nation nation, Town town) {
		super(nation, town);
	}

}
