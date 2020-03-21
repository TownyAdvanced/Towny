package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationTagChangeEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

    private String newTag;
    private Nation nation;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public NationTagChangeEvent(Nation nation, String newTag) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.nation = nation;
        this.newTag = newTag;
    }

    public String getNewTag() {
        return newTag;
    }

    public Nation getNation() {
        return nation;
    }
}
