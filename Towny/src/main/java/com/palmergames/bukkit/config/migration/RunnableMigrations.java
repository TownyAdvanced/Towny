package com.palmergames.bukkit.config.migration;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;

import org.bukkit.Registry;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Locale;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.logging.Level;

@SuppressWarnings({"unused", "unchecked"})
public class RunnableMigrations {
	private final Map<String, Consumer<CommentedConfiguration>> BY_NAME = new HashMap<>();
	
	public RunnableMigrations() {
		try {
			for (final Field field : this.getClass().getDeclaredFields()) {
				if (!Modifier.isStatic(field.getModifiers()))
					continue;

				field.setAccessible(true);
				Object value = field.get(null);

				if (!(value instanceof Consumer))
					continue;
				
				BY_NAME.put(field.getName().toLowerCase(Locale.ROOT), (Consumer<CommentedConfiguration>) value);
			}
		} catch (ReflectiveOperationException e) {
			Towny.getPlugin().getLogger().log(Level.WARNING, "Exception occurred when getting runnable migrations", e);
		}
	}
	
	@Nullable
	public Consumer<CommentedConfiguration> getByName(String name) {
		return BY_NAME.get(name.toLowerCase(Locale.ROOT));
	}
	
	public boolean addMigration(String name, Consumer<CommentedConfiguration> migration) {
		return BY_NAME.putIfAbsent(name.toLowerCase(Locale.ROOT), migration) == null;
	}
	
	private static final Consumer<CommentedConfiguration> MIGRATE_NOTIFICATIONS = config -> {
		if (Boolean.parseBoolean(config.getString("notification.notifications_appear_in_action_bar", "true")))
			config.set("notification.notifications_appear_as", "action_bar");
		else if (Boolean.parseBoolean(config.getString("notification.notifications_appear_on_bossbar", "false")))
			config.set("notification.notifications_appear_as", "bossbar");
		else 
			config.set("notification.notifications_appear_as", "chat");
	};
	
	private static final Consumer<CommentedConfiguration> ADD_TOWNBLOCKTYPE_LIMITS = config -> {
		for (Map<?, ?> level : config.getMapList("levels.town_level"))
			((Map<String, Object>) level).put("townBlockTypeLimits", new HashMap<>());
	};
	
	private static final Consumer<CommentedConfiguration> CONVERT_ENTITY_CLASS_NAMES = config -> {
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
	
	private static final Consumer<CommentedConfiguration> ADD_MILKABLE_ANIMALS_TO_FARM_PLOT = config -> {
		for (Map<?, ?> plotType : config.getMapList("townblocktypes.types")) {
			if (plotType.get("name").equals("farm")) {
				String allowedBlocks = (String) plotType.get("allowedBlocks");
				((Map<String, Object>) plotType).replace("allowedBlocks", "COW_SPAWN_EGG,GOAT_SPAWN_EGG,MOOSHROOM_SPAWN_EGG," + allowedBlocks);
			}
		}
	};

	/**
	 * 0.100.2.10 included a change which revamped the ItemLists used to construct the farm blocks, resulting in a more comprehensive list.
	 * This runnable will add any blocks that older configs may have had which were missing from older configs.
	 */
	private static final Consumer<CommentedConfiguration> UPDATE_FARM_BLOCKS = config -> {
		for (Map<?, ?> plotType : config.getMapList("townblocktypes.types")) {
			if (!plotType.get("name").equals("farm"))
				continue;
			String rawBlocks = (String) plotType.get("allowedBlocks");
			List<String> currentBlocks = Arrays.asList(rawBlocks.split(","));
			List<String> missingBlocks = Arrays.asList(TownySettings.getDefaultFarmblocks().split(",")).stream()
				.filter(block -> !currentBlocks.contains(block))
				.collect(Collectors.toList());
			((Map<String, Object>) plotType).replace("allowedBlocks", rawBlocks + "," + StringMgmt.join(missingBlocks, ","));
		}
	};
	
	private static final Consumer<CommentedConfiguration> DISABLE_MODERN_ECO = config -> {
		config.set(ConfigNodes.ECO_ADVANCED_MODERN.getRoot(), "false");
	};

	/**
	 * Adds 1.21.4 Pale Oak items to farm blocks
	 */
	private static final Consumer<CommentedConfiguration> ADD_PALE_OAK_TO_FARM_PLOT = config -> {
		for (Map<?, ?> plotType : config.getMapList("townblocktypes.types")) {
			if (plotType.get("name").equals("farm")) {
				String allowedBlocks = (String) plotType.get("allowedBlocks");
				((Map<String, Object>) plotType).replace("allowedBlocks", "PALE_OAK_LOG,PALE_MOSS_BLOCK,PALE_MOSS_CARPET,PALE_OAK_SAPLING,PALE_HANGING_MOSS,PALE_OAK_LEAVES,CLOSED_EYEBLOSSOM,OPEN_EYEBLOSSOM," + allowedBlocks);
			}
		}
	};
}
