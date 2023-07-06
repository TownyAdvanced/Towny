package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

public class TranslationLoadEvent extends Event {
	private static final HandlerList handlers = new HandlerList();
	private final Map<String, Map<String, String>> addedTranslations;

	public TranslationLoadEvent() {
		super(!Bukkit.isPrimaryThread());
		this.addedTranslations = new HashMap<>();
	}
	
	public void addTranslation(@NotNull String locale, @NotNull String key, @NotNull String value) {
		addedTranslations.computeIfAbsent(locale, k -> new HashMap<>());
		
		addedTranslations.get(locale).put(key, value);
	}
	
	public Map<String, Map<String, String>> getAddedTranslations() {
		return addedTranslations;
	}
	
	@Override
	public @NotNull HandlerList getHandlers() {
		return handlers;
	}
	
	public static HandlerList getHandlerList() {
		return handlers;
	}
}
