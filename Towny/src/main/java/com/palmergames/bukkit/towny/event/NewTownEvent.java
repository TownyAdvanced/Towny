package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is no longer called.
 * @deprecated since 0.99.6.4 use {@link com.palmergames.bukkit.towny.event.town.NewTownEvent} instead.
 */
@Deprecated
@Warning(reason = "Event is no longer called. Event has been moved to the com.palmergames.bukkit.towny.event.town package.")
public class NewTownEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private final Town town;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public NewTownEvent(Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = town;
    }

    /**
     *
     * @return the new town.
     */
    public Town getTown() {
        return town;
    }
    
}