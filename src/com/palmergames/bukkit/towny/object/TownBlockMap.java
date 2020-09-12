package com.palmergames.bukkit.towny.object;

import com.google.common.collect.ForwardingConcurrentMap;
import org.apache.commons.lang.Validate;
import org.jetbrains.annotations.NotNull;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * A concurrent map which facilitates the storage of TownBlocks.
 * 
 * <p>The key advantage of this map is the fast lookup it provides when
 * accessing it's value collection, instead of an O(n) lookup for values
 * it maintains a O(1) value lookup time complexity</p>
 */
public class TownBlockMap extends ForwardingConcurrentMap<WorldCoord, TownBlock> {
	
	private final ConcurrentMap<WorldCoord, TownBlock> map = new ConcurrentHashMap<>();

	@Override
	public boolean containsValue(Object value) {
		Validate.isTrue(value instanceof TownBlock);
		return map.containsKey(((TownBlock) value).getWorldCoord());
	}

	@Override
	protected ConcurrentMap<WorldCoord, TownBlock> delegate() {
		return map;
	}
}
