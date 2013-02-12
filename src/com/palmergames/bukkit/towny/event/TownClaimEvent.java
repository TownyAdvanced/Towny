package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownBlock;

/*
 * This event runs Async. Be aware of such.
 */
public class TownClaimEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private TownBlock townBlock;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public TownClaimEvent(TownBlock _townBlock) {
        this.townBlock = _townBlock;
    }

    /**
     *
     * @return the new TownBlock.
     */
    public TownBlock getTownBlock() {
        return townBlock;
    }
    
}