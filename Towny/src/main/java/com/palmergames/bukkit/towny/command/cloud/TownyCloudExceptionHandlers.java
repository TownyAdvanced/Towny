package com.palmergames.bukkit.towny.command.cloud;

import cloud.commandframework.exceptions.InvalidCommandSenderException;
import cloud.commandframework.minecraft.extras.MinecraftExceptionHandler;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.function.BiFunction;

public class TownyCloudExceptionHandlers {
	public static BiFunction<CommandSender, Exception, Component> INVALID_SENDER_HANDLER = (sender, exception) -> {
		final InvalidCommandSenderException ex = (InvalidCommandSenderException) exception;
		
		if (ex.getRequiredSender() == Player.class)
			return Translatable.of("default_towny_prefix").append(Translatable.of("msg_err_player_only")).component(Translation.getLocale(sender));
		else if (ex.getRequiredSender() == ConsoleCommandSender.class)
			return Translatable.of("default_towny_prefix").append(Translatable.of("msg_err_console_only")).component(Translation.getLocale(sender));
		
		return MinecraftExceptionHandler.DEFAULT_INVALID_SENDER_FUNCTION.apply(exception);
	};
	
	public static BiFunction<CommandSender, Exception, Component> NO_PERMISSION_HANDLER = (sender, e) ->
		Translatable.of("default_towny_prefix").append(Translatable.of("msg_err_command_disable")).component(Translation.getLocale(sender));
	
	public static BiFunction<CommandSender, Exception, Component> EXECUTION_EXCEPTION_HANDLER = (sender, exception) -> {
		if (exception.getCause() instanceof TownyException townyException)
			return Translatable.of("default_towny_prefix").append(TownyComponents.miniMessage(townyException.getMessage(sender))).component(Translation.getLocale(sender));
		
		return MinecraftExceptionHandler.DEFAULT_COMMAND_EXECUTION_FUNCTION.apply(exception);
	};
}
