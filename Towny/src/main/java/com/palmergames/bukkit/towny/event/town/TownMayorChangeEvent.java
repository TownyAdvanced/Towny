package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.command.CommandSender;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * A Cancellable Event that gets fired when a town's mayor changes by a player
 * using an in-game command.
 */
public class TownMayorChangeEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final CommandSender sender;
	private final Resident oldMayor;
    private final Resident newMayor;

    public TownMayorChangeEvent(CommandSender sender, Resident oldMayor, Resident newMayor) {
    	this.sender = sender;
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

	/**
	 * @return the CommandSender that ran the /t set mayor command.
	 */
	public CommandSender getCommandSender() {
		return sender;
	}
}
