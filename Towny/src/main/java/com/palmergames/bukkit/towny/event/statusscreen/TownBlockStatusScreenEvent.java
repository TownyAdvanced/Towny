package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;

public class TownBlockStatusScreenEvent extends StatusScreenEvent {

	private TownBlock townBlock;

	public TownBlockStatusScreenEvent(StatusScreen screen, TownBlock townBlock) {
		super(screen);
		this.townBlock = townBlock;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

}
