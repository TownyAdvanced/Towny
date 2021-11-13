package com.palmergames.bukkit.towny.event.plot.changeowner;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;

public class PlotUnclaimEvent extends PlotChangeOwnerEvent {

	public PlotUnclaimEvent(Resident oldResident, Resident newResident, TownBlock townBlock) {
		super (oldResident, newResident, townBlock);
	}

}
