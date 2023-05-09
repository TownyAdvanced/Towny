package com.palmergames.bukkit.towny.event.town;

import org.bukkit.entity.Player;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownPreSetHomeBlockEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Town town;
	private final TownBlock townBlock;
	private final Player player;
	
	public TownPreSetHomeBlockEvent(Town town, TownBlock townBlock, Player player) {
		this.town = town;
		this.townBlock = townBlock;
		this.player = player;
		setCancelMessage(Translation.of("msg_err_homeblock_has_not_been_set"));
	}

	/**
	 * 
	 * @return Town which is about to set their homeblock.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * 
	 * @return TownBlock which will become the homeblock.
	 */
	public TownBlock getTownBlock() {
		return townBlock;
	}

	/**
	 * 
	 * @return Player which is setting the town's homeblock.
	 */
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
