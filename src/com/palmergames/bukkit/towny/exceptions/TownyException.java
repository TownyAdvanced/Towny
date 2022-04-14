package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.jetbrains.annotations.ApiStatus;

public class TownyException extends Exception {

	private static final long serialVersionUID = -6821768221748544277L;
	private Object message;

	public TownyException() {
		super("unknown");
	}

	public TownyException(String message) {
		super(message);
		this.message = message;
	}
	
	public TownyException(Translatable message) {
		super(message.translate());
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		if (message instanceof Translatable translatable)
			return translatable.translate();
		else
			return (String) message;
	}
	
	/**
	 * @deprecated Deprecated, use {@link #message(CommandSender)} instead.
	 */
	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public String getMessage(CommandSender sender) {
		if (message instanceof Translatable translatable)
			return translatable.locale(sender).forLocale(sender);
		else
			// dumb legacy
			return TownyComponents.toLegacy(TownyComponents.miniMessage((String) message));
	}
	
	public Component message(CommandSender sender) {
		if (message instanceof Translatable translatable)
			return translatable.locale(sender).component();
		else
			return TownyComponents.miniMessage((String) message);
	}
}
