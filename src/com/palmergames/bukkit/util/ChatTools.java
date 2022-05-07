package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.text.Component;

import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.map.MinecraftFont;
import org.jetbrains.annotations.ApiStatus;

/**
 * Useful function for use with the Minecraft Server chatbox.
 * 
 * @version 2.0
 * @author Shade (xshade.ca)
 * 
 */

public class ChatTools {
	private static final MinecraftFont font = new MinecraftFont();
	private static final int DEFAULT_CHAT_WIDTH = 320;
	private static final int SPACE_WIDTH = 4;
	private static final int UNDERSCORE_WIDTH = 6;
	// Padding used for the main title formatting
	private static final String WIDGET = ".oOo.";
	private static final int WIDGET_WIDTH = 22;
	
	// Padding used for subtitle formatting
	private static final String SUBWIDGET = " .]|[. ";
	
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

	/**
	 * @deprecated Deprecated, use {@link Colors#strip(String)} instead.
	 */
	@Deprecated
	@ApiStatus.ScheduledForRemoval
	public static String stripColour(String s) {
		return Colors.strip(s);
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

	public static Component formatTitle(Component title) {
		title = TownyComponents.miniMessage(".[ " + Translation.of("status_title_secondary_colour")
			+ TownyComponents.unMiniMessage(title)
			+ Translation.of("status_title_primary_colour") + " ]."
		);
		
		return centerComponent(title, WIDGET, '_', UNDERSCORE_WIDTH);
	}

	public static Component formatSubTitle(Component subTitle) {
		return centerComponent(subTitle, SUBWIDGET, ' ', SPACE_WIDTH);
	}
	
	public static Component centerComponent(Component title, String sidePadding, char paddingChar, float paddingWidth) {
		if (!font.isValid(TownyComponents.plain(title)))
			return legacyFormatTitle(title);
		
		float widthToPad = (DEFAULT_CHAT_WIDTH - (WIDGET_WIDTH * 2) - (float) font.getWidth(TownyComponents.plain(title))) / 2;

		if (paddingWidth * 2 > widthToPad)
			return TownyComponents.prependMiniMessage(title, Translation.of("status_title_primary_colour"));

		StringBuilder paddingBuilder = new StringBuilder();
		for (float i = paddingWidth; i < widthToPad; i += paddingWidth)
			paddingBuilder.append(paddingChar);

		String padding = paddingBuilder.toString();
		String primaryColour = Translation.of("status_title_primary_colour");
		
		return TownyComponents.miniMessage(primaryColour + sidePadding  + padding + TownyComponents.unMiniMessage(title) + primaryColour + padding + sidePadding);
	}

	private static Component legacyFormatTitle(Component title) {
		String line = ".oOo.__________________________________________________.oOo.";
		
		String legacy = TownyComponents.toLegacy(title);
		if (legacy.length() > line.length())
			legacy = legacy.substring(0, line.length());
		
		int pivot = line.length() / 2;
		String center = legacy;
		// minimessage -> component -> legacy feels wrong
		String out = TownyComponents.toLegacy(TownyComponents.miniMessage(Translation.of("status_title_primary_colour"))) + line.substring(0, Math.max(0, (pivot - center.length() / 2)));
		
		out += center + line.substring(pivot + center.length() / 2);
		return TownyComponents.fromLegacy(out);
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
