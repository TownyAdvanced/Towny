package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Event that gets fired when a nation's king changes.
 */
public class NationKingChangeEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Resident oldKing;
    private final Resident newKing;
    private boolean cancelled = false;
    private String cancelMessage = Translation.of("msg_err_command_disable");

    public NationKingChangeEvent(Resident oldKing, Resident newKing) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.oldKing = oldKing;
        this.newKing = newKing;
    }

    public Resident getOldKing() {
        return oldKing;
    }

    public Resident getNewKing() {
        return newKing;
    }

    public Nation getNation() {
        return newKing.getNationOrNull();
    }

    public boolean isCapitalChange() {
        return !TownyAPI.getInstance().getResidentTownOrNull(oldKing).hasResident(newKing);
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
