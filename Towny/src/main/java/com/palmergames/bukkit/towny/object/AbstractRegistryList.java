package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.util.BukkitTools;

import com.palmergames.bukkit.util.EntityLists;
import com.palmergames.bukkit.util.ItemLists;
import com.palmergames.util.JavaUtil;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractRegistryList<T extends Keyed> {
	private final Registry<T> registry;
	protected final Set<T> tagged = new HashSet<>();

	public AbstractRegistryList(@NotNull Registry<T> registry, @NotNull Collection<T> collection) {
		this.registry = registry;
		this.tagged.addAll(collection);
	}

	/**
	 * @param element The element to test for
	 * @return Whether this list contains the given element
	 */
	public boolean contains(@NotNull T element) {
		return this.tagged.contains(element);
	}

	public boolean contains(@NotNull NamespacedKey key) {
		final T element = registry.get(key);

		return element != null && this.contains(element);
	}

	public boolean contains(@NotNull String element) {
		if (element.isEmpty())
			return false;

		final T matched = BukkitTools.matchRegistry(this.registry, element);
		return matched != null && this.contains(matched);
	}
	
	protected Collection<T> tagged() {
		return this.tagged;
	}

	public static class Builder<T extends Keyed, F extends AbstractRegistryList<T>> {
		private final Registry<T> registry;
		private final Class<T> clazz;
		private final Function<Collection<T>, F> convertFunction;

		private final List<LayerConsumer<T>> layers = new ArrayList<>();

		/**
		 * @param registry The bukkit registry, used for matching strings into {@link T}.
		 * @param clazz The class that belongs to {@link T}, used for tag lookups in {@link #withTag(String, NamespacedKey)}.
		 * @param function The mapping function to convert a {@code Collection<T>} to your implementing class, this can just be {@code MyClass::new}.
		 */
		public Builder(Registry<T> registry, Class<T> clazz, Function<Collection<T>, F> function) {
			this.registry = registry;
			this.clazz = clazz;
			this.convertFunction = function;
		}

		public F build() {
			Set<T> allValues = new HashSet<>();
			
			for (final T value : registry) {
				allValues.add(value);
			}
			
			allValues = Collections.unmodifiableSet(allValues);
			
			final Set<T> matches = new HashSet<>();

			for (final LayerConsumer<T> layer : layers) {
				layer.accept(matches, allValues);
			}

			return convertFunction.apply(matches);
		}

		public Builder<T, F> startsWith(String startingWith) {
			layers.add((currentSet, allPossible) -> {
				for (final T value : allPossible)
					if (value.getKey().getKey().regionMatches(true, 0, startingWith, 0, startingWith.length()))
						currentSet.add(value);
			});
			return this;
		}

		public Builder<T, F> endsWith(@NotNull String endingWith) {
			final String endingWithLower = endingWith.toLowerCase(Locale.ROOT);
			layers.add((currentSet, allPossible) -> {
				for (final T value : allPossible)
					if (value.getKey().getKey().endsWith(endingWithLower))
						currentSet.add(value);
			});
			return this;
		}

		public Builder<T, F> not(@NotNull String name) {
			final T value = get(name);
			if (value != null)
				layers.add((currentSet, allPossible) -> currentSet.remove(value));

			return this;
		}

		public Builder<T, F> notStartsWith(@NotNull String notStartingWith) {
			layers.add((currentSet, allPossible) -> {
				currentSet.removeIf(value -> value.getKey().getKey().regionMatches(true, 0, notStartingWith, 0, notStartingWith.length()));
			});
			return this;
		}

		public Builder<T, F> notEndsWith(@NotNull String notEndingWith) {
			final String notEndingLower = notEndingWith.toLowerCase(Locale.ROOT);
			layers.add((currentSet, allPossible) -> {
				currentSet.removeIf(value -> value.getKey().getKey().endsWith(notEndingLower));
			});
			return this;
		}

		public Builder<T, F> contains(@NotNull String containing) {
			final String containingLower = containing.toLowerCase(Locale.ROOT);
			layers.add((currentSet, allPossible) -> {
				for (final T value : allPossible)
					if (value.getKey().getKey().contains(containingLower))
						currentSet.add(value);
			});
			return this;
		}

		public Builder<T, F> notContains(@NotNull String notContaining) {
			final String notContainingLower = notContaining.toLowerCase(Locale.ROOT);
			layers.add((currentSet, allPossible) -> {
				currentSet.removeIf(value -> value.getKey().getKey().contains(notContainingLower));
			});
			return this;
		}

		/**
		 * Matches all elements in a specific vanilla tag to be added to the resulting list.
		 * <br>Tags can be found <a href="https://github.com/misode/mcmeta/tree/data/data/minecraft/tags">here</a>.
		 * @param registry The registry to look in, i.e. 'blocks' or 'items'. See {@link Tag} for registry name constants.
		 * @param key The key of the tag.
		 */
		public Builder<T, F> withTag(@NotNull String registry, @NotNull NamespacedKey key) {
			final Tag<T> tag = Bukkit.getServer().getTag(registry, key, this.clazz);

			if (tag != null)
				layers.add((currentSet, allPossible) -> currentSet.addAll(tag.getValues()));

			return this;
		}

		/**
		 * Identical to {@link #withTag(String, NamespacedKey)}, but reverse.
		 * @param registry The registry to look in, i.e. 'blocks' or 'items'. See {@link Tag} for registry name constants.
		 * @param key The key of the tag.
		 */
		public Builder<T, F> excludeTag(@NotNull String registry, @NotNull NamespacedKey key) {
			final Tag<T> tag = Bukkit.getServer().getTag(registry, key, this.clazz);

			if (tag != null)
				layers.add((currentSet, allPossible) -> currentSet.removeAll(tag.getValues()));

			return this;
		}

		/**
		 * Manually adds the given names to the resulting list, assuming that they can be matched and are available on the current running version.
		 * @param names An array of names to add.
		 */
		public Builder<T, F> add(@NotNull String... names) {
			for (String name : names) {
				final T match = get(name);
				if (match != null)
					layers.add((currentSet, allPossible) -> currentSet.add(match));
				else {
					try {
						TownyMessaging.sendDebugMsg("Expected element with name '" + name + "' was not found in the " + this.clazz.getSimpleName() + " registry.");
					} catch (final Exception ignored) {}
				}
			}

			return this;
		}

		/**
		 * Adds an entire lists contents.
		 * @param list list to add.
		 */
		public Builder<T, F> includeList(@NotNull AbstractRegistryList<T> list) {
			layers.add((currentSet, allPossible) -> {
				currentSet.addAll(list.tagged());
			});

			return this;
		}
		
		public Builder<T, F> retainList(@NotNull AbstractRegistryList<T> list) {
			layers.add(((currentSet, allPossible) -> {
				currentSet.retainAll(list.tagged());
			}));
			return this;
		}

		public Builder<T, F> removeIf(@NotNull Predicate<T> predicate) {
			layers.add((currentSet, allPossible) -> {
				currentSet.removeIf(predicate);
			});
			return this;
		}

		public Builder<T, F> addIf(@NotNull Predicate<T> predicate) {
			layers.add((currentSet, allPossible) -> {
				for (final T value : allPossible)
					if (predicate.test(value))
						currentSet.add(value);
			});
			return this;
		}
		
		public Builder<T, F> conditionally(@NotNull BooleanSupplier supplier, @NotNull Consumer<Builder<T, F>> consumer) {
			if (supplier.getAsBoolean())
				consumer.accept(this);
			
			return this;
		}
		
		@Nullable
		private T get(final String name) {
			return BukkitTools.matchRegistry(this.registry, name);
		}
		
		private interface LayerConsumer<T> extends BiConsumer<Collection<T>, Collection<T>> {
			@Override
			void accept(final @NotNull Collection<T> currentSet, final @NotNull @Unmodifiable Collection<T> allPossible);
		}
	}

	/**
	 * A class to allow round tripping with string names
	 */
	@SuppressWarnings("unchecked")
	public static class CompactableCollection<T extends Keyed> extends AbstractCollection<T> {
		private final List<String> names = new ArrayList<>();
		private final Class<T> clazz;
		
		private final Set<T> cachedValues = new HashSet<>();

		public CompactableCollection(Class<T> clazz) {
			this.clazz = clazz;
		}

		@ApiStatus.Internal
		protected static CompactableCollection<Material> materials() {
			return new CompactableCollection<>(Material.class);
		}
		
		protected static CompactableCollection<Material> materials(final @NotNull Collection<String> materials) {
			return JavaUtil.make(materials(), m -> m.setNames(materials));
		}

		@ApiStatus.Internal
		protected static CompactableCollection<EntityType> entityTypes() {
			return new CompactableCollection<>(EntityType.class);
		}

		@ApiStatus.Internal
		protected static CompactableCollection<EntityType> entityTypes(final @NotNull Collection<String> entityTypes) {
			return JavaUtil.make(entityTypes(), m -> m.setNames(entityTypes));
		}
		
		public void setNames(final Collection<String> names) {
			synchronized (this.names) {
				this.names.clear();
				this.names.addAll(names);
			}
			
			updateCache();
		}
		
		public Collection<String> getNames() {
			return this.names;
		}

		@Override
		public @NotNull Iterator<T> iterator() {
			synchronized (cachedValues) {
				return cachedValues.iterator();
			}
		}

		@Override
		public int size() {
			synchronized (cachedValues) {
				return cachedValues.size();
			}
		}
		
		@Override
		public boolean add(final T value) {
			Objects.requireNonNull(value, "value");
			final String asString = BukkitTools.keyAsString(value.getKey());

			//noinspection ConstantValue
			final boolean result = !this.names.contains(asString) && this.names.add(asString);
			if (result) updateCache();
			
			return result;
		}
		
		@Override
		public boolean remove(final Object object) {
			Objects.requireNonNull(object, "value");
			
			if (!object.getClass().equals(this.clazz) || !(object instanceof Keyed key))
				throw new ClassCastException(object.getClass() + " is not a " + this.clazz);
			
			final T value = (T) object;
			final String asString = BukkitTools.keyAsString(value.getKey());

			final boolean result = this.names.remove(BukkitTools.keyAsString(key.getKey()));
			if (result) updateCache();
			
			// Value is still part of the cached values despite being removed from names, which must mean they're part of a group
			if (this.cachedValues.contains(value) && !this.names.contains(asString)) {
				// Find the group
				for (final Map.Entry<String, Collection<? extends Keyed>> group : getAllGroups().entrySet()) {
					int index = this.names.indexOf(group.getKey());
					
					if (index != -1 && group.getValue().contains(value)) {
						this.names.remove(index);
						
						// Create a copy of the group values without the element that's being removed
						Set<? extends Keyed> replacingValues = new HashSet<>(group.getValue());
						replacingValues.remove(value);
						
						this.names.addAll(index, BukkitTools.convertKeyedToString(replacingValues));
						updateCache();
						
						return true;
					}
				}
				
			}

			return result;
		}

		/**
		 * @return {@code true} if names were able to be compacted into a group
		 */
		public boolean compact() {
			final int oldSize = this.names.size();

			Collection<String> newNames = BukkitTools.convertKeyedToString(this.cachedValues);

			for (Map.Entry<String, Collection<? extends Keyed>> group : getAllGroups().entrySet()) {
				Collection<String> asString = BukkitTools.convertKeyedToString(group.getValue());

				if (newNames.containsAll(asString)) {
					newNames.removeAll(asString);
					newNames.add(group.getKey());
				}
			}
			
			this.setNames(newNames);
			
			return this.names.size() != oldSize;
		}
		
		private void updateCache() {
			synchronized (cachedValues) {
				cachedValues.clear();
				
				if (this.clazz == Material.class)
					cachedValues.addAll((Collection<T>) TownySettings.toMaterialSet(this.names));
				else if (this.clazz == EntityType.class)
					cachedValues.addAll((Collection<T>) TownySettings.toEntityTypeSet(this.names));
				else 
					throw new UnsupportedOperationException("Unsupported class " + this.clazz);
			}
		}
		
		private Map<String, Collection<? extends Keyed>> getAllGroups() {
			if (this.clazz == Material.class)
				return ItemLists.allGroups();
			else if (this.clazz == EntityType.class) {
				return EntityLists.allGroups();
			} else
				throw new UnsupportedOperationException("Unsupported class " + this.clazz);
		}
	}
}
