package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotChangeOwnerEvent extends Event implements Cancellable{
    public static final HandlerList handlers = new HandlerList();
    private Resident oldowner;
    private Resident newowner;
    public boolean cancelled = false;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
     * @param oldowner - Old Owner
     * @param newowner - New Owner
     */
    public PlotChangeOwnerEvent(Resident oldowner, Resident newowner) {
        this.newowner = newowner;
        this.oldowner = oldowner;
    }

    public String getNewowner() {
        return newowner.getName();
    }

    public String getOldowner() {
        if (oldowner == null) {
            return "undefined";
        }
        return oldowner.getName();
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
