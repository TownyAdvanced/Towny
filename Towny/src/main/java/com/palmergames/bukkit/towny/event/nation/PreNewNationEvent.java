package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class PreNewNationEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final String nationName;

	public PreNewNationEvent(Town town, String nationName) {
		this.town = town;
		this.nationName = nationName;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	public Town getTown() {
		return town;
	}

	public String getNationName() {
		return nationName;
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
