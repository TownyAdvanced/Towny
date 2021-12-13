package com.palmergames.bukkit.towny.command;


import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import net.kyori.adventure.audience.Audience;
import net.kyori.adventure.text.BuildableComponent;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.title.Title;
import org.bukkit.Bukkit;

import java.util.Locale;

public class FancyTabCompletions {
	private static final TextColor highlight = NamedTextColor.GREEN;
	private static final TextColor lightHighlight = NamedTextColor.DARK_AQUA;
	private final static int maxPreviousArgs = 2;

	private static Component getFancyCommandTabCompletion(final String command, final String[] args, final String hintCurrentArg, String hintNextArgs) {

		final StringBuilder argsTogether = new StringBuilder();


		final int initialCutoff = (args.length) - maxPreviousArgs;
		int cutoff = (args.length) - maxPreviousArgs;

		for (int i = -1; i < args.length - 1; i++) {
			if (cutoff == 0) {
				if (i == -1) {
					argsTogether.append("/").append(command).append(" ");
				} else {
					argsTogether.append(args[i]).append(" ");
				}
			} else {
				if (cutoff > 0) {
					cutoff -= 1;
				} else { //Just 1 arg
					argsTogether.append("/").append(command).append(" ");
				}
			}
		}

		if (initialCutoff > 0) {
			argsTogether.insert(0, "[...] ");
		}


		Component currentCompletion;
		if (args[args.length - 1].isEmpty()) {
			currentCompletion = Component.text("" + hintCurrentArg, highlight, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		} else {
			currentCompletion = Component.text("" + args[args.length - 1], NamedTextColor.YELLOW, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false);
		}

		if (!hintNextArgs.isEmpty() && !hintNextArgs.toLowerCase(Locale.ROOT).contains("optional")){
			//Chop off if too long
			if (hintNextArgs.length() > 15) {
				hintNextArgs = hintNextArgs.substring(0, 14) + "...";
			}

			return Component.text(argsTogether.toString(), lightHighlight, TextDecoration.ITALIC)
				.append(currentCompletion)
				.append(Component.text(" " + hintNextArgs, NamedTextColor.GRAY));
		} else {
			if(!args[args.length-1].isEmpty()){
				return Component.text(argsTogether.toString(), lightHighlight, TextDecoration.ITALIC)
					.append(currentCompletion)
					.append(Component.text(" ✓", NamedTextColor.GREEN, TextDecoration.BOLD));
			} else {
				return Component.text(argsTogether.toString(), lightHighlight, TextDecoration.ITALIC)
					.append(currentCompletion);
			}

		}

	}


	public static void sendFancyCommandCompletion(final String command, final Audience audience, final String[] args, final String hintCurrentArg, final String hintNextArgs) {
		if (!TownySettings.isActionBarTabCompletions() && !TownySettings.isTitleTabCompletions()){
			return;
		}
		final Component fancyTabCompletion = getFancyCommandTabCompletion(command, args, hintCurrentArg, hintNextArgs);
		if (TownySettings.isActionBarTabCompletions()) {
			audience.sendActionBar(fancyTabCompletion);
		}
		if (TownySettings.isTitleTabCompletions()) {
			audience.showTitle(Title.title(Component.text(""), fancyTabCompletion));
		}
	
	}
}
