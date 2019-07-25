package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

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

    public TownClaimEvent(TownBlock townBlock) {
    	super(!Bukkit.getServer().isPrimaryThread());
        this.townBlock = townBlock;
    }

    /**
     *
     * @return the new TownBlock.
     */
    public TownBlock getTownBlock() {
        return townBlock;
    }
    
}