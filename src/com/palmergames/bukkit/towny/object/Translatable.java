package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.Colors;

import java.util.Arrays;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.Nullable;

public class Translatable {
	private String key;
	private Object[] args;
	private boolean stripColors;
	private Component appended = Component.empty();
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
		appended = appended.append(TownyComponents.miniMessage(append));
		return this;
	}
	
	public Translatable append(Component append) {
		appended = appended.append(append);
		return this;
	}
	
	public Translatable append(Translatable translatable) {
		appended = appended.append(translatable.locale(this.locale).component());
		return this;
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
		translated += TownyComponents.toLegacy(appended);
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public String translate() {
		if (this.locale != null)
			return translate(this.locale);

		checkArgs(null);
		String translated = args == null ? Translation.of(key) : Translation.of(key, args);
		translated += TownyComponents.toLegacy(appended);
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public String forLocale(Resident resident) {
		return translate(resident.locale());
	}
	
	public String forLocale(CommandSender sender) {
		return translate(Translation.getLocale(sender));
	}
	
	public Component componentFor(Resident resident) {
		return component(resident.locale());
	}
	
	public Component componentFor(CommandSender sender) {
		return component(Translation.getLocale(sender));
	}
	
	public String defaultLocale() {
		return translate(Translation.getDefaultLocale());
	}
	
	public Component component(@Nullable Locale locale) {
		if (locale == null)
			return component();

		checkArgs(locale);
		String translated = args == null ? Translation.of(key, locale) : Translation.of(key, locale, args);
		Component parsed = TownyComponents.miniMessage(translated).append(appended);
		
		return stripColors ? Colors.strip(parsed) : parsed;
	}
	
	public Component component() {
		if (this.locale != null)
			return component(locale);

		checkArgs(null);
		String translated = args == null ? Translation.of(key) : Translation.of(key, args);
		Component parsed = TownyComponents.miniMessage(translated).append(appended);

		return stripColors ? Colors.strip(parsed) : parsed;
	}

	private void checkArgs(@Nullable Locale locale) {
		if (args == null)
			return;
		
		for (int i = 0; i < args.length; i++)
			if (args[i] instanceof Translatable)
				args[i] = ((Translatable) args[i]).translate(locale);
	}
	
	@Override
	public String toString() {
		return "Translatable{" +
			"key='" + key + '\'' +
			", args=" + Arrays.toString(args) +
			", stripColors=" + stripColors +
			", appended=" + TownyComponents.unMiniMessage(appended) +
			", locale=" + locale +
			'}';
	}
}
