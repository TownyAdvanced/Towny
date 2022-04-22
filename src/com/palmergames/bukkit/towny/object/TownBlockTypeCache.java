package com.palmergames.bukkit.towny.object;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TownBlockTypeCache {

	private Map<TownBlockType, Integer> typeCache = new ConcurrentHashMap<>();
	private Map<TownBlockType, Integer> forSaleCache = new ConcurrentHashMap<>();
	private Map<TownBlockType, Integer> residentOwnedCache = new ConcurrentHashMap<>();
	
	/*
	 * Type Cache methods
	 */

	public int getNumTownBlocksOfType(TownBlockType type) {
		return typeCache.get(type);
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

	/*
	 * ForSale by Type Cache methods
	 */

	public int getNumTownBlocksOfTypeForSale(TownBlockType type) {
		return forSaleCache.get(type);
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

	
	/*
	 * Resident-owned by Type Cache methods
	 */

	public int getNumTownBlocksOfTypeResidentOwned(TownBlockType type) {
		return residentOwnedCache.get(type);
	}

	public void removeTownBlockOfTypeResidentOwned(TownBlock townBlock) {
		residentOwnedCache.merge(townBlock.getType(), -1, Integer::sum);
	}

	public void addTownBlockOfTypeResidentOwned(TownBlock townBlock) {
		residentOwnedCache.merge(townBlock.getType(), 1, Integer::sum);
	}

	public void removeTownBlockOfTypeResidentOwned(TownBlockType type) {
		residentOwnedCache.merge(type, -1, Integer::sum);
	}

	public void addTownBlockOfTypeResidentOwned(TownBlockType type) {
		residentOwnedCache.merge(type, 1, Integer::sum);
	}
	
}
