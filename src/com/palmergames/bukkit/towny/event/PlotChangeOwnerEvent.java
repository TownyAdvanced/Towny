package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotChangeOwnerEvent extends Event {
    public static final HandlerList handlers = new HandlerList();
    private Resident oldowner;
    private Resident newowner;
    private TownBlock townBlock;

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
    public PlotChangeOwnerEvent(Resident oldowner, Resident newowner, TownBlock townBlock) {
        this.newowner = newowner;
        this.oldowner = oldowner;
        this.townBlock = townBlock;
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

    public TownBlock getTownBlock() {
        return townBlock;
    }
}
