package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

public class TownPreMergeEvent extends CancellableTownyEvent {
	private final Town remainingTown;
	private final Town succumbingTown;

	public TownPreMergeEvent(Town remainingTown, Town succumbingTown) {
		this.remainingTown = remainingTown;
		this.succumbingTown = succumbingTown;
		setCancelMessage(Translation.of("msg_town_merge_cancelled"));
	}

	public Town getRemainingTown() {
		return remainingTown;
	}

	public Town getSuccumbingTown() {
		return succumbingTown;
	}
}
