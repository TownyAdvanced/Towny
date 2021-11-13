package com.palmergames.bukkit.towny.event.plot.changeowner;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Cancellable;

public class PlotPreClaimEvent extends PlotChangeOwnerEvent implements Cancellable {
	private boolean cancelled;
	private String cancelMessage = "";
	
	public PlotPreClaimEvent(Resident oldResident, Resident newResident, TownBlock townBlock) {
		super (oldResident, newResident, townBlock);
	}

	@Override
	public boolean isCancelled() {
		return cancelled;
	}
	
	@Override
	public void setCancelled(boolean cancelled) {
		this.cancelled = cancelled;
	}
	
	public String getCancelMessage() {
		return cancelMessage;
	}
	
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
