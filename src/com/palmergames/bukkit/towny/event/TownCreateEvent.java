package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * @author Artuto
 *
 * Fired after a new Town has been created.
 */
public class TownCreateEvent extends Event
{
    private static final HandlerList handlers = new HandlerList();

    private Town town;

    public TownCreateEvent(Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = town;
    }

    /**
     *
     * @return the new town
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
