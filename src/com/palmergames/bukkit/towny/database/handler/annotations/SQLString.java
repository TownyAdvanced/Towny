package com.palmergames.bukkit.towny.database.handler.annotations;

import com.palmergames.bukkit.towny.database.handler.SQLStringType;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface SQLString {
	SQLStringType stringType();
	int length() default 0;
}
