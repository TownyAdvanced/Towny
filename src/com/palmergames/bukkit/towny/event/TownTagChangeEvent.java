package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownTagChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private Town town;
    private String newTag;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public TownTagChangeEvent(String newTag, Town town) {
        this.newTag = newTag;
        this.town = town;
    }

    public String getNewTag() {
        return newTag;
    }

    public Town getTown() {
        return town;
    }
}
