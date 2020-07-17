package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class TownTagChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private final Town town;
    private final String newTag;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public TownTagChangeEvent(String newTag, Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
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
