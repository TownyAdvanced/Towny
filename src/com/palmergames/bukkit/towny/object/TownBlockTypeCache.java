package com.palmergames.bukkit.towny.object;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TownBlockTypeCache {

	private Map<TownBlockType, Integer> typeCache = new ConcurrentHashMap<>();
	private Map<TownBlockType, Integer> forSaleCache = new ConcurrentHashMap<>();
	
	public Map<TownBlockType, Integer> getTypeCache() {
		return typeCache;
	}
	
	public void removeTownBlockOfType(TownBlock townBlock) {
		typeCache.put(townBlock.getType(), typeCache.get(townBlock.getType()).intValue() - 1);
	}
	
	public void addTownBlockOfType(TownBlock townBlock) {
		typeCache.put(townBlock.getType(), typeCache.get(townBlock.getType()).intValue() + 1);
	}
	
	public void removeTownBlockOfType(TownBlockType type) {
		typeCache.put(type, typeCache.get(type).intValue() - 1);
	}
	
	public void addTownBlockOfType(TownBlockType type) {
		typeCache.put(type, typeCache.get(type).intValue() + 1);
	}
	
	public int getNumTownBlocksOfType(TownBlockType type) {
		return typeCache.get(type);
	}
	
	public Map<TownBlockType, Integer> getForSaleCache() {
		return forSaleCache;
	}
	
	public void removeTownBlockOfTypeForSale(TownBlock townBlock) {
		forSaleCache.put(townBlock.getType(), forSaleCache.get(townBlock.getType()).intValue() - 1);
	}
	
	public void addTownBlockOfTypeForSale(TownBlock townBlock) {
		forSaleCache.put(townBlock.getType(), forSaleCache.get(townBlock.getType()).intValue() + 1);
	}
	
	public void removeTownBlockOfTypeForSale(TownBlockType type) {
		forSaleCache.put(type, forSaleCache.get(type).intValue() - 1);
	}
	
	public void addTownBlockOfTypeForSale(TownBlockType type) {
		forSaleCache.put(type, forSaleCache.get(type).intValue() + 1);
	}
	
	public int getNumTownBlocksOfTypeForSale(TownBlock townBlock) {
		return forSaleCache.get(townBlock.getType());
	}
	
	public int getNumTownBlocksOfTypeForSale(TownBlockType type) {
		return forSaleCache.get(type);
	}

}
