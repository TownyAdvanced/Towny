package com.palmergames.bukkit.config.migration;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;

@SuppressWarnings("FieldCanBeLocal")
public class RunnableMigrations {
	private final Map<String, Consumer<CommentedConfiguration>> BY_NAME = new HashMap<>();
	
	public RunnableMigrations() {
		BY_NAME.put("migrate_notifications", MIGRATE_NOTIFICATIONS);
		BY_NAME.put("add_townblocktype_limits", ADD_TOWNBLOCKTYPE_LIMITS);
		BY_NAME.put("convert_entity_class_names", CONVERT_ENTITY_CLASS_NAMES);
	}
	
	@Nullable
	public Consumer<CommentedConfiguration> getByName(String name) {
		return BY_NAME.get(name.toLowerCase(Locale.ROOT));
	}
	
	public boolean addMigration(String name, Consumer<CommentedConfiguration> migration) {
		return BY_NAME.putIfAbsent(name.toLowerCase(Locale.ROOT), migration) == null;
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
	
	private final Consumer<CommentedConfiguration> CONVERT_ENTITY_CLASS_NAMES = config -> {
		List<String> entities = new ArrayList<>(Arrays.asList(config.getString("new_world_settings.plot_management.wild_revert_on_mob_explosion.entities", "").split(",")));

		ListIterator<String> iterator = entities.listIterator();
		while (iterator.hasNext()) {
			String entity = iterator.next();
			
			// The old config default had a Fireball class which was never a valid class, but it is a valid key for the registry, so only remove it if LargeFireball is also present.
			if (entity.equals("Fireball") && entities.contains("LargeFireball")) {
				iterator.remove();
				continue;
			}

			for (EntityType type : Registry.ENTITY_TYPE) {
				if (type.getEntityClass() != null && type.getEntityClass().getSimpleName().equalsIgnoreCase(entity)) {
					iterator.set(BukkitTools.keyAsString(type.getKey()));
					break;
				}
			}
		}
		
		config.set("new_world_settings.plot_management.wild_revert_on_mob_explosion.entities", String.join(",", entities));
	};
}
