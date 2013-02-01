package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class DeleteTownEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private String townName;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public DeleteTownEvent(String town) {
        this.townName = town;
    }

    /**
     *
     * @return the deleted town name.
     */
    public String getTownName() {
        return townName;
    }
    
}