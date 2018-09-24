package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class DeleteTownEvent extends Event  implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    public boolean cancelled = false;
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

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}