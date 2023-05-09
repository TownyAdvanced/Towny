package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Author: Chris H (Zren / Shade)
 * Date: 5/23/12
 *
 * Fired after a resident has been added to a town.
 */
public class TownAddResidentEvent extends Event {

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

    public TownAddResidentEvent(Resident resident, Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.resident = resident;
        this.town = town;
    }

    /**
     *
     * @return the resident who has joined a town.
     */
    public Resident getResident() {
        return resident;
    }

    /**
     *
     * @return the town the resident has just joined.
     */
    public Town getTown() {
        return town;
    }

	/**
	 * If this event has been thrown by a resident starting a new town, the town
	 * will not have set their mayor yet. You should delay your EventHandler by 1
	 * tick if you need the mayor of the town in your EventHandler.
	 * 
	 * @return the Mayor of the town which has added a resident, or null if this
	 *         event has been thrown upon a resident creating a new town.
	 */
	@Nullable
	public Resident getMayor() {
		return town.hasMayor() ? town.getMayor() : null;
	}
}