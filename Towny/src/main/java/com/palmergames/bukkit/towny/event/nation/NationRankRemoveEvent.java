package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class NationRankRemoveEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Nation nation;
	private final Resident res;
	private final String rank;

	public NationRankRemoveEvent(Nation nation, String rank, Resident res) {
		this.nation = nation;
		this.rank = rank;
		this.res = res;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	public Nation getNation() {
		return nation;
	}

	public Resident getResident() {
		return res;
	}

	public String getRank() {
		return rank;
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
