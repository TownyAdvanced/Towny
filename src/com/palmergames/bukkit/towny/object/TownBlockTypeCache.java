package com.palmergames.bukkit.towny.object;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TownBlockTypeCache {

	private Map<TownBlockType, Integer> typeCache = new ConcurrentHashMap<>();
	private Map<TownBlockType, Integer> forSaleCache = new ConcurrentHashMap<>(0);
	private Map<TownBlockType, Integer> residentOwnedCache = new ConcurrentHashMap<>(0);

	/**
	 * The main getter for a Town's TownBlockTypeCache. Use this is quickly get the
	 * number of TownBlocks of a given TownBlockType.
	 * 
	 * Possible CacheTypes: ALL, FORSALE, RESIDENTOWNED
	 * 
	 * @param townBlockType TownBlockType which we want to get a count of.
	 * @param cacheType     CacheType to filter by.
	 * @return number of TownBlocks which have the TownBlockType and meet the
	 *         CacheType requirement.
	 */
	public int getNumTownBlocks(TownBlockType townBlockType, CacheType cacheType) {
		return switch (cacheType) {
		case ALL -> typeCache.containsKey(townBlockType) ? typeCache.get(townBlockType) : 0;
		case FORSALE -> forSaleCache.containsKey(townBlockType) ? forSaleCache.get(townBlockType) : 0;
		case RESIDENTOWNED -> residentOwnedCache.containsKey(townBlockType) ? residentOwnedCache.get(townBlockType) : 0;
		};
	}
	
	/*
	 * Type Cache methods
	 */

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

	public int getNumberOfResidentOwnedTownBlocks() {
		return residentOwnedCache.values().stream().mapToInt(d-> d).sum();
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

	public enum CacheType {
		ALL,
		FORSALE,
		RESIDENTOWNED
	}
}
