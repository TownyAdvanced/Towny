package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NationPreMergeEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Nation nation;
	private final Nation remainingNation;

	public NationPreMergeEvent(Nation nation, Nation remainingNation) {
		this.nation = nation;
		this.remainingNation = remainingNation;
	}

	public Nation getRemainingNation() {
		return remainingNation;
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
