package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.util.Colors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Translatable {
	private String key;
	private Object[] args;
	private boolean stripColors;
	private final List<Object> appended = new ArrayList<>(0);
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
	
	public String appended() {
		StringBuilder appended = new StringBuilder();

		for (Object object : this.appended) {
			if (object instanceof String string)
				appended.append(string);
			else if (object instanceof Translatable translatable)
				appended.append(translatable.locale(this.locale).translate());
			else if (object instanceof Component component)
				appended.append(LegacyComponentSerializer.legacySection().serialize(component));
		}

		return appended.toString();
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
		appended.add(append);
		return this;
	}
	
	public Translatable append(Component append) {
		appended.add(append);
		return this;
	}
	
	public Translatable append(Translatable translatable) {
		appended.add(translatable);
		return this;
	}

	public Translatable locale(@Nullable Locale locale) {
		this.locale = locale;
		return this;
	}

	public Translatable locale(@NotNull Resident resident) {
		this.locale = Translation.getLocale(resident);
		return this;
	}
	
	public Translatable locale(@NotNull CommandSender commandSender) {
		this.locale = Translation.getLocale(commandSender);
		return this;
	}
	
	public String translate(@NotNull Locale locale) {
		this.locale = locale;
		return translate();
	}
	
	public String translate() {
		translateArgs(this.locale);
		
		String translated;
		if (args == null)
			translated = locale == null ? Translation.of(key) : Translation.of(key, locale);
		else 
			translated = locale == null ? Translation.of(key, args) : Translation.of(key, locale, args);
		
		translated += appended();
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public Component component(@NotNull Locale locale) {
		this.locale = locale;
		return component();
	}
	
	public Component component() {
		return LegacyComponentSerializer.legacySection().deserialize(translate());
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

	private void translateArgs(@Nullable Locale locale) {
		if (args == null)
			return;
		
		for (int i = 0; i < args.length; i++)
			if (args[i] instanceof Translatable)
				args[i] = ((Translatable) args[i]).locale(locale).translate();
	}
	
	@Override
	public String toString() {
		return "Translatable{" +
			"key='" + key + '\'' +
			", args=" + Arrays.toString(args) +
			", stripColors=" + stripColors +
			", appended=" + appended() +
			", locale=" + locale +
			'}';
	}
}
