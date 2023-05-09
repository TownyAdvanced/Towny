package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import java.util.UUID;

abstract class TownyObjDeleteEvent extends Event {
	
	protected final String name;
	protected final UUID uuid;
	protected final long registered;
	
	TownyObjDeleteEvent(String name, UUID uuid, long registered) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.name = name;
		this.uuid = uuid;
		this.registered = registered;
	}
}
