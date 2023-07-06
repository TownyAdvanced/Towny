package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownPreMergeEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
