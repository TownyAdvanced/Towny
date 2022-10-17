package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;

/**
 * An event fired when a '/town unclaim [args]' command is issued, prior to any
 * other calculations.
 * <p>
 * Useful for plugins (like war systems) wanting halt the command in it's
 * tracks. For an example, see Flag War's
 * FlagWarCustomListener.onWarPreUnclaim().
 * <p>
 * Not to be confused with {@link TownPreUnclaimEvent}, which is handled within
 * the {@link com.palmergames.bukkit.towny.db.TownyDatabaseHandler}.
 */
public class TownPreUnclaimCmdEvent extends CancellableTownyEvent {

	private final Town town;
	private final Resident resident;
	private final TownyWorld townyWorld;

	/**
	 * Constructs the TownPreUnclaimCmdEvent and stores data an external war plugin
	 * may use.
	 *
	 * @param town     The {@link Town} about to process un-claiming (a) plot(s).
	 * @param resident The {@link Resident} who initiated the command.
	 * @param world    The {@link TownyWorld} in which the resident is in.
	 */
	public TownPreUnclaimCmdEvent(Town town, Resident resident, TownyWorld world) {
		this.town = town;
		this.resident = resident;
		this.townyWorld = world;
		setCancelMessage(Translation.of("msg_err_town_unclaim_canceled"));
	}

	/**
	 * @return Gets the {@link Town} which would have it's TownBlocks unclaimed.
	 */
	public Town getTown() {
		return town;
	}

	/**
	 * @return Gets the {@link Resident} that issued the '/t unclaim ...' command.
	 */
	public Resident getResident() {
		return resident;
	}

	/**
	 * @return Gets the {@link TownyWorld} where the land is being unclaimed.
	 */
	public TownyWorld getTownyWorld() {
		return townyWorld;
	}
}
