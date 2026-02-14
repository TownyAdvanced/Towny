package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translation;
import net.kyori.adventure.text.Component;

/**
 * Useful function for use with the Minecraft Server chatbox.
 * 
 * @version 2.0
 * @author Shade (xshade.ca)
 * 
 */

public class ChatTools {
	private static final int DEFAULT_CHAT_WIDTH = 320;
	private static final float SPACE_WIDTH = FontUtil.measureWidth(' ');
	private static final float UNDERSCORE_WIDTH = FontUtil.measureWidth('_');
	
	// Padding used for the main title formatting
	private static final String WIDGET = ".oOo.";
	private static final float WIDGET_WIDTH = FontUtil.measureWidth(WIDGET);
	
	// Padding used for subtitle formatting
	private static final String SUBWIDGET = " .]|[. ";
	private static final float SUBWIDGET_WIDTH = FontUtil.measureWidth(SUBWIDGET);
	
	public static String listArr(String[] args, String prefix) {

		return list(Arrays.asList(args), prefix);
	}

	public static String list(Collection<String> args) {

		return list(args, "");
	}

	public static String list(Collection<String> args, String prefix) {
		if (args.isEmpty())
			return "";

		return prefix + String.join(", ", args);
	}

	public static String stripColour(String s) {
		return Colors.strip(s);
	}

	/**
	 * Formats a title for a TownyObject, taking into account that on
	 * servers with high max_name_length could end up breaking the math
	 * @param object TownyObject (town or nation)
	 * @return a title bar which won't exceed the allowed length.
	 */
	public static String formatTitle(TownyObject object) {
		
		String title = object.getFormattedName();
		if (title.length() > 51)
			title = object.getName();
		if (title.length() > 51)
			title = title.substring(0, 51);
		
		return formatTitle(title);
	}
	
	public static String formatTitle(String title) {
		title = ".[ " + Translation.of("status_title_secondary_colour") + title + Translation.of("status_title_primary_colour") + " ].";
		
		if (!FontUtil.isValidMinecraftFont(title))
			return legacyFormatTitle(title);
		
		final float width = FontUtil.measureWidth(TownyComponents.miniMessage(title));
		
		// Max width - widgetx2 (already padded with an extra 1px) - title - 2 (1px before and after the title.) 
		float remainder = DEFAULT_CHAT_WIDTH - (WIDGET_WIDTH * 2) - width - 2;
		if (remainder < 1)
			return Translation.of("status_title_primary_colour") + title;
		if (remainder < 14)
			return Translation.of("status_title_primary_colour") + WIDGET + title + WIDGET;
		
		int times = (int) Math.floor(remainder / (UNDERSCORE_WIDTH * 2));
		return Translation.of("status_title_primary_colour") + WIDGET + repeatChar(times, "_") + title + repeatChar(times, "_") + WIDGET;
	}

	public static Component formatTitle(Component title) {
		title = Translatable.literal(".[ ").append(Translatable.of("status_title_secondary_colour").append(title)).append(Translatable.of("status_title_primary_colour")).append(" ].").component();

		final float width = FontUtil.measureWidth(title);

		// Max width - widgetx2 (already padded with an extra 1px) - title - 2 (1px before and after the title.) 
		float remainder = DEFAULT_CHAT_WIDTH - (WIDGET_WIDTH * 2) - width - 2;
		if (remainder < 1)
			return Translatable.of("status_title_primary_colour").append(title).component();
		if (remainder < 14)
			return Translatable.of("status_title_primary_colour").append(WIDGET).append(title).append(WIDGET).component();

		int times = (int) Math.floor(remainder / (UNDERSCORE_WIDTH * 2));
		return Translatable.of("status_title_primary_colour").append(WIDGET).append(repeatChar(times, "_")).append(title).append(repeatChar(times, "_")).append(WIDGET).component();
	}

	private static String legacyFormatTitle(String title) {
		String line = ".oOo.__________________________________________________.oOo.";
		if (title.length() > line.length())
			title = title.substring(0, line.length());
		int pivot = line.length() / 2;
		String center = title;
		String out = Translation.of("status_title_primary_colour") + line.substring(0, Math.max(0, (pivot - center.length() / 2)));
		out += center + line.substring(pivot + center.length() / 2);
		return out;
	}

	public static String formatSubTitle(String subtitle) {
		if (!FontUtil.isValidMinecraftFont(subtitle))
			return legacyFormatSubtitle(subtitle);
		
		final float width = FontUtil.measureWidth(TownyComponents.miniMessage(subtitle));
		
		// Max width - widgetx2 (already padded with an extra 1px) - title - 2 (1px before and after the title.) 
		float remainder = DEFAULT_CHAT_WIDTH - (SUBWIDGET_WIDTH * 2) - width - 2;
		if (remainder < 1)
			return Translation.of("status_title_primary_colour") + subtitle;
		if (remainder < 10)
			return Translation.of("status_title_primary_colour") + SUBWIDGET+ subtitle + Translation.of("status_title_primary_colour") + SUBWIDGET;

		int times = (int) Math.floor(remainder / (SPACE_WIDTH * 2));
		return Translation.of("status_title_primary_colour") + SUBWIDGET + repeatChar(times, " ") + subtitle + repeatChar(times, " ") + Translation.of("status_title_primary_colour")  + SUBWIDGET;
	}

	private static String legacyFormatSubtitle(String subtitle) {
		String line = " .]|[.                                                                     .]|[.";
		int pivot = line.length() / 2;
		String center = subtitle + Translation.of("status_title_primary_colour");
		String out = Translation.of("status_title_primary_colour") + line.substring(0, Math.max(0, (pivot - center.length() / 2)));
		out += center + line.substring(Math.min(line.length(), pivot + center.length() / 2));
		return out;	
	}
	
	private static String repeatChar(int num, String character) {
		return character.repeat(num);
	}
	
	public static String formatCommand(String command, String subCommand, String help) {
		return formatCommand("", command, subCommand, help);
	}

	public static String formatCommand(String requirement, String command, String subCommand, String help) {

		String out = "  ";
		if (requirement.length() > 0)
			out += Translation.of("help_menu_requirement") + requirement + ": ";
		out += Translation.of("help_menu_command") + command;
		if (subCommand.length() > 0)
			out += " " + Translation.of("help_menu_subcommand") + subCommand;
		if (help.length() > 0)
			out += Translation.of("help_menu_explanation") + " : " + help;
		return out;
	}

	/**
	 * @param title   - Title of the list,
	 * @param subject - Subject of the listing.
	 * @param list    - Any list that is in an order of ranking.
	 * @param page    - Already formatted TownySettings.getListPageMsg(page,total) handler.
	 * @return - Fully formatted output which should be sent to the player.
	 */
	public static String[] formatList(String title, String subject, List<String> list, String page) {
		List<String> output = new ArrayList<>();
		output.add(0, formatTitle(title));
		output.add(1, subject);
		output.addAll(list);
		output.add(page);
		return output.toArray(new String[0]);
	}
}
