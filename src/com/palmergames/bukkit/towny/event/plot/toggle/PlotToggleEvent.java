package com.palmergames.bukkit.towny.event.plot.toggle;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public abstract class PlotToggleEvent extends Event implements Cancellable {

	private static final HandlerList handlers = new HandlerList();
	private final TownBlock townBlock;
	private final Town town;
	private final boolean futureState;
	private final Player player;
	private boolean isCancelled = false;
	private String cancellationMsg = Translation.of("msg_err_command_disable");

	public PlotToggleEvent(TownBlock townBlock, Player player, boolean futureState) {
		this.townBlock = townBlock;
		this.town = townBlock.getTownOrNull();
		this.player = player;
		this.futureState = futureState;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean isCancelled) {
		this.isCancelled = isCancelled;
	}
	
	public String getCancellationMsg() {
		return cancellationMsg;
	}

	public void setCancellationMsg(String cancellationMsg) {
		this.cancellationMsg = cancellationMsg;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
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
	
}
