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
		typeCache.merge(townBlock.getType(), -1, Integer::sum);
	}
	
	public void addTownBlockOfType(TownBlock townBlock) {
		typeCache.merge(townBlock.getType(), 1, Integer::sum);
	}
	
	public void removeTownBlockOfType(TownBlockType type) {
		typeCache.merge(type, -1, Integer::sum);
	}
	
	public void addTownBlockOfType(TownBlockType type) {
		typeCache.merge(type, 1, Integer::sum);
	}
	
	public int getNumTownBlocksOfType(TownBlockType type) {
		return typeCache.get(type);
	}
	
	public Map<TownBlockType, Integer> getForSaleCache() {
		return forSaleCache;
	}
	
	public void removeTownBlockOfTypeForSale(TownBlock townBlock) {
		forSaleCache.merge(townBlock.getType(), -1, Integer::sum);
	}
	
	public void addTownBlockOfTypeForSale(TownBlock townBlock) {
		forSaleCache.merge(townBlock.getType(), 1, Integer::sum);
	}
	
	public void removeTownBlockOfTypeForSale(TownBlockType type) {
		forSaleCache.merge(type, -1, Integer::sum);
	}
	
	public void addTownBlockOfTypeForSale(TownBlockType type) {
		forSaleCache.merge(type, 1, Integer::sum);
	}
	
	public int getNumTownBlocksOfTypeForSale(TownBlock townBlock) {
		return forSaleCache.get(townBlock.getType());
	}
	
	public int getNumTownBlocksOfTypeForSale(TownBlockType type) {
		return forSaleCache.get(type);
	}

}
