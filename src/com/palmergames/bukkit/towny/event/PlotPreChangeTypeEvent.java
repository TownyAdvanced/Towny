package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PlotPreChangeTypeEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final TownBlockType newType;
    private final TownBlock townBlock;
	private final Resident resident;

    /**
	 * Changes a plot's TownBlockType
     * @param newType - New Type
	 * @param townBlock - Plot to target
	 * @param resident - The resident who led to this event   
     */
    public PlotPreChangeTypeEvent(TownBlockType newType, TownBlock townBlock, Resident resident) {
        this.newType = newType;
        this.townBlock = townBlock;
        this.resident = resident;
    }

    public TownBlockType getNewType() {
        return newType;
    }

    public TownBlockType getOldType() {
        return townBlock.getType();
    }

    public TownBlock getTownBlock() {
        return townBlock;
    }

	public Resident getResident() {
		return resident;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
