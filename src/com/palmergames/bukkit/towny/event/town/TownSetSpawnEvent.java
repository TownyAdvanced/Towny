package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Fired when a player uses /town set spawn
 */
public class TownSetSpawnEvent extends CancellableTownyEvent {
	private final Town town;
	private final Player player;
	private final Location oldSpawn;
	private Location newSpawn;

	public TownSetSpawnEvent(Town town, Player player, Location newSpawn) {
		this.town = town;
		this.player = player;
		this.oldSpawn = town.getSpawnOrNull();
		this.newSpawn = newSpawn;
		setCancelMessage(Translation.of("msg_err_command_disable"));
	}

	/**
	 * @return The town for which this spawn is being set.
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
	 * @return The town's old spawnpoint location.
	 */
	@Nullable
	public Location getOldSpawn() {
		return oldSpawn;
	}

	/**
	 * @return The new spawn location.
	 */
	@NotNull
	public Location getNewSpawn() {
		return newSpawn;
	}

	public void setNewSpawn(@NotNull Location newSpawn) {
		this.newSpawn = newSpawn;
	}
}
