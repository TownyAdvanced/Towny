package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 5/23/12
 *
 * Fired after a resident has been removed from a town.
 */
public class TownRemoveResidentEvent extends Event implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private Resident resident;
    private Town town;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public TownRemoveResidentEvent(Resident resident, Town town) {
        this.resident = resident;
        this.town = town;
    }

    /**
     *
     * @return the resident who has been removed from a town.
     */
    public Resident getResident() {
        return resident;
    }

    /**
     *
     * @return the town the resident was previously in.
     */
    public Town getTown() {
        return town;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
