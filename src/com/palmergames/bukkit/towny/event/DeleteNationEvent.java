package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class DeleteNationEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private String nationName;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public DeleteNationEvent(String nation) {
        this.nationName = nation;
    }

    /**
     *
     * @return the deleted nation name.
     */
    public String getNationName() {
        return nationName;
    }
    
}