package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.Colors;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class Translatable implements ComponentLike {
	private String key;
	private final List<ComponentLike> args = new ArrayList<>();
	private boolean stripColors;
	private final List<Object> appended = new ArrayList<>(0);
	private Locale locale;
	
	private Translatable(String key) {
		this.key = key;
	}
	
	private Translatable(String key, Object... args) {
		this.key = key;
		this.args.addAll(TownyComponents.convert(args));
	}
	
	public static Translatable of(String key) {
		return translatable(key);
	}
	
	public static Translatable of(String key, Object... args) {
		return translatable(key, args);
	}
	
	public static Translatable translatable(String key) {
		return new Translatable(key);
	}
	
	public static Translatable translatable(String key, Object... args) {
		return new Translatable(key, args);
	}
	
	/**
	 * @deprecated As of TODO insert version, components and translatables can be used interchangeably so this is no longer needed.
	 */
	@Deprecated
	public static Translatable literal(String text) {
		return new LiteralTranslatable(text);
	}
	
	public String key() {
		return key;
	}
	
	public Object[] args() {
		Object[] arr = new Object[args.size()];
		
		for (int i = 0; i < args.size(); i++)
			arr[i] = args.get(i);
		
		return arr;
	}
	
	/**
	 * @return The set locale that this translatable will be translated in.
	 */
	@Nullable
	public Locale locale() {
		return this.locale;
	}
	
	public boolean stripColors() {
		return stripColors;
	}
	
	public String appended() {
		if (this.appended.isEmpty())
			return "";

		StringBuilder converted = new StringBuilder();

		for (Object object : this.appended) {
			if (object instanceof String string)
				converted.append(string);
			else if (object instanceof Translatable translatable)
				converted.append(translatable.locale(this.locale).translate());
			else if (object instanceof Component component)
				converted.append(TownyComponents.toLegacy(component));
		}

		return converted.toString();
	}
	
	public Translatable key(String key) {
		this.key = key;
		return this;
	}
	
	public Translatable args(Object... args) {
		this.args.clear();
		this.args.addAll(TownyComponents.convert(args));
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

	/**
	 * Sets the locale that will be used if {@link #translate()} or {@link #component()} is invoked without a locale.
	 * If this translatable is converted to a {@link Component} the locale will be lost however, so it is encouraged to chain this with one of the translation methods.
	 * 
	 * @see #translate(Locale) 
	 * @see #component(Locale) 
	 */
	public Translatable locale(@Nullable Locale locale) {
		this.locale = locale;
		return this;
	}

	/**
	 * Sets the locale that will be used if {@link #translate()} or {@link #component()} is invoked without a locale to the locale of the given resident.
	 * If this translatable is converted to a {@link Component} the locale will be lost however, so it is encouraged to chain this with one of the translation methods.
	 * 
	 * @see #translate(Locale)
	 * @see #component(Locale)
	 */
	public Translatable locale(@NotNull Resident resident) {
		this.locale = Translation.getLocale(resident);
		return this;
	}
	
	/**
	 * Sets the locale that will be used if {@link #translate()} or {@link #component()} is invoked without a locale to the locale of the given command sender.
	 * If this translatable is converted to a {@link Component} the locale will be lost however, so it is encouraged to chain this with one of the translation methods.
	 * 
	 * @see #translate(Locale)
	 * @see #component(Locale)
	 */
	public Translatable locale(@NotNull CommandSender commandSender) {
		this.locale = Translation.getLocale(commandSender);
		return this;
	}
	
	public String translate(@NotNull Locale locale) {
		this.locale = locale;
		return translate();
	}
	
	public String translate() {
		final Component translated = component();
		
		return this.stripColors
			? TownyComponents.plain(translated)
			: MiniMessage.miniMessage().serialize(translated);
	}
	
	public Component component(@NotNull Locale locale) {
		this.locale = locale;
		return component();
	}
	
	public Component component() {
		return Translation.render(this.asComponent(), this.locale == null ? Translation.getDefaultLocale() : this.locale);
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
	
	@Override
	public String toString() {
		// Something is causing our translatable to become a string, this is usually
		// cause for translating it and appending it to an ongoing string.
		return translate();
	}
	
	public String debug() {
		return "Translatable{" +
			"key='" + key + '\'' +
			", args=[" + this.args.stream().map(comp -> MiniMessage.miniMessage().serialize(comp.asComponent())).collect(Collectors.joining(", ")) + "]" +
			", stripColors=" + stripColors +
			", appended=" + appended() +
			", locale=" + locale +
			'}';
	}
	
	@Override
	public @NotNull Component asComponent() {
		return Component.translatable(this.key(), this.args);
	}

	/**
	 * @deprecated As of TODO insert version, components and translatables can be used interchangeably so this is no longer needed.
	 */
	@Deprecated
	private static final class LiteralTranslatable extends Translatable {

		private LiteralTranslatable(String key) {
			super(key);
		}

		private LiteralTranslatable(String key, Object... args) {
			super(key, args);
		}
		
		@Override
		public String translate() {
			return stripColors() ? Colors.strip(key() + appended()) : key() + appended();
		}
	}
}
