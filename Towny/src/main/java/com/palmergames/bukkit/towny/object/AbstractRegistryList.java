package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
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
}
