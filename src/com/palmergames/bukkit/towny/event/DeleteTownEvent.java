package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;

import java.util.UUID;


public class DeleteTownEvent extends TownyObjDeleteEvent  {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    private final UUID mayorUUID;
    
    public DeleteTownEvent(Town town, UUID uuid) {
    	super(town.getName(), town.getUUID(), town.getRegistered());
    	mayorUUID = uuid;
    }

    /**
     *
     * @return the deleted town name.
     */
    public String getTownName() {
        return name;
    }

	/**
	 * 
	 * @return the deleted town uuid.
	 */
	public UUID getTownUUID() {
    	return uuid;
	}

	/**
	 * 
	 * @return the deleted town's time of creation (in ms).
	 */
	public long getTownCreated() {
    	return registered;
	}

	/**
	 * @return the deleted town's mayor's UUID.
	 */
	public UUID getMayorUUID() {
		return mayorUUID;
	}
    
}