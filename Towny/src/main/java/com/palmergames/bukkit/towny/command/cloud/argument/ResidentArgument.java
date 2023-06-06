package com.palmergames.bukkit.towny.command.cloud.argument;

import cloud.commandframework.ArgumentDescription;
import cloud.commandframework.arguments.CommandArgument;
import cloud.commandframework.arguments.parser.ArgumentParseResult;
import cloud.commandframework.arguments.parser.ArgumentParser;
import cloud.commandframework.context.CommandContext;
import cloud.commandframework.exceptions.parsing.NoInputProvidedException;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import io.leangen.geantyref.TypeToken;
import org.bukkit.command.CommandSender;
import org.checkerframework.checker.nullness.qual.NonNull;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;
import java.util.Queue;

public class ResidentArgument extends CommandArgument<CommandSender, Resident> {
	private ResidentArgument(boolean required, @NonNull String name, @NonNull String defaultValue, @NonNull ArgumentDescription defaultDescription) {
		super(required, name, ResidentParser.INSTANCE, defaultValue, TypeToken.get(Resident.class), ResidentParser.INSTANCE::suggestions, defaultDescription, Collections.emptyList());
	}

	public static ResidentArgument of(@NotNull String name) {
		return new ResidentArgument(true, name, "", ArgumentDescription.empty());
	}

	public static ResidentArgument of(@NotNull String name, @NotNull ArgumentDescription description) {
		return new ResidentArgument(true, name, "", description);
	}

	public static ResidentArgument optional(@NotNull String name) {
		return new ResidentArgument(false, name, "", ArgumentDescription.empty());
	}

	public static ResidentArgument optional(@NotNull String name, @NotNull ArgumentDescription description) {
		return new ResidentArgument(false, name, "", description);
	}
	
	private static class ResidentParser implements ArgumentParser<CommandSender, Resident> {
		private static final ResidentParser INSTANCE = new ResidentParser();
		
		@Override
		public @NonNull ArgumentParseResult<@NonNull Resident> parse(@NonNull CommandContext<@NonNull CommandSender> context, @NonNull Queue<@NonNull String> inputQueue) {
			final String input = inputQueue.peek();
			if (input == null)
				return ArgumentParseResult.failure(new NoInputProvidedException(ResidentArgument.class, context));

			final Resident resident = TownyUniverse.getInstance().getResident(input);
			if (resident == null)
				return ArgumentParseResult.failure(new NotRegisteredException(Translatable.of("msg_err_resident_unknown").forLocale(context.getSender())));

			inputQueue.remove();
			return ArgumentParseResult.success(resident);
		}

		@Override
		public @NonNull List<@NonNull String> suggestions(@NonNull CommandContext<CommandSender> context, @NonNull String input) {
			return TownyUniverse.getInstance().getResidentsTrie().getStringsFromKey(input);
		}
	}
}
