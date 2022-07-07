package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;

import java.util.Locale;

import org.bukkit.command.CommandSender;

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
	
	public Component comp(String key) {
		return TownyComponents.miniMessageAndColour(of(key));
	}
	
	public Component comp(String key, Object... args) {
		return TownyComponents.miniMessageAndColour(of(key, args));
	}
}
