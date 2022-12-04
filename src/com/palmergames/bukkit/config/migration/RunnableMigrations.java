package com.palmergames.bukkit.config.migration;

import com.palmergames.bukkit.config.CommentedConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

public class RunnableMigrations {
	private final Map<String, Consumer<CommentedConfiguration>> BY_NAME = new HashMap<>();
	
	public RunnableMigrations() {
		BY_NAME.put("migrate_notifications", MIGRATE_NOTIFICATIONS);
		BY_NAME.put("add_townblocktype_limits", ADD_TOWNBLOCKTYPE_LIMITS);
	}
	
	@Nullable
	public Consumer<CommentedConfiguration> getByName(String name) {
		return BY_NAME.get(name.toLowerCase(Locale.ROOT));
	}
	
	public boolean addMigration(String name, Consumer<CommentedConfiguration> migration) {
		if (BY_NAME.containsKey(name.toLowerCase(Locale.ROOT)))
			return false;
		
		BY_NAME.put(name.toLowerCase(Locale.ROOT), migration);
		return true;
	}
	
	private final Consumer<CommentedConfiguration> MIGRATE_NOTIFICATIONS = config -> {
		if (Boolean.parseBoolean(config.getString("notification.notifications_appear_in_action_bar", "true")))
			config.set("notification.notifications_appear_as", "action_bar");
		else if (Boolean.parseBoolean(config.getString("notification.notifications_appear_on_bossbar", "false")))
			config.set("notification.notifications_appear_as", "bossbar");
		else 
			config.set("notification.notifications_appear_as", "chat");
	};
	
	@SuppressWarnings("unchecked")
	private final Consumer<CommentedConfiguration> ADD_TOWNBLOCKTYPE_LIMITS = config -> {
		for (Map<?, ?> level : config.getMapList("levels.town_level"))
			((Map<String, Object>) level).put("townBlockTypeLimits", new HashMap<>());
	};
}
