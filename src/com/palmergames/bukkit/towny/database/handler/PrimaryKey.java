package com.palmergames.bukkit.towny.database.handler;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks if a field is a primary key for an SQL table.
 * 
 * Note: Because you can only have one primary key, the first primary key
 * found within the field list, is considered the primary key.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface PrimaryKey {} // Marker Annotation, no body needed!
