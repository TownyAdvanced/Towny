package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * An event fired before a town is deemed too far to be in the specified nation. May be fired pre-canceled due to Proximity rules.
 * Plugins may use #setCancelled(false) to bypass proximity rules and forcefully allow the town to join, or stay if it already is a member.
 */
public class NationRangeAllowTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Nation nation;
	private final Town town;

	/**
	 * An event fired before a town is deemed too far to be in the specified nation. May be fired pre-canceled due to Proximity rules.
	 * Plugins may use #setCancelled(false) to bypass proximity rules and forcefully allow the town to join, or stay if it already is a member.
	 */
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
