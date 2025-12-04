package com.palmergames.bukkit.towny.exceptions;

import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;

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
	
	public TownyException(String message, Throwable cause) {
		super(message, cause);
		this.message = message;
	}
	
	public TownyException(Translatable message) {
		super(message.translate());
		this.message = message;
	}
	
	@Override
	public String getMessage() {
		if (message instanceof Translatable)
			return ((Translatable) message).translate();
		else
			return (String) message;
	}
	
	public String getMessage(CommandSender sender) {
		if (message instanceof Translatable)
			return ((Translatable) message).translate(Translation.getLocale(sender));
		else
			return (String) message;
	}
	
	public Component errorMessage(CommandSender sender) {
		if (message instanceof Translatable translatable) {
			return translatable.component(Translation.getLocale(sender));
		} else {
			return TownyComponents.miniMessage((String) message);
		}
	}
}
