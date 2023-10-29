package com.palmergames.bukkit.towny.event.plot.toggle;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class PlotToggleEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final TownBlock townBlock;
	private final Town town;
	private final boolean futureState;
	private final Player player;

	public PlotToggleEvent(TownBlock townBlock, Player player, boolean futureState) {
		this.townBlock = townBlock;
		this.town = townBlock.getTownOrNull();
		this.player = player;
		this.futureState = futureState;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public Town getTown() {
		return town;
	}

	public boolean getFutureState() {
		return futureState;
	}

	public Player getPlayer() {
		return player;
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
