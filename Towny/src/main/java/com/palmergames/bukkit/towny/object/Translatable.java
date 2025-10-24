package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.Colors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.ComponentLike;
import net.kyori.adventure.text.LinearComponents;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.kyori.adventure.text.format.TextDecoration;
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
	
	public static Translatable literal(String text) {
		return new LiteralTranslatable(text);
	}

	public static Translatable literal(ComponentLike component) {
		return new LiteralTranslatable(component.asComponent());
	}
	
	public String key() {
		return key;
	}
	
	public Object[] args() {
		return args;
	}
	
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

	public Component appendedAsComponent() {
		if (this.appended.isEmpty()) {
			return Component.empty();
		}

		final List<Component> components = new ArrayList<>();

		for (Object object : this.appended) {
			if (object instanceof String string) {
				components.add(TownyComponents.miniMessage(string));
			} else if (object instanceof Translatable translatable) {
				components.add(translatable.locale(this.locale).component());
			} else if (object instanceof ComponentLike component) {
				components.add(component.asComponent());
			}
		}

		if (components.isEmpty()) {
			return Component.empty();
		} else {
			return LinearComponents.linear(components.toArray(new Component[]{}));
		}
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
	
	/*
	 * Translates the key and the args in the current locale.
	 */
	protected String translateBase() {
		translateArgs(this.locale);
		
		String translated;
		if (args == null)
			translated = locale == null ? Translation.of(key) : Translation.of(key, locale);
		else 
			translated = locale == null ? Translation.of(key, args) : Translation.of(key, locale, args);
		
		return translated;
	}

	public String translate() {
		String translated = this.translateBase() + appended();
		
		return stripColors ? Colors.strip(translated) : translated;
	}
	
	public Component component(@NotNull Locale locale) {
		this.locale = locale;
		return component();
	}
	
	public Component component() {
		final Component translated = TownyComponents.miniMessage(translateBase()).append(this.appendedAsComponent()).decorationIfAbsent(TextDecoration.ITALIC, TextDecoration.State.FALSE); // Because of item lore/names;

		if (this.stripColors) {
			return Component.text(PlainTextComponentSerializer.plainText().serialize(translated));
		} else {
			return translated;
		}
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
		// Something is causing our translatable to become a string, this is usually
		// cause for translating it and appending it to an ongoing string.
		return translate();
	}
	
	public String debug() {
		return "Translatable{" +
			"key='" + key + '\'' +
			", args=" + Arrays.toString(args) +
			", stripColors=" + stripColors +
			", appended=" + appended() +
			", locale=" + locale +
			'}';
	}
	
	private static final class LiteralTranslatable extends Translatable {
		private Component component = null;

		private LiteralTranslatable(String key) {
			super(key);
		}

		private LiteralTranslatable(Component component) {
			super(TownyComponents.toMiniMessage(component));
			this.component = component;
		}
		
		@Override
		public String translateBase() {
			return key();
		}

		@Override
		public Component component() {
			if (this.component == null) {
				return super.component();
			}

			return this.component;
		}
	}
}
