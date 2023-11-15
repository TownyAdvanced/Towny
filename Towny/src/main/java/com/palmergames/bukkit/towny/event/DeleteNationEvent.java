package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

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
    private final Resident king;

    public DeleteNationEvent(Nation nation, Resident king) {
        super(nation.getName(), nation.getUUID(), nation.getRegistered());
        
        this.king = king;
        this.kingUUID = king == null ? null : king.getUUID();
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
	 * @return the deleted nation's leader's UUID, or {@code null}.
	 */
	@Nullable
	public UUID getLeaderUUID() {
		return kingUUID;
	}

	/**
	 * @return The deleted nation's leader's Resident object, or {@code null}.
	 */
	@Nullable
	public Resident getLeader() {
		return king;
	}

}