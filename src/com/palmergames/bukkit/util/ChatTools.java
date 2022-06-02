package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;

import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translation;
import solar.squares.pixelwidth.PixelWidthSource;

/**
 * Useful function for use with the Minecraft Server chatbox.
 * 
 * @version 2.0
 * @author Shade (xshade.ca)
 * 
 */

public class ChatTools {
	final static PixelWidthSource source = PixelWidthSource.pixelWidth();
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
	public static Component formatTitle(TownyObject object) {
		
		String title = object.getFormattedName();
		if (title.length() > 51)
			title = object.getName();
		if (title.length() > 51)
			title = title.substring(0, 51);
		
		return formatTitle(title);
	}
	
	public static Component formatTitle(String title) {
		return formatTitle(TownyComponents.miniMessage(title));
	}

	// TODO: can probably be done better
	public static Component formatTitle(Component title) {
		title = TownyComponents.miniMessage(".[ " + Translation.of("status_title_secondary_colour")
					+ TownyComponents.unMiniMessage(title)
					+ Translation.of("status_title_primary_colour") + " ]."
				);

		// Max width - widgetx2 (already padded with an extra 1px) - title - 2 (1px before and after the title.) 
		float remainder = MAX_FONT_WIDTH - (WIDGET_WIDTH * 2) - source.width(title) - 2;
		if (remainder < 1)
			return TownyComponents.prependMiniMessage(title, Translation.of("status_title_primary_colour"));
		if (remainder < 14)
			return TownyComponents.miniMessage(Translation.of("status_title_primary_colour") + WIDGET + TownyComponents.unMiniMessage(title) + WIDGET);

		int times = (int) remainder / (UNDERSCORE_WIDTH * 2);
		return TownyComponents.miniMessage(Translation.of("status_title_primary_colour") + WIDGET + repeatChar(times, "_") + TownyComponents.unMiniMessage(title) + repeatChar(times, "_") + WIDGET);
	}

	public static Component formatSubTitle(Component subTitle) {
		// Max width - widgetx2 (already padded with an extra 1px) - title - 2 (1px before and after the title.) 
		float remainder = MAX_FONT_WIDTH - (SUBWIDGET_WIDTH * 2) - source.width(subTitle) - 2;
		if (remainder < 1)
			return TownyComponents.prependMiniMessage(subTitle, Translation.of("status_title_primary_colour"));
		if (remainder < 10)
			return TownyComponents.miniMessage(Translation.of("status_title_primary_colour") + SUBWIDGET + TownyComponents.unMiniMessage(subTitle) + Translation.of("status_title_primary_colour") + SUBWIDGET);

		int times = (int) remainder / (SPACE_WIDTH * 2);
		return TownyComponents.miniMessage(Translation.of("status_title_primary_colour") + SUBWIDGET + repeatChar(times, " ") + TownyComponents.unMiniMessage(subTitle) + repeatChar(times, " ") + Translation.of("status_title_primary_colour")  + SUBWIDGET);
	}
	
	private static String repeatChar(int num, String character) {
		String output = "";
		for (int i = 0; i < num; i++)
			output += character;
		return output;
	}
	
	public static Component formatCommand(String command, String subCommand, String help) {
		return formatCommand("", command, subCommand, help);
	}

	public static Component formatCommand(String requirement, String command, String subCommand, String help) {

		String out = "  ";
		if (requirement.length() > 0)
			out += "<red>" + requirement + ": ";
		out += "<dark_aqua>" + command;
		if (subCommand.length() > 0)
			out += " " + "<aqua>" + subCommand;
		if (help.length() > 0)
			out += " <gray> : " + help;

		return TownyComponents.miniMessage(out);
	}

	/**
	 * @param title   - Title of the list,
	 * @param subject - Subject of the listing.
	 * @param list    - Any list that is in an order of ranking.
	 * @param page    - Already formatted TownySettings.getListPageMsg(page,total) handler.
	 * @return - Fully formatted output which should be sent to the player.
	 * @author - Articdive
	 */
	public static List<Component> formatList(Component title, Component subject, List<Component> list, Component page) {
		List<Component> output = new ArrayList<>();
		output.add(formatTitle(title));
		output.add(subject);
		output.addAll(list);
		output.add(page);
		return output;
	}
}
