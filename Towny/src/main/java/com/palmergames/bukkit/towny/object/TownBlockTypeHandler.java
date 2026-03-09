package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.CommentedConfiguration;
import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.TownBlockTypeRegisterEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.ItemLists;
import com.palmergames.util.StringMgmt;

import net.kyori.adventure.text.format.NamedTextColor;

import org.bukkit.Material;
import org.bukkit.Registry;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;

public final class TownBlockTypeHandler {
	private final static Map<String, TownBlockType> townBlockTypeMap = new ConcurrentHashMap<>();
	
	public static void initialize() {
		Map<String, TownBlockType> newData = new ConcurrentHashMap<>();
		
		for (Field field : TownBlockType.class.getFields()) {
			try {
				TownBlockType type = (TownBlockType) field.get(null);
				newData.put(type.getName().toLowerCase(Locale.ROOT), type);
			} catch (Exception ignored) {}
		}
		
		applyConfigSettings(newData);
		
		// Overwrite any entries of our own built-in townblocktypes.
		townBlockTypeMap.putAll(newData);
		
		BukkitTools.fireEvent(new TownBlockTypeRegisterEvent());
		
		Towny.getPlugin().getLogger().info(String.format("Config: Loaded %d townblock types: %s.", townBlockTypeMap.size(), StringMgmt.join(townBlockTypeMap.keySet(), ", ")));
	}

	/**
	 * Registers a new type. Should not be used at all outside of the TownBlockTypeRegisterEvent.
	 * @param type - The type
	 * @throws TownyException - If a type with this name is already registered.
	 */
	public static void registerType(@NotNull TownBlockType type) throws TownyException {
		if (exists(type.getName()))
			throw new TownyException(String.format("API: A type named '%s' is already registered!", type.getName()));
		
		townBlockTypeMap.put(type.getName().toLowerCase(Locale.ROOT), type);
		Towny.getPlugin().getLogger().info(String.format("API: A new townblock type was registered: %s", type.getName()));
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
	 * @param typeName The name of the town block type.
	 * @return The townblocktype instance, or {@code null} if none is registered.   
	 */
	@Nullable
	public static TownBlockType getType(@NotNull String typeName) {
		return townBlockTypeMap.get(typeName.toLowerCase(Locale.ROOT));
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
	
	@SuppressWarnings("unchecked")
	private static void applyConfigSettings(Map<String, TownBlockType> newData) {

		List<Map<?, ?>> types = TownySettings.getConfig().getMapList("townblocktypes.types");
		for (Map<?, ?> genericType : types) {
			String name = "unknown type";
			
			try {
				Map<String, Object> type = (Map<String, Object>) genericType;

				name = String.valueOf(type.get("name"));
				double cost = parseDouble(type.getOrDefault("cost", 0.0).toString());
				double tax = parseDouble(type.getOrDefault("tax", 0.0).toString());
				String mapKey = String.valueOf(type.getOrDefault("mapKey", "+"));

				String colourName = String.valueOf(type.getOrDefault("colour", ""));
				NamedTextColor colour = colourName.isEmpty() ? null : Colors.toNamedTextColor(colourName);

				Set<Material> itemUseIds = loadMaterialList("itemUseIds", String.valueOf(type.getOrDefault("itemUseIds", "")), name);
				Set<Material> switchIds = loadMaterialList("switchIds", String.valueOf(type.getOrDefault("switchIds", "")), name);
				Set<Material> allowedBlocks = loadMaterialList("allowedBlocks", String.valueOf(type.getOrDefault("allowedBlocks", "")), name);
				
				TownBlockType townBlockType = newData.get(name.toLowerCase(Locale.ROOT));
				TownBlockData data;
				
				if (townBlockType == null) {
					data = new TownBlockData();
					townBlockType = new TownBlockType(name, data);
				} else
					data = townBlockType.getData();
				
				data.setCost(cost);
				data.setTax(tax);
				data.setMapKey(mapKey);
				data.setColour(colour);
				data.setItemUseIds(itemUseIds);
				data.setSwitchIds(switchIds);
				data.setAllowedBlocks(allowedBlocks);
				
				newData.put(name.toLowerCase(Locale.ROOT), townBlockType);
				
			} catch (Exception e) {
				Towny.getPlugin().getLogger().log(Level.WARNING, String.format("Config: Error while loading townblock type '%s', skipping...", name), e);
			}
		}
	}
	
	private static Set<Material> loadMaterialList(String listName, String materialList, String typeName) {
		if (!materialList.isEmpty()) {
			Set<Material> set = new LinkedHashSet<>();
			for (String materialName : materialList.split(",")) {
				if (ItemLists.hasGroup(materialName)) {
					set.addAll(ItemLists.getGrouping(materialName));
					continue;
				}
				
				Material material = matchMaterial(materialName, listName, typeName);
				if (material != null)
					set.add(material);
			}
			
			return set;
		} else
			return new HashSet<>();
	}

	@Nullable
	private static Material matchMaterial(String materialName, String listName, String typeName) {
		Material material = BukkitTools.matchRegistry(Registry.MATERIAL, materialName);
		if (material == null)
			TownyMessaging.sendDebugMsg(String.format("Could not find a material named '%s' while loading the " + listName + " list for the %s type.", materialName, typeName));
		
		return material;
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
		
		public static void checkForLegacyOptions(CommentedConfiguration config) {
			
			if (config.contains(ConfigNodes.TOWNBLOCKTYPES_TYPES.getRoot()))
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
		
		@SuppressWarnings("unchecked")
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
		
		private record Migration(String type, String key, Object value) {}
	}
}
