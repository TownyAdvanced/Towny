package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author Artuto
 *
 * Fired before a Town is deleted.
 */
public class TownDeleteEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private Town town;

    public TownDeleteEvent(Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = town;
    }

    /**
     *
     * @return the town that is going to be deleted
     * */
    public Town getTown()
    {
        return town;
    }

    @Override
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }
}
