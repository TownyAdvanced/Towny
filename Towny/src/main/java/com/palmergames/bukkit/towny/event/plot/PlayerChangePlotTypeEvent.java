package com.palmergames.bukkit.towny.event.plot;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;

public class PlayerChangePlotTypeEvent extends Event {
	private static final HandlerList HANDLER_LIST = new HandlerList();

	private final TownBlockType newType;
	private final TownBlockType oldType;
	private final TownBlock townBlock;
	private final Player player;

	/**
	 * Thrown when a player has changed the plot type of a TownBlock successfully.
	 * 
	 * @param newType   New TownBlockType
	 * @param oldType   Previous TownBlockType
	 * @param townBlock TownBlock which had a change of TownBlockType.
	 * @param player    The player who changed a plot type.
	 */
	public PlayerChangePlotTypeEvent(TownBlockType newType, TownBlockType oldType, TownBlock townBlock, Player player) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.newType = newType;
		this.oldType = oldType;
		this.townBlock = townBlock;
		this.player = player;
	}

	public TownBlockType getNewType() {
		return newType;
	}

	public TownBlockType getOldType() {
		return oldType;
	}

	public TownBlock getTownBlock() {
		return townBlock;
	}

	public Player getPlayer() {
		return player;
	}
	
	public Resident getResident() {
		return TownyAPI.getInstance().getResident(player);
	}

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
