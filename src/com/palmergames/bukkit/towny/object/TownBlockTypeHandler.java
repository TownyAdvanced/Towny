package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.TownBlockTypeRegisterEvent;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class TownBlockTypeHandler {
	private static Map<String, TownBlockData> townBlockDataMap = new ConcurrentHashMap<>();
	
	public static void initialize() {
		Map<String, TownBlockData> newData = new ConcurrentHashMap<>();
		for (TownBlockType type : TownBlockType.values()) {
			String typeName = type.getName().toLowerCase(Locale.ROOT);
			newData.put(typeName, new TownBlockData(typeName));
		}
		
		applyConfigSettings(newData);
		
		Bukkit.getPluginManager().callEvent(new TownBlockTypeRegisterEvent());
		
		townBlockDataMap = newData;
	}

	/**
	 * Registers a new type. Should not be used at all outside of the TownBlockTypeRegisterEvent.
	 * @param name - The name for this type.
	 * @param data - The data for this type.
	 * @throws TownyException - If a type with this name is already registered.
	 */
	public static void registerType(@NotNull String name, @Nullable TownBlockData data) throws TownyException {
		if (exists(name))
			throw new TownyException(String.format("A type named '%s' is already registered!", name));
		
		if (data == null)
			data = new TownBlockData(name);
		
		townBlockDataMap.put(name.toLowerCase(), data);
	}

	/**
	 * Gets the data for a townblock type.
	 * @param townBlockType The name of the town block type.
	 */
	@Nullable
	public static TownBlockData getData(@NotNull String townBlockType) {
		return townBlockDataMap.get(townBlockType.toLowerCase(Locale.ROOT));
	}

	/**
	 * @param typeName The name of the type to test for.
	 * @return Whether a type with the specified name exists.
	 */
	public static boolean exists(@NotNull String typeName) {
		return getData(typeName) != null;
	}
	
	private static void applyConfigSettings(Map<String, TownBlockData> newData) {

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
				
				TownBlockData data = new TownBlockData(name);
				data.setCost(cost);
				data.setTax(tax);
				data.setMapKey(mapKey);
				data.setItemUseIds(itemUseIds);
				data.setSwitchIds(switchIds);
				data.setAllowedBlocks(allowedBlocks);
				
				newData.put(name.toLowerCase(), data);
				Towny.getPlugin().getLogger().info(String.format("Loaded a townblock type: %s", name));
				
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
