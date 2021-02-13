package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownMergeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    private final Town town;
    private final Town remainingTown;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
		return handlers;
	}

    public TownMergeEvent(Town town, Town remainingTown) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.remainingTown = remainingTown;
        this.town = town;
    }
    
    public Town getTown() {
        return town;
    }

    public Town getRemainingTown() {
        return remainingTown;
    }
}
