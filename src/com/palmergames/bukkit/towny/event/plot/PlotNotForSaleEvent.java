package com.palmergames.bukkit.towny.event.plot;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


/**
 * Created by Glare
 * Date: 12/15/2020
 * Time: 6:48 AM
 */
public class PlotNotForSaleEvent extends Event {
	private static final HandlerList HANDLERS = new HandlerList();
	private final Resident resident;
	private final TownBlock townBlock;

	public PlotNotForSaleEvent(Resident resident, TownBlock townBlock) {
		this.resident = resident;
		this.townBlock = townBlock;
	}

	public HandlerList getHandlers() {
		return HANDLERS;
	}

	public static HandlerList getHandlerList() {
		return HANDLERS;
	}

	@NotNull
	public TownBlock getTownBlock() {
		return townBlock;
	}

	@NotNull
	public Resident getResident() {
		return resident;
	}

	@NotNull
	public Player getPlayer() {
		return resident.getPlayer();
	}

	@Nullable
	public Town getTown() {
		return TownyAPI.getInstance().getTownOrNull(townBlock);
	}
}
