package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.ApiStatus;

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
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final Nation nation;
	private int nationLevelNumber;
	private final int modifier;

	@ApiStatus.Internal
	public NationCalculateNationLevelNumberEvent(Nation nation, int predeterminedLevelNumber, int modifier) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.nationLevelNumber = predeterminedLevelNumber;
		this.modifier = modifier;
	}

	public Nation getNation() {
		return nation;
	}

	@Deprecated
	public void setNationlevelNumber(int value) {
		this.nationLevelNumber = value;
	}

	public void setNationLevelNumber(int number) {
		this.nationLevelNumber = number;
	}

	public int getNationLevelNumber() {
		return nationLevelNumber;
	}

	/**
	 * {@return the modifier which is being used to calculate the nation level, typically the resident count}
	 */
	public int getModifier() {
		return modifier;
	}

	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}
}
