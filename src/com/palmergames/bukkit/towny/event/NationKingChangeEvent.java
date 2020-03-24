package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class NationKingChangeEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private Nation nation;
	private Resident king;
	
	public NationKingChangeEvent(Nation nation, Resident newMayor) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.nation = nation;
		this.king = newMayor;
	}


	public static HandlerList getHandlerList() {
		return handlers;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public Nation getNation() {
		return nation;
	}

	public Resident getNewKing() {
		return king;
	}
}
