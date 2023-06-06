package com.palmergames.bukkit.towny.command.cloud;

import cloud.commandframework.execution.postprocessor.CommandPostprocessingContext;
import cloud.commandframework.execution.postprocessor.CommandPostprocessor;
import cloud.commandframework.execution.preprocessor.CommandPreprocessingContext;
import cloud.commandframework.execution.preprocessor.CommandPreprocessor;
import cloud.commandframework.services.types.ConsumerService;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class SafeModePreProcessor implements CommandPreprocessor<CommandSender> {
	private static final Set<String> SAFEMODE_BYPASS_COMMANDS = new HashSet<>(Arrays.asList("townyadmin", "ta"));
	private final Towny plugin;
	
	public SafeModePreProcessor(Towny plugin) {
		this.plugin = plugin;
	}
	
	@Override
	public void accept(@NonNull CommandPreprocessingContext<CommandSender> context) {
		if (!plugin.isError() || context.getInputQueue().isEmpty() || SAFEMODE_BYPASS_COMMANDS.contains(context.getInputQueue().getFirst()))
			return;

		TownyMessaging.sendErrorMsg(context.getCommandContext().getSender(), Translatable.of("msg_safe_mode"));
		ConsumerService.interrupt();
	}
}
