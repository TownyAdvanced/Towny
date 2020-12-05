package com.palmergames.bukkit.towny.event.town.toggle;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public abstract class TownToggleEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final Town town;
	
	/**
	 * A generic cancellable event thrown when a player uses the /town toggle {args} command.
	 * 
	 * @param player Player who has run the command.
	 * @param town Town which will have something cancelled.
	 */
	public TownToggleEvent(Player player, Town town) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.town = town;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Nullable
	public Resident getResident() {
		try {
			return TownyUniverse.getInstance().getDataSource().getResident(player.getName());
		} catch (NotRegisteredException ignored) {}
		return null;
	}
	
	@Nullable
	public Player getPlayer() {
		return player;
	}

	public Town getTown() {
		return town;
	}

}
