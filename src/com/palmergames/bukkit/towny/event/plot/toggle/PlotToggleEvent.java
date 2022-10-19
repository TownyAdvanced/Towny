package com.palmergames.bukkit.towny.event.plot.toggle;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.entity.Player;

public abstract class PlotToggleEvent extends CancellableTownyEvent {

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

	/**
	 * @deprecated since 0.98.4.0, use {@link #getCancelMessage()}
	 */
	@Deprecated 
	public String getCancellationMsg() {
		return getCancelMessage();
	}

	/**
	 * @deprecated since 0.98.4.0, use {@link #setCancelMessage(String)}
	 * @param cancellationMsg
	 */
	@Deprecated
	public void setCancellationMsg(String cancellationMsg) {
		setCancelMessage(cancellationMsg);
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
