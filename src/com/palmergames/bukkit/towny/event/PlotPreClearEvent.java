package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;

public class PlotPreClearEvent extends CancellableTownyEvent {

	private final TownBlock townBlock;

	public PlotPreClearEvent(TownBlock _townBlock) {
		this.townBlock = _townBlock;
	}

	/**
	 * @return the new TownBlock.
	 */
	public TownBlock getTownBlock() {
		return townBlock;
	}
}
