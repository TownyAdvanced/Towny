package com.palmergames.bukkit.towny.event.town;

import java.util.UUID;

import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownMergeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();
    
    private final Town remainingTown;
    private final String succumbingTownName;
    private final UUID succumbingTownUUID;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
		return handlers;
	}

    public TownMergeEvent(Town remainingTown, String succumbingTownName, UUID succumbingTownUUID) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.remainingTown = remainingTown;
        this.succumbingTownName = succumbingTownName;
        this.succumbingTownUUID = succumbingTownUUID;
    }
    
    public String getSuccumbingTownName() {
        return succumbingTownName;
    }

    public UUID getSuccumbingTownUUID() {
        return succumbingTownUUID;
    }

    public Town getRemainingTown() {
        return remainingTown;
    }
}
