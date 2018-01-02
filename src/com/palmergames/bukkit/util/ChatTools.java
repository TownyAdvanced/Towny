package com.palmergames.bukkit.util;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.bukkit.ChatColor;

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

		return new ArrayList<String>();
	}
	
	/**
	 * @author Wowserman
	 * 
	 * Format a List of Strings to only have a certain amount of Characters per line.
	 * 
	 * Example of formating a List to a max charecter set of 10.
	 * 
	 * Input:
	 * 
	 * "I'm waking up, I feel it in my bones"
	 * "Enough to make my systems blow"
	 * "Welcome to the new age, to the new age"
	 * "Welcome to the new age, to the new age"
	 * 
	 * Output:
	 * 
	 * "I'm waking"
	 * "up, I feel"
	 * "in my"
	 * "bones"
	 * "Enough to"
	 * "make my"
	 * "Systems"
	 * "blow"
	 * "Welcome to"
	 * "the new"
	 * "age, to"
	 * "the new"
	 * "age"
	 * "Welcome to"
	 * "the new"
	 * "age, to"
	 * "the new"
	 * "age"
	 * 
	 * @param text Input
	 * @param maxChar Maximum Charecters a Line should have.
	 * @return Output
	 */
	public static List<String> formatList(List<String> text, int maxChar) {
		String s = "";
		for (String string:text){
			s = s.length()==0 ? string: s + " " + string;
		}
		
		return formatText(s, maxChar);
	}
	
	/**
	 * @author Wowserman
	 * 
	 * Format a List of Strings to only have a certain amount of Characters per line.
	 * 
	 * Example of formating a List to a max charecter set of 10.
	 * 
	 * Input:
	 * 
	 * "I'm waking up, I feel it in my bones Enough to make my systems blow Welcome to the new age, to the new age Welcome to the new age, to the new age"
	 * 
	 * Output:
	 * 
	 * "I'm waking"
	 * "up, I feel"
	 * "in my"
	 * "bones"
	 * "Enough to"
	 * "make my"
	 * "Systems"
	 * "blow"
	 * "Welcome to"
	 * "the new"
	 * "age, to"
	 * "the new"
	 * "age"
	 * "Welcome to"
	 * "the new"
	 * "age, to"
	 * "the new"
	 * "age"
	 * 
	 * @param text Input
	 * @param maxChar Maximum Charecters a Line should have.
	 * @return Output
	 */
	public static List<String> formatText(String text, int maxChar) {
		List<String> list = new ArrayList<String>();
		
		String[] split = text.split(" ");
		
		String currentLine = "";
		int line = 0;
		
		for (String string:split) {

			if (ChatColor.stripColor(string).length() + currentLine.length() <= maxChar) {
				currentLine = currentLine.length()==0 ? string : currentLine + " " + string;
			}
			
			else {
				line = list.add(currentLine) ? line + 1:line;
				currentLine = "";
			}
		
		}
		
		if (list.size()==0)
			list.add(currentLine);
		
		return list;
	}

	public static List<String> wordWrap(String[] tokens) {

		List<String> out = new ArrayList<String>();
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
	
	public static void main(String[] args) {

		String[] players = {
				"dude", "bowie", "blarg", "sonbitch", "songoku", "pacman",
				"link", "stacker", "hacker", "newb" };
		for (String line : ChatTools.listArr(players))
			System.out.println(line);

		String testLine = "Loren Ipsum blarg voila tssssssh, boom wakka wakka \u00A7apacman on a boat bitch. From the boat union. Beata lingiushtically \u00A71nootchie lolk erness.";
		for (String line : ChatTools.color(testLine))
			System.out.println(line);
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
		List<String> output = new ArrayList<String>();
		output.add(0, formatTitle(title));
		output.add(1, subject);
		output.addAll(list);
		output.add(page);
		return output.toArray(new String[0]);
	}
}

