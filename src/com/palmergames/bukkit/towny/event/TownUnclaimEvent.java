package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;


public class TownUnclaimEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private boolean cancelled = false;
    private Town town;
    private WorldCoord worldCoord;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public TownUnclaimEvent(Town _town, WorldCoord _worldcoord) {
        this.town = _town;
        this.worldCoord = _worldcoord;
    }

    /**
     *
     * @return the Town.
     */
    public Town getTown() {
        return town;
    }
    
    /**
    *
    * @return the Unclaimed WorldCoord.
    * 
    */
   public WorldCoord getWorldCoord() {
       return worldCoord;
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