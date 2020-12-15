package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotChangeOwnerEvent extends Event {
    public static final HandlerList handlers = new HandlerList();
    private final Resident oldowner;
    private final Resident newowner;
    private final TownBlock townBlock;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
	 * Changes the owner of a Plot
	 * 
     * @param oldowner - Old Owner (Resident)
     * @param newowner - New Owner (Resident)
	 * @param townBlock - Plot to change ownership of.
     */
    public PlotChangeOwnerEvent(Resident oldowner, Resident newowner, TownBlock townBlock) {
        super(!Bukkit.getServer().isPrimaryThread());
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
