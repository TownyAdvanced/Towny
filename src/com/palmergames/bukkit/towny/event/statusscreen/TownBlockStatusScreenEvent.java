package com.palmergames.bukkit.towny.event.statusscreen;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import org.bukkit.command.CommandSender;

public class TownBlockStatusScreenEvent extends StatusScreenEvent {

	private final TownBlock townBlock;

	public TownBlockStatusScreenEvent(StatusScreen screen, CommandSender receiver, TownBlock townBlock) {
		super(screen, receiver);
		this.townBlock = townBlock;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

}
