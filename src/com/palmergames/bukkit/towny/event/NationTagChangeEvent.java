package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationTagChangeEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    public boolean cancelled = false;
    private String newTag;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public NationTagChangeEvent(String newTag) {
        this.newTag = newTag;
    }

    public String getNewTag() {
        return newTag;
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
