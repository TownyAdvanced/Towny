package com.palmergames.bukkit.towny.object;

import java.util.Locale;

public class Translatable {
	private String key;
	private Object[] args;
	
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
	
	public void key(String key) {
		this.key = key;
	}
	
	public void args(Object... args) {
		this.args = args;
	}
	
	public String translate(Locale locale) {
		checkArgs(locale);
		return args == null ? Translation.of(key, locale) : Translation.of(key, locale, args);
	}
	
	public String translate() {
		checkArgs();
		return args == null ? Translation.of(key) : Translation.of(key, args);
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
}
