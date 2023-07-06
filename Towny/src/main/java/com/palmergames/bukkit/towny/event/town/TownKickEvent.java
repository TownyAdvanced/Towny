package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event that gets fired when a resident gets kicked from a town.
 */
public class TownKickEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Resident kickedResident;
    private final Object kicker;

    public TownKickEvent(Resident kickedResident, Object kicker) {
        this.kickedResident = kickedResident;
        this.kicker = kicker;
        setCancelMessage(Translation.of("msg_err_command_disable"));
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
