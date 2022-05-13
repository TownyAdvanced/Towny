package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Alliance;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DeleteAllianceEvent extends TownyObjDeleteEvent  {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    private final UUID founderUUID;
    
    public DeleteAllianceEvent(Alliance alliance, UUID uuid) {
        super(alliance.getName(), alliance.getUUID(), alliance.getRegistered());
        founderUUID = uuid;
    }

    /**
     *
     * @return the deleted alliance name.
     */
    public String getAllianceName() {
        return name;
    }

	/**
	 * @return the deleted alliance uuid.
	 */
	public UUID getAllianceUUID() {
    	return uuid;
    }

	/**
	 * @return deleted alliance time of creation (in ms).
	 */
	public long getAllianceCreated() {
    	return registered;
	}
	
	/**
	 * @return deleted alliance's founding nation's uuid.
	 */
	@Nullable
	public UUID getFounderUUID() {
		return founderUUID;
	}
}