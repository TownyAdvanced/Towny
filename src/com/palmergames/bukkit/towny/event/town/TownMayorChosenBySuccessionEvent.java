package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Event that gets fired when a town has had their mayor removed by Towny,
 * resulting in Towny choosing a Town resident to become mayor.
 * 
 * Towny has chosen the newMayor out of the potentialResidents, but you can
 * override who will become the new mayor using this event.
 */
public class TownMayorChosenBySuccessionEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Resident oldMayor;
	private Resident newMayor;
	private final List<Resident> potentialResidents = new ArrayList<>();

	public TownMayorChosenBySuccessionEvent(Resident oldMayor, Resident newMayor, List<Resident> potentialResidents) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.oldMayor = oldMayor;
		this.newMayor = newMayor;
		for (Resident resident : potentialResidents) {
			if (resident != oldMayor)
				this.potentialResidents.add(resident);
		}

	}

	public Resident getOldMayor() {
		return oldMayor;
	}

	public Resident getNewMayor() {
		return newMayor;
	}

	public void setNewMayor(Resident replacementMayor) {
		newMayor = replacementMayor;
	}

	public Town getTown() {
		return TownyAPI.getInstance().getResidentTownOrNull(newMayor);
	}

	@Nullable
	public List<Resident> getPotentialMayors() {
		return potentialResidents;
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
