package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class NationAddTownEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private final Town town;
    private final Nation nation;

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    public NationAddTownEvent(Town town, Nation nation) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = town;
        this.nation = nation;
    }

    /**
     *
     * @return the town who has joined a nation.
     */
    public Town getTown() {
        return town;
    }

    /**
     *
     * @return the nation the town has just joined.
     */
    public Nation getNation() {
        return nation;
    }
    
}
