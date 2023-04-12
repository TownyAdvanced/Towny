package com.palmergames.bukkit.towny.object;

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class TownBlockTypeCache {

	private Map<TownBlockType, Integer> typeCache = new ConcurrentHashMap<>();
	private Map<TownBlockType, Integer> forSaleCache = new ConcurrentHashMap<>(0);
	private Map<TownBlockType, Integer> residentOwnedCache = new ConcurrentHashMap<>(0);

	/**
	 * The main getter for a Town's TownBlockTypeCache. Use this to quickly get the
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
		case ALL -> typeCache.getOrDefault(townBlockType, 0);
		case FORSALE -> forSaleCache.getOrDefault(townBlockType, 0);
		case RESIDENTOWNED -> residentOwnedCache.getOrDefault(townBlockType, 0);
		};
	}

	/**
	 * Getter for easy access to each cache Map
	 * 
	 * Possible CacheTypes: ALL, FORSALE, RESIDENTOWNED
	 * 
	 * @param cacheType CacheType map to access
	 * @return unmodifiable Map of each type to an integer
	 */
	public Map<TownBlockType, Integer> getCache(CacheType cacheType) {
		return switch (cacheType) {
			case ALL -> Collections.unmodifiableMap(typeCache);
			case FORSALE -> Collections.unmodifiableMap(forSaleCache);
			case RESIDENTOWNED -> Collections.unmodifiableMap(residentOwnedCache);
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
