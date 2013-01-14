package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;


public class NationRemoveTownEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private Town town;
    private Nation nation;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public NationRemoveTownEvent(Town town, Nation nation) {
        this.town = town;
        this.nation = nation;
    }

    /**
     *
     * @return the town who has left a nation.
     */
    public Town getTown() {
        return town;
    }

    /**
     *
     * @return the nation the town has just left.
     */
    public Nation getNation() {
        return nation;
    }
    
}