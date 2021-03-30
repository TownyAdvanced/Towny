package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;


public class NationMergeEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private final Nation nation;
    private final Nation remainingnation;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
		return handlers;
	}

    public NationMergeEvent(Nation nation, Nation remainingnation) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.remainingnation = remainingnation;
        this.nation = nation;
    }
    
    public Nation getNation() {
        return nation;
    }

	public Nation getRemainingnation() {
		return remainingnation;
	}
}
