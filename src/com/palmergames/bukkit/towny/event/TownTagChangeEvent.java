package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;

/**
 * @deprecated as of 0.96.2.10 use {@link TerritoryTagChangeEvent} instead.
 */
@Deprecated
public class TownTagChangeEvent extends TagChangeEvent {
	private final Town town;

    public TownTagChangeEvent(String newTag, Town town) {
        super(newTag);
        this.town = town;
    }

    public Town getTown() {
        return town;
    }
}
