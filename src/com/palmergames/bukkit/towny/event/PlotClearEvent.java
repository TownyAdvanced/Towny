package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.TownBlock;


public class PlotClearEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    public boolean cancelled = false;
    private TownBlock townBlock;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public PlotClearEvent(TownBlock _townBlock) {
        this.townBlock = _townBlock;
    }

    /**
     *
     * @return the new TownBlock.
     */
    public TownBlock getTownBlock() {
        return townBlock;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = false;
    }
}