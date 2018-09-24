package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;

/*
 * @author LlmDl
 * 
 */

public class PreDeleteTownEvent extends Event implements Cancellable{
	 private static final HandlerList handlers = new HandlerList();
	 public boolean cancelled = false;
	 private String townName;
	 private Town town;

	 @Override
	 public HandlerList getHandlers() {
	    	
	 	return handlers; }
	 	public static HandlerList getHandlerList() {

	 	return handlers;
	 }

	 public PreDeleteTownEvent(String town) {
	        this.townName = town;
	    }
	    public PreDeleteTownEvent(Town town) {
	        this.town = town;
	    }

	    /**
	     *
	     * @return the deleted town name.
	     */
	    public String getTownName() {
	        return townName;
	    }
	    
	    /**
	     *
	     * @return the deleted town object.
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
