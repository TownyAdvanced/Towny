package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.TownBlockTypeRegisterEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.HashSet;
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
		
		Bukkit.getPluginManager().callEvent(new TownBlockTypeRegisterEvent());
		
		Towny.getPlugin().getLogger().info(String.format("Loaded %d townblock types: %s", newData.size(), Arrays.toString(newData.keySet().toArray())));
		
		townBlockTypeMap = newData;
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
				double cost = Double.parseDouble(type.get("cost").toString());
				double tax = Double.parseDouble(type.get("tax").toString());
				String mapKey = String.valueOf(type.get("mapKey"));

				Set<Material> itemUseIds = loadMaterialList(String.valueOf(type.get("itemUseIds")), name);
				Set<Material> switchIds = loadMaterialList(String.valueOf(type.get("switchIds")), name);
				Set<Material> allowedBlocks = loadMaterialList(String.valueOf(type.get("allowedBlocks")), name);
				
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
	
	private static Set<Material> loadMaterialList(String materialList, String typeName) {
		if (!materialList.isEmpty()) {
			Set<Material> set = new HashSet<>();
			for (String materialName : materialList.split(",")) {
				Material material = Material.matchMaterial(materialName);

				if (material == null)
					Towny.getPlugin().getLogger().warning(String.format("Could not find a material named '%s' while loading the item use list for the %s type.", materialName, typeName));
				else
					set.add(material);
			}
			
			return set;
		} else
			return new HashSet<>();
	}
	
	private TownBlockTypeHandler() {}
}
