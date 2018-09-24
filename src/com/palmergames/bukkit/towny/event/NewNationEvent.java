package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Nation;


public class NewNationEvent extends Event  implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    public boolean cancelled = false;
    private Nation nation;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public NewNationEvent(Nation nation) {
        this.nation = nation;
    }

    /**
     *
     * @return the new nation.
     */
    public Nation getNation() {
        return nation;
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
