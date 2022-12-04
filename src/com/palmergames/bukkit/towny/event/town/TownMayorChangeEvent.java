package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event that gets fired when a town's mayor changes.
 */
public class TownMayorChangeEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Resident oldMayor;
    private final Resident newMayor;

    public TownMayorChangeEvent(Resident oldMayor, Resident newMayor) {
        this.oldMayor = oldMayor;
        this.newMayor = newMayor;
        setCancelMessage(Translation.of("msg_err_command_disable"));
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

    public boolean isNationCapital() {
        return getTown().isCapital();
    }

    public boolean isKingChange() {
        return oldMayor.isKing();
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
