package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Fired before a resident has been removed from a town.
 */
public class TownPreRemoveResidentEvent extends Event {

    private static final HandlerList handlers = new HandlerList();
    
    private final Resident resident;
    private final Town town;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public TownPreRemoveResidentEvent(Resident resident, Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.resident = resident;
        this.town = town;
    }

    /**
     * @return the resident who is about to be removed from a town.
     */
    public Resident getResident() {
        return resident;
    }

    /**
     * @return the town the resident is about to be removed from.
     */
    public Town getTown() {
        return town;
    }

}
