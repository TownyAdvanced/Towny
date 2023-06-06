package com.palmergames.bukkit.towny.command.cloud.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import io.leangen.geantyref.TypeToken;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class TownArgument extends CommandArgument<CommandSender, Town> {
	private TownArgument(boolean required, @NonNull String name, @NonNull String defaultValue, @NonNull ArgumentDescription defaultDescription) {
		super(required, name, TownParser.INSTANCE, defaultValue, TypeToken.get(Town.class), TownParser.INSTANCE::suggestions, defaultDescription, Collections.emptyList());
	}
	
	public static TownArgument of(@NotNull String name) {
		return new TownArgument(true, name, "", ArgumentDescription.empty());
	}
	
	public static TownArgument of(@NotNull String name, @NotNull ArgumentDescription description) {
		return new TownArgument(true, name, "", description);
	}

	public static TownArgument optional(@NotNull String name) {
		return new TownArgument(false, name, "", ArgumentDescription.empty());
	}

	public static TownArgument optional(@NotNull String name, @NotNull ArgumentDescription description) {
		return new TownArgument(false, name, "", description);
	}

	private static class TownParser implements ArgumentParser<CommandSender, Town> {
		private static final TownParser INSTANCE = new TownParser();
		
		@Override
		public @NonNull ArgumentParseResult<@NonNull Town> parse(@NonNull CommandContext<@NonNull CommandSender> context, @NonNull Queue<@NonNull String> inputQueue) {
			final String input = inputQueue.peek();
			if (input == null)
				return ArgumentParseResult.failure(new NoInputProvidedException(ResidentArgument.class, context));

			final Town town = TownyUniverse.getInstance().getTown(input);
			if (town == null)
				return ArgumentParseResult.failure(new NotRegisteredException(Translatable.of("msg_err_town_unknown", input).forLocale(context.getSender())));

			inputQueue.remove();
			return ArgumentParseResult.success(town);
		}

		@Override
		public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<CommandSender> commandContext, @NonNull String input) {
			return TownyUniverse.getInstance().getTownsTrie().getStringsFromKey(input);
		}
	}
}
