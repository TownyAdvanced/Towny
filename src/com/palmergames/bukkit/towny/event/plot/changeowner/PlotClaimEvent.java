package com.palmergames.bukkit.towny.event.plot.changeowner;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;

public class PlotClaimEvent extends PlotChangeOwnerEvent {

	public PlotClaimEvent(Resident oldResident, Resident newResident, TownBlock townBlock) {
		super(oldResident, newResident, townBlock);
	}
}
