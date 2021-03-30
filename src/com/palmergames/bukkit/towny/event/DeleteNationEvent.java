package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DeleteNationEvent extends TownyObjDeleteEvent  {

    private static final HandlerList handlers = new HandlerList();

    @Override
    public HandlerList getHandlers() {
    	
        return handlers;
    }
    
    public static HandlerList getHandlerList() {

		return handlers;
	}

    private final UUID kingUUID;
    
    public DeleteNationEvent(Nation nation, UUID uuid) {
        super(nation.getName(), nation.getUUID(), nation.getRegistered());
        kingUUID = uuid;
    }

    /**
     *
     * @return the deleted nation name.
     */
    public String getNationName() {
        return name;
    }

	/**
	 * @return the deleted nation uuid.
	 */
	public UUID getNationUUID() {
    	return uuid;
    }

	/**
	 * @return deleted nation time of creation (in ms).
	 */
	public long getNationCreated() {
    	return registered;
	}
	
	/**
	 * @return deleted nation king uuid.
	 */
	@Nullable
	public UUID getNationKing() {
		return kingUUID;
	}
}