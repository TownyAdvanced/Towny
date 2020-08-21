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
 */
public class TownBlockMap extends ForwardingConcurrentMap<WorldCoord, TownBlock> {
	
	private final ConcurrentMap<WorldCoord, TownBlock> map = new ConcurrentHashMap<>();
	
	@NotNull
	@Override
	public Collection<TownBlock> values() {
		return new ValueLookupView(map.values());
	}

	/**
	 * The purpose of this collection view is to allow the
	 * Collection#contains method associated with Collection#values
	 * to gain the average O(1) lookup runtime that the map
	 * in this class uses.
	 *
	 * <p>We can take advantage of the fact that we know
	 * the hashed key is a member of the value.</p>
	 */
	private final class ValueLookupView extends AbstractCollection<TownBlock> {
		private final Collection<TownBlock> view;
		
		ValueLookupView(Collection<TownBlock> view) {
			this.view = view;
		}

		@Override
		public Iterator<TownBlock> iterator() { return view.iterator(); }

		@Override
		public int size() { return view.size(); }

		@Override
		public boolean contains(Object o) {
			Validate.isTrue(o instanceof TownBlock);
			return TownBlockMap.this.containsKey(((TownBlock) o).getWorldCoord());
		}
	}

	@Override
	protected ConcurrentMap<WorldCoord, TownBlock> delegate() {
		return map;
	}
}
