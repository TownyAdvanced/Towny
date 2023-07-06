package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NationPreAddTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final String townName;
	private final Town town;
	private final String nationName;
	private final Nation nation;

	public NationPreAddTownEvent(Nation nation, Town town) {
		this.townName = town.getName();
		this.town = town;
		this.nation = nation;
		this.nationName = nation.getName();
	}

	public String getTownName() {
		return townName;
	}

	public String getNationName() {
		return nationName;
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
