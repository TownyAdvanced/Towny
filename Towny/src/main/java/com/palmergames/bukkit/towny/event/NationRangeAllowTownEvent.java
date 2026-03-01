package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NationRangeAllowTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Nation nation;
	private final Town town;

	public NationRangeAllowTownEvent(Nation nation, Town town) {
		this.town = town;
		this.nation = nation;
	}

	public Town getTown() {
		return town;
	}

	public Nation getNation() {
		return nation;
	}
	
	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
