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
 * Event that gets fired when a resident gets kicked from a town.
 */
public class TownKickEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Resident kickedResident;
    private final Object kicker;
    private boolean cancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");

    public TownKickEvent(Resident kickedResident, Object kicker) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.kickedResident = kickedResident;
        this.kicker = kicker;
    }

    public Resident getKickedResident() {
        return kickedResident;
    }

    public Town getTown() {
        return TownyAPI.getInstance().getResidentTownOrNull(kickedResident);
    }

    /**
     * Gets whoever kicked the resident. Can either be an instance of Player or CommandSender.
     * @return The kicker.
     */
    public Object getKicker() {
        return kicker;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

    public String getCancelMessage() {
        return cancelMessage;
    }
    
    public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}
}
