package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class PlotPreChangeTypeEvent extends Event implements Cancellable {
    public static final HandlerList handlers = new HandlerList();
    private TownBlockType newType;
    private TownBlock townBlock;
	private String cancelMessage = "Sorry this event was cancelled";
	private boolean isCancelled = false;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    /**
	 * Changes a plot's TownBlockType
     * @param newType - New Type
	 * @param townBlock - Plot to target
     */
    public PlotPreChangeTypeEvent(TownBlockType newType, TownBlock townBlock) {
    	super(!Bukkit.getServer().isPrimaryThread());
        this.newType = newType;
        this.townBlock = townBlock;
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

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean cancelled) {
		this.isCancelled = cancelled;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
