package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that gets fired when a town's mayor changes.
 */
public class TownMayorChangeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Resident oldMayor;
    private final Resident newMayor;
    private boolean cancelled = false;
    private String cancelMessage = Translation.of("msg_err_command_disable");

    public TownMayorChangeEvent(Resident oldMayor, Resident newMayor) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.oldMayor = oldMayor;
        this.newMayor = newMayor;
    }

    public Resident getOldMayor() {
        return oldMayor;
    }

    public Resident getNewMayor() {
        return newMayor;
    }

    public Town getTown() {
        return TownyAPI.getInstance().getResidentTownOrNull(newMayor);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getCancelMessage() {
        return cancelMessage;
    }

    public void setCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

    public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
