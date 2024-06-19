package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownPreReclaimEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Player player;
	private final Resident resident;
	private final Town town;

	/**
	 * Event thrown prior to a {@link Town} being reclaimed by a {@link Resident}.
	 *
	 * @param town     The Town being reclaimed.
	 * @param resident The resident who would become mayor.
	 * @param player   The Player who would become mayor.
	 */
	public TownPreReclaimEvent(Town town, Resident resident, Player player) {
		this.town = town;
		this.resident = resident;
		this.player = player;
	}

	/**
	 * @return the {@link Town} which will be reclaimed.
	 */
	@Nullable
	public Town getTown() {
		return town;
	}

	/**
	 * @return the resident who will become mayor.
	 */
	public Resident getResident() {
		return resident;
	}

	/**
	 * @return the player who will become mayor.
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