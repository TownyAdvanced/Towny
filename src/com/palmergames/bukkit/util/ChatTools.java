package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Useful function for use with the Minecraft Server chatbox.
 * 
 * @version 2.0
 * @author Shade (xshade.ca)
 * 
 */

public class ChatTools {

	public static final int lineLength = 54;

	public static List<String> listArr(String[] args) {

		return list(Arrays.asList(args));
	}

	public static List<String> listArr(String[] args, String prefix) {

		return list(Arrays.asList(args), prefix);
	}

	public static List<String> list(List<String> args) {

		return list(args, "");
	}

	public static List<String> list(List<String> args, String prefix) {

		if (args.size() > 0) {
			String line = "";
			for (int i = 0; i < args.size() - 1; i++)
				line += args.get(i) + ", ";
			line += args.get(args.size() - 1).toString();

			return color(prefix + line);
		}

		return new ArrayList<>();
	}

	public static List<String> wordWrap(String[] tokens) {

		List<String> out = new ArrayList<>();
		out.add("");

		for (String s : tokens) {
			if (stripColour(out.get(out.size() - 1)).length() + stripColour(s).length() + 1 > lineLength)
				out.add("");
			out.set(out.size() - 1, out.get(out.size() - 1) + s + " ");
		}

		return out;
	}

	public static List<String> color(String line) {

		List<String> out = wordWrap(line.split(" "));

		String c = "f";
		for (int i = 0; i < out.size(); i++) {
			if (!out.get(i).startsWith("\u00A7") && !c.equalsIgnoreCase("f"))
				out.set(i, "\u00A7" + c + out.get(i));

			for (int index = 0; index < lineLength; index++)
				try {
					if (out.get(i).substring(index, index + 1).equalsIgnoreCase("\u00A7"))
						c = out.get(i).substring(index + 1, index + 2);
				} catch (Exception e) {
				}
		}

		return out;
	}

	public static String parseSingleLineString(String str) {

		return str.replaceAll("&", "\u00A7");
	}

	public static String stripColour(String s) {

		String out = "";
		for (int i = 0; i < s.length(); i++) {
			String c = s.substring(i, i + 1);
			if (c.equals("\u00A7"))
				i += 1;
			else
				out += c;
		}
		return out;
	}

	public static String formatTitle(String title) {

		String line = ".oOo.__________________________________________________.oOo.";
		int pivot = line.length() / 2;
		String center = ".[ " + Colors.Yellow + title + Colors.Gold + " ].";
		String out = Colors.Gold + line.substring(0, Math.max(0, (pivot - center.length() / 2)));
		out += center + line.substring(pivot + center.length() / 2);
		return out;
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
