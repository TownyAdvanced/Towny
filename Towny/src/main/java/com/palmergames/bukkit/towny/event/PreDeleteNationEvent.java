package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PreDeleteNationEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final String nationName;
	private final Nation nation;
	
	public PreDeleteNationEvent(Nation nation) {
		this.nation = nation;
		this.nationName = nation.getName();
	}

	/**
	 *
	 * @return the deleted nation name.
	 */
	public String getNationName() {
		return nationName;
	}

	/**
	 * @return the deleted nation object.
	 */
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
