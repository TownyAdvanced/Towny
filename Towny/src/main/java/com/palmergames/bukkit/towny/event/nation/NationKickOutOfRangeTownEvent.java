package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NationKickOutOfRangeTownEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final Nation nation;

	public NationKickOutOfRangeTownEvent(Nation nation, Town town) {
		this.nation = nation;
        this.town = town;
	}

	public Nation getNation() {
		return nation;
	}

	public Town getTown() {
		return town;
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
