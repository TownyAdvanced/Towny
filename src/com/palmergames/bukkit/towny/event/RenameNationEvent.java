package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;


public class RenameNationEvent extends Event  implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    
    private String oldName;
    private Nation nation;
    public boolean cancelled = false;
    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public RenameNationEvent(String oldName, Nation nation) {
        this.oldName = oldName;
        this.nation = nation;
    }

    /**
     *
     * @return the old nation name.
     */
    public String getOldName() {
        return oldName;
    }
    
    /**
    *
    * @return the nation with it's changed name
    */
   public Nation getNation() {
       return this.nation;
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