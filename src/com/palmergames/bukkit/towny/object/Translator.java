package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;

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
	
	public Component comp(String key) {
		return TownyComponents.miniMessage(of(key));
	}
	
	public Component comp(String key, Object... args) {
		return TownyComponents.miniMessage(of(key, args));
	}
}
