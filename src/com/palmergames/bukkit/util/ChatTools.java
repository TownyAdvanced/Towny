package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.bukkit.map.MinecraftFont;

import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translation;

/**
 * Useful function for use with the Minecraft Server chatbox.
 * 
 * @version 2.0
 * @author Shade (xshade.ca)
 * 
 */

public class ChatTools {
	final static int MAX_FONT_WIDTH = 321; // Two pixels less than the actual max width.
	final static int SPACE_WIDTH = 4;
	final static int UNDERSCORE_WIDTH = 6;
	final static int WIDGET_WIDTH = 22;
	final static String WIDGET = ".oOo.";
	final static int SUBWIDGET_WIDTH = 22;
	final static String SUBWIDGET = " .]|[. ";
	
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

		StringBuilder out = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			String c = s.substring(i, i + 1);
			if (c.equals("\u00A7"))
				i += 1;
			else
				out.append(c);
		}
		return out.toString();
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
		final MinecraftFont font = new MinecraftFont();
		title = ".[ " + Translation.of("status_title_secondary_colour") + title + Translation.of("status_title_primary_colour") + " ].";
		// Some language characters do not like being measured with the mojang font.
		if (!font.isValid(title))
			return legacyFormatTitle(title);
		// Max width - widgetx2 (already padded with an extra 1px) - title - 2 (1px before and after the title.) 
		int remainder = MAX_FONT_WIDTH - (WIDGET_WIDTH * 2) - font.getWidth(Colors.strip(title)) - 2;
		if (remainder < 1)
			return Translation.of("status_title_primary_colour") + title;
		if (remainder < 14)
			return Translation.of("status_title_primary_colour") + WIDGET + title + WIDGET;
		
		int times = remainder / (UNDERSCORE_WIDTH * 2);
		return Translation.of("status_title_primary_colour") + WIDGET + repeatChar(times, "_") + title + repeatChar(times, "_") + WIDGET;
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
		final MinecraftFont font = new MinecraftFont();
		// Some language characters do not like being measured with the mojang font.
		if (!font.isValid(subtitle))
			return legacyFormatSubtitle(subtitle);
		// Max width - widgetx2 (already padded with an extra 1px) - title - 2 (1px before and after the title.) 
		int remainder = MAX_FONT_WIDTH - (SUBWIDGET_WIDTH * 2) - font.getWidth(Colors.strip(subtitle)) - 2;
		if (remainder < 1)
			return Translation.of("status_title_primary_colour") + subtitle;
		if (remainder < 10)
			return Translation.of("status_title_primary_colour") + SUBWIDGET+ subtitle + Translation.of("status_title_primary_colour") + SUBWIDGET;

		int times = remainder / (SPACE_WIDTH * 2);
		return Translation.of("status_title_primary_colour") + SUBWIDGET + repeatChar(times, " ") + subtitle + repeatChar(times, " ") + Translation.of("status_title_primary_colour")  + SUBWIDGET;
	}

	private static String legacyFormatSubtitle(String subtitle) {
		String line = " .]|[.                                                                     .]|[.";
		int pivot = line.length() / 2;
		String center = subtitle + Translation.of("status_title_primary_colour");
		String out = Translation.of("status_title_primary_colour") + line.substring(0, Math.max(0, (pivot - center.length() / 2)));
		out += center + line.substring(pivot + center.length() / 2);
		return out;	
	}
	
	private static String repeatChar(int num, String character) {
		String output = "";
		for (int i = 0; i < num; i++)
			output += character;
		return output;
	}
	
	public static String formatCommand(String command, String subCommand, String help) {
		return formatCommand("", command, subCommand, help);
	}

	public static String formatCommand(String requirement, String command, String subCommand, String help) {

		String out = "  ";
		if (requirement.length() > 0)
			out += Colors.Rose + requirement + ": ";
		out += Colors.Blue + command;
		if (subCommand.length() > 0)
			out += " " + Colors.LightBlue + subCommand;
		if (help.length() > 0)
			out += " " + Colors.LightGray + " : " + help;
		return out;
	}

	/**
	 * @param title   - Title of the list,
	 * @param subject - Subject of the listing.
	 * @param list    - Any list that is in an order of ranking.
	 * @param page    - Already formatted TownySettings.getListPageMsg(page,total) handler.
	 * @return - Fully formatted output which should be sent to the player.
	 * @author - Articdive
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
