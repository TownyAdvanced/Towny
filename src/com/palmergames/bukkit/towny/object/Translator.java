package com.palmergames.bukkit.towny.object;

import java.util.Locale;

public class Translator {
	private final Locale locale;
	
	public Translator(Locale locale) {
		this.locale = locale;
	}
	
	public static Translator locale(Locale locale) {
		return new Translator(locale);
	}
	
	public String of(String key) {
		return Translation.of(key, locale);
	}
	
	public String of(String key, Object... args) {
		return Translation.of(key, locale, args);
	}
}
