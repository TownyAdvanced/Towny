package com.palmergames.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Marks whether a method returns a collection that cannot be changed.
 * Specifically, elements cannot be added or removed.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Unmodifiable {
}
