package com.palmergames.bukkit.towny.database.handler.annotations;

import com.palmergames.bukkit.towny.database.Saveable;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ForeignKey {
	Class<? extends Saveable> reference();
	boolean cascadeOnDelete() default true;
}
