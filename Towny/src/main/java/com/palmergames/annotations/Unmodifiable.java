package com.palmergames.annotations;

/**
 * Marks whether a method returns a collection that cannot be changed.
 * Specifically, elements cannot be added or removed.
 * 
 * @deprecated Deprecated as of 0.98.4.1 in favour of {@link org.jetbrains.annotations.Unmodifiable}.
 */
@Deprecated
public @interface Unmodifiable {
}
