package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * An event that is thrown which allows other plugins to alter a Nation's
 * NationLevel Number. ie: 1 to {@link TownySettings#getNationLevelMax()}. This
 * is used as a key to determine which NationLevel a Nation receives, and
 * ultimately which attributes that Nation will receive.
 * 
 * @author LlmDl
 * @since 0.99.6.2
 */
public class NationCalculateNationLevelNumberEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private int nationLevelNumber;
	private final Nation nation;

	/**
	 * An event that is thrown which allows other plugins to alter a Nation's
	 * NationLevel Number. ie: 1 to {@link TownySettings#getNationLevelMax()}. This
	 * is used as a key to determine which NationLevel a Nation receives, and
	 * ultimately which attributes that Nation will receive.
	 * 
	 * @param nation                   Nation which is having their NationLevel
	 *                                 number calculated.
	 * @param predeterminedLevelNumber The number which Towny has already assigned
	 *                                 to the nation.
	 */
	public NationCalculateNationLevelNumberEvent(Nation nation, int predeterminedLevelNumber) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.nationLevelNumber = predeterminedLevelNumber;
	}

	public Nation getNation() {
		return nation;
	}

	public void setNationlevelNumber(int value) {
		this.nationLevelNumber = value;
	}

	public int getNationLevelNumber() {
		return nationLevelNumber;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
