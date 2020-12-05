package com.palmergames.bukkit.towny.event.nation.toggle;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;

public abstract class NationToggleEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final Player player;
	private final Nation nation;
	
	public NationToggleEvent(Player player, Nation nation) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.player = player;
		this.nation = nation;
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
	
	public Player getPlayer() {
		return player;
	}

	public Nation getNation() {
		return nation;
	}

}
