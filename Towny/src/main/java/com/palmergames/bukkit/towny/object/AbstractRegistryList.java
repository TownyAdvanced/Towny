package com.palmergames.bukkit.towny.object;

import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Tag;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

public abstract class AbstractRegistryList<T extends Keyed> {
	private final Registry<T> registry;
	protected final Set<T> tagged = new HashSet<>();
	
	public AbstractRegistryList(Registry<T> registry, Collection<T> collection) {
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
		T element = registry.get(key);
		if (element == null)
			return false;
		
		return this.contains(element);
	}
	
	public boolean contains(@NotNull String element) {
		if (element.isEmpty())
			return false;
		
		T matched = registry.match(element);
		if (matched != null)
			return this.contains(matched);
		
		return false;
	}
	
	@SuppressWarnings("unused")
	public static class Builder<T extends Keyed, F extends AbstractRegistryList<T>> {
		private final Registry<T> registry;
		private final Class<T> clazz;
		private final Function<Collection<T>, F> convertFunction;
		
		// Predicates where all should match, this is used for functions that exclude certain elements. (notStartsWith, contains, etc.)
		private final Set<Predicate<NamespacedKey>> allMatchPredicates = new HashSet<>();
		// Predicates where only 1 has to match, this is used for functions that include new elements. (endsWith, startsWith)
		private final Set<Predicate<NamespacedKey>> anyMatchPredicates = new HashSet<>();
		private @Nullable Set<T> exceptions;
		
		public Builder(Registry<T> registry, Class<T> clazz, Function<Collection<T>, F> function) {
			this.registry = registry;
			this.clazz = clazz;
			this.convertFunction = function;
		}

		public F build() {
			final Set<T> matches = new HashSet<>();

			if (!allMatchPredicates.isEmpty() || !anyMatchPredicates.isEmpty()) {
				for (T element : registry)
					if (allMatchPredicates.stream().allMatch(predicate -> predicate.test(element.getKey())) && anyMatchPredicates.stream().anyMatch(predicate -> predicate.test(element.getKey())))
						matches.add(element);
			}

			if (exceptions != null)
				matches.addAll(exceptions);

			return convertFunction.apply(matches);
		}

		public Builder<T, F> startsWith(String startingWith) {
			anyMatchPredicates.add((s) -> s.getKey().regionMatches(true, 0, startingWith, 0, startingWith.length()));
			return this;
		}

		public Builder<T, F> endsWith(@NotNull String endingWith) {
			anyMatchPredicates.add((s) -> s.getKey().toLowerCase(Locale.ROOT).endsWith(endingWith.toLowerCase(Locale.ROOT)));
			return this;
		}

		public Builder<T, F> not(@NotNull String name) {
			allMatchPredicates.add((s) -> !s.getKey().toLowerCase(Locale.ROOT).equals(name.toLowerCase(Locale.ROOT)));
			return this;
		}

		public Builder<T, F> notStartsWith(@NotNull String notStartingWith) {
			allMatchPredicates.add((s) -> !s.getKey().regionMatches(true, 0, notStartingWith, 0, notStartingWith.length()));
			return this;
		}

		public Builder<T, F> notEndsWith(@NotNull String notEndingWith) {
			allMatchPredicates.add((s) -> !s.getKey().toLowerCase(Locale.ROOT).endsWith(notEndingWith.toLowerCase(Locale.ROOT)));
			return this;
		}

		public Builder<T, F> contains(@NotNull String containing) {
			allMatchPredicates.add((s) -> s.getKey().toLowerCase(Locale.ROOT).contains(containing.toLowerCase(Locale.ROOT)));
			return this;
		}

		public Builder<T, F> notContains(@NotNull String notContaining) {
			allMatchPredicates.add((s) -> !s.getKey().toLowerCase(Locale.ROOT).contains(notContaining.toLowerCase(Locale.ROOT)));
			return this;
		}

		/**
		 * Tags can be found <a href="https://github.com/misode/mcmeta/tree/data/data/minecraft/tags">here</a>.
		 * @param registry The registry to look in, i.e. 'blocks' or 'items'
		 * @param key The key of the tag
		 */
		public Builder<T, F> withTag(@NotNull String registry, @NotNull NamespacedKey key) {
			Tag<T> tag = Bukkit.getServer().getTag(registry, key, this.clazz);
			
			if (tag != null)
				anyMatchPredicates.add(s -> {
					T exact = this.registry.get(s);
					return exact != null && tag.isTagged(exact);
				});
			
			return this;
		}

		public Builder<T, F> add(@NotNull String... names) {
			if (exceptions == null)
				exceptions = new HashSet<>();

			for (String name : names) {
				if (name.isEmpty())
					continue;

				T match = registry.match(name);
				if (match != null)
					exceptions.add(match);
			}

			return this;
		}
	}
}