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
     * @deprecated in favor of {@link #getKingUUID()} 
	 * @return deleted nation king uuid.
	 */
	@Nullable
	public UUID getNationKing() {
		return kingUUID;
	}

    /**
	 * @return the deleted nation's king's UUID, or {@code null}.
	 */
	@Nullable
	public UUID getKingUUID() {
		return kingUUID;
	}

	/**
	 * @return The deleted nation's king, or {@code null}.
	 */
	@Nullable
	public Resident getKing() {
		return king;
	}
}