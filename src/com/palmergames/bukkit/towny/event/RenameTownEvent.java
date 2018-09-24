package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;


public class RenameTownEvent extends Event  implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    public boolean cancelled = false;
    private String oldName;
    private Town town;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public RenameTownEvent(String oldName, Town town) {
        this.oldName = oldName;
        this.town = town;
    }

    /**
     *
     * @return the old town name.
     */
    public String getOldName() {
        return oldName;
    }
    
    /**
    *
    * @return the town with it's changed name
    */
   public Town getTown() {
       return this.town;
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