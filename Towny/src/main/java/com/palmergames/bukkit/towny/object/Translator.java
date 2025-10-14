package com.palmergames.bukkit.towny.object;

import java.util.Locale;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.checkerframework.framework.qual.DefaultQualifier;
import org.jetbrains.annotations.NotNull;

@DefaultQualifier(NotNull.class)
public class Translator {
	private final Locale locale;
	
	public Translator(Locale locale) {
		this.locale = locale;
	}
	
	public static Translator locale(Locale locale) {
		return new Translator(locale);
	}
	
	public static Translator locale(CommandSender sender) {
		return new Translator(Translation.getLocale(sender));
	}
	
	public String of(String key) {
		return Translation.of(key, locale);
	}
	
	public String of(String key, Object... args) {
		return Translation.of(key, locale, args);
	}
	
	public Component component(String key) {
		return Translatable.of(key).locale(this.locale).component();
	}
	
	public Component component(String key, Object... args) {
		return Translatable.of(key, args).locale(this.locale).component();
	}
	
	public Locale locale() {
		return this.locale;
	}
}
