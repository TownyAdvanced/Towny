package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Event that gets fired when a town's mayor has changed.
 */
public class TownMayorChangedEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Resident oldMayor;
	private final Resident newMayor;

	public TownMayorChangedEvent(Resident oldMayor, Resident newMayor) {
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
