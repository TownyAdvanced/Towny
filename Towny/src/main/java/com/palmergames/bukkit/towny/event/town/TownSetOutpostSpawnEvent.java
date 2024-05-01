package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Fired when a player uses /town set outpost
 * 
 * @since 0.100.2.7
 */
public class TownSetOutpostSpawnEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Town town;
	private final Player player;
	private Location newSpawn;

	public TownSetOutpostSpawnEvent(Town town, Player player, Location newSpawn) {
		this.town = town;
		this.player = player;
		this.newSpawn = newSpawn;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	/**
	 * @return The town for which this outpost spawn is being set.
	 */
	@NotNull
	public Town getTown() {
		return town;
	}

	/**
	 * @return The player that ran the command
	 */
	@NotNull
	public Player getPlayer() {
		return player;
	}

	/**
	 * @return The TownBlock where the outpost is located.
	 */
	public TownBlock getTownBlock() {
		return TownyAPI.getInstance().getTownBlock(player);
	}

	/**
	 * @return The new outpost spawn location.
	 */
	@NotNull
	public Location getNewSpawn() {
		return newSpawn;
	}

	public void setNewSpawn(@NotNull Location newSpawn) {
		this.newSpawn = newSpawn;
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
