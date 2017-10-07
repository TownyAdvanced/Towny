package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotChangeTypeEvent extends Event {
    public static final HandlerList handlers = new HandlerList();
    private TownBlockType oldType;
    private TownBlockType newType;

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
    public PlotChangeTypeEvent(TownBlockType oldType, TownBlockType newType) {
        this.newType = newType;
        this.oldType = oldType;
    }

    public TownBlockType getNewType() {
        return newType;
    }

    public TownBlockType getOldType() {
        if (oldType == null) {
            return TownBlockType.WILDS; // Considering the further fact we know null is wilderness if there is no old type, it has to have been wilderness.
        }
        return oldType;
    }
}
