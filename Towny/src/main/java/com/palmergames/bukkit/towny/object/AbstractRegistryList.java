package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ItemLists;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
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

		// Predicates where all should match, this is used for functions that exclude certain elements. (notStartsWith, contains, etc.)
		private final Set<Predicate<T>> allMatchPredicates = new HashSet<>();
		// Predicates where only 1 has to match, this is used for functions that include new elements. (endsWith, startsWith)
		private final Set<Predicate<T>> anyMatchPredicates = new HashSet<>();

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
			final Set<T> matches = new HashSet<>();

			if (!allMatchPredicates.isEmpty() || !anyMatchPredicates.isEmpty()) {
				for (final T element : registry)
					if (allMatchPredicates.stream().allMatch(predicate -> predicate.test(element)) && (anyMatchPredicates.isEmpty() || anyMatchPredicates.stream().anyMatch(predicate -> predicate.test(element))))
						matches.add(element);
			}

			return convertFunction.apply(matches);
		}

		public Builder<T, F> startsWith(String startingWith) {
			anyMatchPredicates.add((s) -> s.getKey().getKey().regionMatches(true, 0, startingWith, 0, startingWith.length()));
			return this;
		}

		public Builder<T, F> endsWith(@NotNull String endingWith) {
			final String endingWithLower = endingWith.toLowerCase(Locale.ROOT);
			anyMatchPredicates.add((s) -> s.getKey().getKey().endsWith(endingWithLower));
			return this;
		}

		public Builder<T, F> not(@NotNull String name) {
			allMatchPredicates.add((s) -> !s.getKey().getKey().equalsIgnoreCase(name));
			return this;
		}

		public Builder<T, F> notStartsWith(@NotNull String notStartingWith) {
			allMatchPredicates.add((s) -> !s.getKey().getKey().regionMatches(true, 0, notStartingWith, 0, notStartingWith.length()));
			return this;
		}

		public Builder<T, F> notEndsWith(@NotNull String notEndingWith) {
			final String notEndingLower = notEndingWith.toLowerCase(Locale.ROOT);
			allMatchPredicates.add((s) -> !s.getKey().getKey().endsWith(notEndingLower));
			return this;
		}

		public Builder<T, F> contains(@NotNull String containing) {
			final String containingLower = containing.toLowerCase(Locale.ROOT);
			allMatchPredicates.add((s) -> s.getKey().getKey().contains(containingLower));
			return this;
		}

		public Builder<T, F> notContains(@NotNull String notContaining) {
			final String notContainingLower = notContaining.toLowerCase(Locale.ROOT);
			allMatchPredicates.add((s) -> !s.getKey().getKey().contains(notContainingLower));
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
				anyMatchPredicates.add(tag::isTagged);

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
				allMatchPredicates.add(s -> !tag.isTagged(s));

			return this;
		}

		/**
		 * Manually adds the given names to the resulting list, assuming that they can be matched and are available on the current running version.
		 * @param names An array of names to add.
		 */
		public Builder<T, F> add(@NotNull String... names) {
			for (String name : names) {
				final T match = BukkitTools.matchRegistry(this.registry, name);
				if (match != null)
					anyMatchPredicates.add(t -> t.equals(match));
				else {
					TownyMessaging.sendDebugMsg("Expected element with name '" + name + "' was not found in the " + this.clazz.getSimpleName() + " registry.");
					anyMatchPredicates.add(t -> false);
				}
			}

			return this;
		}

		/**
		 * Adds an entire ItemLists contents.
		 * @param itemList ItemLists to add.
		 */
		public Builder<T, F> addItemList(@NotNull ItemLists itemList) {
			for (Material mat: itemList.tagged) {
				final T match = BukkitTools.matchRegistry(this.registry, mat.name());
				if (match != null)
					anyMatchPredicates.add(t -> t.equals(match));
				else {
					TownyMessaging.sendDebugMsg("Expected element with name '" + mat.name() + "' was not found in the " + this.clazz.getSimpleName() + " registry.");
					anyMatchPredicates.add(t -> false);
				}
			}

			return this;
		}

		public Builder<T, F> filter(@NotNull Predicate<T> predicate) {
			allMatchPredicates.add(predicate);
			return this;
		}
		
		public Builder<T, F> conditionally(@NotNull BooleanSupplier supplier, @NotNull Consumer<Builder<T, F>> consumer) {
			if (supplier.getAsBoolean())
				consumer.accept(this);
			
			return this;
		}
	}
}
