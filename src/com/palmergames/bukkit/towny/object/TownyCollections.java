package com.palmergames.bukkit.towny.object;

import org.apache.commons.lang.Validate;

import java.util.AbstractCollection;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;

public class TownyCollections {
	
	private static abstract class AbstractCollectionView<E> extends AbstractCollection<E> {

		@Override
		public Iterator<E> iterator() { return delegate().iterator(); }

		@Override
		public int size() { return delegate().size(); }

		@Override
		public final boolean remove(Object o) {
			throw new UnsupportedOperationException();
		}

		@Override
		public final boolean add(E e) {
			throw new UnsupportedOperationException();
		}

		abstract protected Collection<E> delegate();
	}
	/**
	 * The purpose of this collection view is to allow the
	 * Collection#contains method to gain the average O(1) lookup
	 * runtime that the map in this class uses.
	 *
	 * We can take advantage of the fact that we know
	 * the hashed key is a member of the value.
	 */
	private static final class TownBlockLookupView extends AbstractCollectionView<TownBlock> {

		final Map<WorldCoord, TownBlock> map;
		final Collection<TownBlock> view;

		TownBlockLookupView(Map<WorldCoord, TownBlock> map) {
			this.map = map;
			this.view = Collections.unmodifiableCollection(map.values());
		}

		@Override
		public boolean contains(Object o) {
			Validate.isTrue(o instanceof TownBlock);
			return map.containsKey(((TownBlock) o).getWorldCoord());
		}

		@Override
		protected Collection<TownBlock> delegate() { return view; }
	}

	/**
	 * Returns a {@link TownBlockLookupView} from the given map.
	 * 
	 * @param map A map keyed by a world coord, and storing it's respective townblock.
	 * @return A new townblock lookup view.
	 */
	public static Collection<TownBlock> townBlockLookupView(Map<WorldCoord, TownBlock> map) {
		return Collections.unmodifiableCollection(new TownBlockLookupView(map));
	}
}
