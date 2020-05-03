package com.palmergames.bukkit.towny.database.handler.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A helper annotation which is used in scenarios where
 * the getter method provides a more accurate representation
 * of the field value, then the field's value itself.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface SaveGetter {
	String keyName();
}
