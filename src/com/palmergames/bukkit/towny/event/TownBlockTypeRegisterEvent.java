package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.TownBlockData;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TownBlockTypeRegisterEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	
	public TownBlockTypeRegisterEvent() {
		super(!Bukkit.isPrimaryThread());
	}

	/**
	 * Registers a new type.
	 * @param name - The name for this type.
	 * @param data - The data for this type.
	 * @throws TownyException - If a type with this name is already registered.
	 */
	public static void registerType(@NotNull String name, @Nullable TownBlockData data) throws TownyException {
		TownBlockTypeHandler.registerType(name, data);
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
