package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownLeaveEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final Resident resident;
	
	public TownLeaveEvent(Resident resident, Town town) {
		this.resident = resident;
		this.town = town;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	/**
	 * @return Town which is about to lose the resident.
	 */
	public Town getTown() {
		return town;
	}
	
	/**
	 * @return Resident which is about to leave their town.
	 */
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
