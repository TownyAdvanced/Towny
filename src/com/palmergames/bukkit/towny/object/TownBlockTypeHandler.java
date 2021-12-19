package com.palmergames.bukkit.towny.object;

import com.github.bsideup.jabel.Desugar;
import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.TownBlockTypeRegisterEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TownBlockTypeHandler {
	private static Map<String, TownBlockType> townBlockTypeMap = new ConcurrentHashMap<>();
	
	public static void initialize() {
		Map<String, TownBlockType> newData = new ConcurrentHashMap<>();
		
		for (Field field : TownBlockType.class.getFields()) {
			try {
				TownBlockType type = (TownBlockType) field.get(null);
				newData.put(type.getName().toLowerCase(), type);
			} catch (Exception ignored) {}
		}
		
		applyConfigSettings(newData);
		
		townBlockTypeMap = newData;
		
		Bukkit.getPluginManager().callEvent(new TownBlockTypeRegisterEvent());
		
		Towny.getPlugin().getLogger().info(String.format("Loaded %d townblock types: %s", townBlockTypeMap.size(), Arrays.toString(townBlockTypeMap.keySet().toArray())));
	}

	/**
	 * Registers a new type. Should not be used at all outside of the TownBlockTypeRegisterEvent.
	 * @param type - The type
	 * @throws TownyException - If a type with this name is already registered.
	 */
	public static void registerType(@NotNull TownBlockType type) throws TownyException {
		if (exists(type.getName()))
			throw new TownyException(String.format("A type named '%s' is already registered!", type.getName()));
		
		townBlockTypeMap.put(type.getName().toLowerCase(), type);
		Towny.getPlugin().getLogger().info(String.format("A new townblock type was registered: %s", type.getName()));
	}

	/**
	 * Gets the currently registered townblock types.
	 * @return An unmodifiable map containing the types.
	 */
	public static Map<String, TownBlockType> getTypes() {
		return Collections.unmodifiableMap(townBlockTypeMap);
	}
	
	public static List<String> getTypeNames() {
		return new ArrayList<>(townBlockTypeMap.keySet());
	}

	/**
	 * Gets the townblock instance for the name.
	 * @param townBlockType The name of the town block type.
	 * @return The townblocktype instance, or {@code null} if none is registered.   
	 */
	@Nullable
	public static TownBlockType getType(@NotNull String townBlockType) {
		return townBlockTypeMap.get(townBlockType.toLowerCase());
	}

	public static TownBlockType getTypeInternal(@NotNull String input) {
		try {
			int id = Integer.parseInt(input);
			return getType(TownBlockType.getLegacylookupmap().getOrDefault(id, "default"));
		} catch (NumberFormatException e) {
			return getType(input);
		}
	}

	/**
	 * @param typeName The name of the type to test for.
	 * @return Whether a type with the specified name exists.
	 */
	public static boolean exists(@NotNull String typeName) {
		return getType(typeName) != null;
	}
	
	private static void applyConfigSettings(Map<String, TownBlockType> newData) {

		List<Map<?, ?>> types = TownySettings.getConfig().getMapList("townblocktypes.types");
		for (Map<?, ?> type : types) {
			String name = "unknown type";
			
			try {
				name = String.valueOf(type.get("name"));
				double cost = parseDouble(type.get("cost").toString());
				double tax = parseDouble(type.get("tax").toString());
				String mapKey = String.valueOf(type.get("mapKey"));

				Set<Material> itemUseIds = loadMaterialList("itemUseIds", String.valueOf(type.get("itemUseIds")), name);
				Set<Material> switchIds = loadMaterialList("switchIds", String.valueOf(type.get("switchIds")), name);
				Set<Material> allowedBlocks = loadMaterialList("allowedBlocks", String.valueOf(type.get("allowedBlocks")), name);
				
				TownBlockType townBlockType = newData.get(name.toLowerCase());
				TownBlockData data;
				
				if (townBlockType == null) {
					data = new TownBlockData();
					townBlockType = new TownBlockType(name, data);
				} else
					data = townBlockType.getData();
				
				data.setCost(cost);
				data.setTax(tax);
				data.setMapKey(mapKey);
				data.setItemUseIds(itemUseIds);
				data.setSwitchIds(switchIds);
				data.setAllowedBlocks(allowedBlocks);
				
				newData.put(name.toLowerCase(), townBlockType);
				
			} catch (Exception e) {
				Towny.getPlugin().getLogger().warning(String.format("Error while loading townblock type '%s', skipping...", name));
				e.printStackTrace();
			}
		}
	}
	
	private static Set<Material> loadMaterialList(String listName, String materialList, String typeName) {
		if (!materialList.isEmpty()) {
			Set<Material> set = new LinkedHashSet<>();
			for (String materialName : materialList.split(",")) {
				Material material = Material.matchMaterial(materialName);

				if (material == null)
					TownyMessaging.sendDebugMsg(String.format("Could not find a material named '%s' while loading the " + listName + " list for the %s type.", materialName, typeName));
				else
					set.add(material);
			}
			
			return set;
		} else
			return new LinkedHashSet<>();
	}

	private static double parseDouble(String string) {
		try {
			return Double.parseDouble(string);
		} catch (NumberFormatException | NullPointerException e) {
			return 0.0D;
		}
	}
	
	private TownBlockTypeHandler() {}
	
	public static class Migrator {
		private static final Set<Migration> migrations = new HashSet<>();
		
		public static void checkForLegacyOptions() {
			Path configPath = Towny.getPlugin().getDataFolder().toPath().resolve("settings").resolve("config.yml");
			if (!Files.exists(configPath))
				return;
			
			CommentedConfiguration config = new CommentedConfiguration(configPath);
			if (!config.load() || config.contains(ConfigNodes.TOWNBLOCKTYPES_TYPES.getRoot()))
				return;
			
			double shopCost = parseDouble(config.getString("economy.plot_type_costs.set_commercial"));
			double arenaCost = parseDouble(config.getString("economy.plot_type_costs.set_arena"));
			double embassyCost = parseDouble(config.getString("economy.plot_type_costs.set_embassy"));
			double wildsCost = parseDouble(config.getString("economy.plot_type_costs.set_wilds"));
			double innCost = parseDouble(config.getString("economy.plot_type_costs.set_inn"));
			double jailCost = parseDouble(config.getString("economy.plot_type_costs.set_jail"));
			double farmCost = parseDouble(config.getString("economy.plot_type_costs.set_farm"));
			double bankCost = parseDouble(config.getString("economy.plot_type_costs.set_bank"));
			
			String farmPlotBlocks = config.getString("global_town_settings.farm_plot_allow_blocks", TownySettings.getDefaultFarmblocks());
			
			migrations.add(new Migration("shop", "cost", shopCost));
			migrations.add(new Migration("arena", "cost", arenaCost));
			migrations.add(new Migration("embassy", "cost", embassyCost));
			migrations.add(new Migration("wilds", "cost", wildsCost));
			migrations.add(new Migration("inn", "cost", innCost));
			migrations.add(new Migration("jail", "cost", jailCost));
			migrations.add(new Migration("farm", "cost", farmCost));
			migrations.add(new Migration("farm", "allowedBlocks", farmPlotBlocks));
			migrations.add(new Migration("bank", "cost", bankCost));
		}
		
		public static void migrate() {
			if (migrations.isEmpty())
				return;

			List<Map<?, ?>> mapList = TownySettings.getConfig().getMapList("townblocktypes.types");
						
			for (Migration migration : migrations) {
				for (Map<?, ?> map : mapList) {
					if (map.get("name").toString().equalsIgnoreCase(migration.type())) {
						((Map<String, Object>) map).put(migration.key(), migration.value());
					}
				}
			}

			TownySettings.getConfig().set("townblocktypes.types", mapList);
			TownySettings.getConfig().save();
			migrations.clear();
		}
		
		@Desugar
		private record Migration(String type, String key, Object value) {}
	}
}
