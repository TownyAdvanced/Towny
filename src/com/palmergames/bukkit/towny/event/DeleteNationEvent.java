package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public class DeleteNationEvent extends TownyObjDeleteEvent  {
    private static final HandlerList HANDLER_LIST = new HandlerList();

    private final Resident king;

    public DeleteNationEvent(Nation nation, Resident king) {
        super(nation.getName(), nation.getUUID(), nation.getRegistered());
        this.king = king;
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
	 * @return deleted nation king's uuid.
	 */
	@Nullable
	public UUID getNationKing() {
		if (this.king == null)
			return null;
		
		return this.king.getUUID();
	}

	/**
	 * @return The deleted nation's king.
	 */
	@Nullable
	public Resident getKing() {
		return this.king;
	}
	
	@Override
	public @NotNull HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}