package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NationSanctionTownAddEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Nation nation;
	private final Town town;

	public NationSanctionTownAddEvent(Nation nation, Town town) {
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
