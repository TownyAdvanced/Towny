package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

public class TownBlockTypeRegisterEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	
	public TownBlockTypeRegisterEvent() {
		super(!Bukkit.isPrimaryThread());
	}

	/**
	 * Registers a new type.
	 * @param type - The type
	 * @throws TownyException - If a type with this name is already registered.
	 */
	public static void registerType(@NotNull TownBlockType type) throws TownyException {
		TownBlockTypeHandler.registerType(type);
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
