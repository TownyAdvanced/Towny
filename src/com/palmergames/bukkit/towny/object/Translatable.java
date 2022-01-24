package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class Translatable {
	private String key;
	private Object[] args;
	private boolean stripColors;
	private List<Object> appended;
	private Locale locale;
	
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
		appendInternal(append);
		return this;
	}
	
	public Translatable append(Component append) {
		appendInternal(append);
		return this;
	}
	
	private void appendInternal(Object toAppend) {
		if (appended == null)
			appended = new ArrayList<>();
		
		appended.add(toAppend);
	}
	
	public Translatable locale(Locale locale) {
		this.locale = locale;
		return this;
	}
	
	public Translatable locale(Resident resident) {
		this.locale = Translation.getLocale(resident);
		return this;
	}
	
	public Translatable locale(CommandSender commandSender) {
		this.locale = Translation.getLocale(commandSender);
		return this;
	}
	
	public String translate(@Nullable Locale locale) {
		if (locale == null)
			return translate();

		checkArgs(locale);
		String translated = args == null ? Translation.of(key, locale) : Translation.of(key, locale, args);
		translated += getAppendString();
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public String translate() {
		if (this.locale != null)
			return translate(this.locale);

		checkArgs(null);
		String translated = args == null ? Translation.of(key) : Translation.of(key, args);
		translated += getAppendString();
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public String forLocale(Resident resident) {
		return translate(Translation.getLocale(resident));
	}
	
	public String forLocale(CommandSender sender) {
		return translate(Translation.getLocale(sender));
	}
	
	public String defaultLocale() {
		return translate(Translation.getDefaultLocale());
	}
	
	public Component component(@Nullable Locale locale) {
		if (locale == null)
			return component();

		checkArgsComponent(locale);
		Component parsed = TownyComponents.miniMessage(translate(locale));
		parsed.append(getAppendedComponent());
		
		return stripColors ? Colors.strip(parsed) : parsed;
	}
	
	public Component component() {
		if (this.locale != null)
			return component(locale);

		checkArgsComponent(null);
		Component parsed = TownyComponents.miniMessage(translate());
		parsed.append(getAppendedComponent());

		return stripColors ? Colors.strip(parsed) : parsed;
	}

	private void checkArgs(@Nullable Locale locale) {
		if (args == null)
			return;
		
		for (int i = 0; i < args.length; i++)
			if (args[i] instanceof Translatable)
				args[i] = ((Translatable) args[i]).translate(locale);
	}
	
	private void checkArgsComponent(@Nullable Locale locale) {
		if (args == null)
			return;
		
		for (int i = 0; i < args.length; i++)
			if (args[i] instanceof Translatable)
				args[i] = ((Translatable) args[i]).component(locale);
	}
	
	private String getAppendString() {
		if (appended == null || appended.isEmpty())
			return "";
		
		StringBuilder appendedString = new StringBuilder();
		for (Object object : appended) {
			if (object instanceof String string)
				appendedString.append(string);
			else if (object instanceof Component component)
				appendedString.append(TownyComponents.toLegacy(component));
		}
		
		return appendedString.toString();
	}
	
	private Component getAppendedComponent() {
		if (appended == null || appended.isEmpty())
			return Component.empty();
		
		Component appendedComponent = Component.empty();
		for (Object object : appended) {
			if (object instanceof String string)
				appendedComponent.append(TownyComponents.miniMessage(string));
			else if (object instanceof Component component)
				appendedComponent.append(component);				
		}
		
		return appendedComponent;
	}
	
	@Override
	public String toString() {
		return translate();
	}
}
