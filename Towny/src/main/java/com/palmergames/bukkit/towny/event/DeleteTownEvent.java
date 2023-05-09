package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

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
	private final Resident mayor;
    
    public DeleteTownEvent(Town town, @Nullable Resident mayor) {
    	super(town.getName(), town.getUUID(), town.getRegistered());

		this.mayor = mayor;
    	this.mayorUUID = mayor == null ? null : mayor.getUUID();
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
	 * @return the deleted town's mayor's UUID, or {@code null}.
	 */
	@Nullable
	public UUID getMayorUUID() {
		return mayorUUID;
	}

	/**
	 * @return The deleted town's mayor, or {@code null}.
	 */
	@Nullable
	public Resident getMayor() {
		return mayor;
	}
}