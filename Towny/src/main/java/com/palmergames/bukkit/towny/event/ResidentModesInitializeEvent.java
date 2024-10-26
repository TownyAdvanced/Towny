package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.resident.mode.AbstractResidentMode;
import com.palmergames.bukkit.towny.object.resident.mode.ResidentModeHandler;

public class ResidentModesInitializeEvent extends Event {
	private static final HandlerList handlers = new HandlerList();

	public ResidentModesInitializeEvent() {
		super(!Bukkit.isPrimaryThread());
	}

	/**
	 * Registers a new ResidentMode.
	 * @param mode The ResidentMode you want to register.
	 * @throws TownyException - If a mode with this name is already registered.
	 */
	public void registerMode(@NotNull AbstractResidentMode mode) throws TownyException {
		ResidentModeHandler.registerMode(mode);
	}

	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}
}
