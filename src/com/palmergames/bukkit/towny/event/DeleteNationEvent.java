package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class DeleteNationEvent extends Event implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    public boolean cancelled = false;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}