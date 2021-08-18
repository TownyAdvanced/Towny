package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.util.Colors;

import java.util.Locale;

import org.bukkit.entity.Player;

public class Translatable {
	private String key;
	private Object[] args;
	private boolean stripColors;
	private String appended = "";
	
	private Translatable(String key) {
		this.key = key;
	}
	
	private Translatable(String key, Object... args) {
		this.key = key;
		this.args = args;
	}
	
	public static Translatable of(String key) {
		return new Translatable(key);
	}
	
	public static Translatable of(String key, Object... args) {
		return new Translatable(key, args);
	}
	
	public String key() {
		return key;
	}
	
	public Object[] args() {
		return args;
	}
	
	public boolean stripColors() {
		return stripColors;
	}
	
	public String appended() {
		return appended;
	}
	
	public Translatable key(String key) {
		this.key = key;
		return this;
	}
	
	public Translatable args(Object... args) {
		this.args = args;
		return this;
	}
	
	public Translatable stripColors(boolean strip) {
		this.stripColors = strip;
		return this;
	}
	
	public Translatable append(String append) {
		appended += append;
		return this;
	}
	
	public String translate(Locale locale) {
		checkArgs(locale);
		String translated = args == null ? Translation.of(key, locale) : Translation.of(key, locale, args);
		translated += appended;
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public String translate() {
		checkArgs();
		String translated = args == null ? Translation.of(key) : Translation.of(key, args);
		translated += appended;
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public String forLocale(Resident resident) {
		return translate(Translation.getLocale(resident));
	}
	
	public String forLocale(Player player) {
		return translate(Translation.getLocale(player));
	}

	private void checkArgs() {
		if (args == null)
			return;
		
		for (int i = 0; i < args.length; i++)
			if (args[i] instanceof Translatable)
				args[i] = ((Translatable) args[i]).translate();
	}
	
	private void checkArgs(Locale locale) {
		if (args == null)
			return;
		
		for (int i = 0; i < args.length; i++)
			if (args[i] instanceof Translatable)
				args[i] = ((Translatable) args[i]).translate(locale);
	}
	
	@Override
	public String toString() {
		return translate();
	}
}
