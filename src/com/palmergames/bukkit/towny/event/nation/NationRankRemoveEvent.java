package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translation;

public class NationRankRemoveEvent extends CancellableTownyEvent {

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
}
