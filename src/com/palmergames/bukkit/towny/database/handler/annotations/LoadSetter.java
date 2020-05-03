package com.palmergames.bukkit.towny.database.handler.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A helper annotation used when a field value is only
 * properly set via a setter and not the field itself.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface LoadSetter {
	String setterName();
}
