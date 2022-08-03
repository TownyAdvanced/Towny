package com.palmergames.util;

import com.google.common.base.Strings;
import com.palmergames.bukkit.towny.object.Translation;

import net.md_5.bungee.api.ChatColor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.jetbrains.annotations.NotNull;

/**
 * Useful functions related to strings, or arrays of them.
 * 
 * @author Shade (Chris H)
 * @version 1.4
 */

public class StringMgmt {

	public static final Pattern hexPattern = Pattern.compile("(?<!\\\\)(#[a-fA-F0-9]{6})");
	public static final Pattern ampersandPattern = Pattern.compile("(?<!\\\\)(&#[a-fA-F0-9]{6})");
	public static final Pattern bracketPattern = Pattern.compile("(?<!\\\\)\\{(#[a-fA-F0-9]{6})}");
	
	public static String translateHexColors(String str) {

		final Matcher hexMatcher = hexPattern.matcher(str);
		final Matcher ampMatcher = ampersandPattern.matcher(str);
		final Matcher bracketMatcher = bracketPattern.matcher(str);

		while (hexMatcher.find()) {
			String hex = hexMatcher.group();
			str = str.replace(hex, ChatColor.of(hex).toString());
		}

		while (ampMatcher.find()) {
			String hex = ampMatcher.group().replace("&", "");
			str = str.replace(hex, ChatColor.of(hex).toString());
			str = str.replace("&", "");
		}

		while (bracketMatcher.find()) {
			String hex = bracketMatcher.group().replace("{", "").replace("}", "");
			str = str.replace(hex, ChatColor.of(hex).toString());
			str = str.replace("{", "").replace("}", "");
		}

		return str;
	}

	public static String join(Collection<?> args) {
		return join(args, " ");
	}

	public static String join(Collection<?> args, String separator) {
		StringJoiner joiner = new StringJoiner(separator);
		
		for (Object o : args) {
			joiner.add(o.toString());
		}
		
		return joiner.toString();
	}

	public static String join(Object[] arr) {

		return join(arr, " ");
	}

	public static String join(Object[] arr, String separator) {

		if (arr.length == 0)
			return "";
		String out = arr[0].toString();
		for (int i = 1; i < arr.length; i++)
			out += separator + arr[i];
		return out;
	}
	
	public static String join(Map<?,?> map, String keyValSeparator, String tokenSeparator) {
		if (map.size() == 0)
			return "";
		StringBuilder sb = new StringBuilder();
		for (Map.Entry<?,?> entry : map.entrySet()) 
			sb.append(entry.getKey()).append(keyValSeparator).append(entry.getValue().toString()).append(tokenSeparator);
		
		return sb.toString();
	}

	public static String repeat(String sequence, int repetitions) {

		return Strings.repeat(sequence, repetitions);
	}
	
	public static String[] remFirstArg(String[] arr) {

		return remArgs(arr, 1);
	}

	public static String[] remLastArg(String[] arr) {

		return subArray(arr, 0, arr.length - 1);
	}

	public static String[] remArgs(String[] arr, int startFromIndex) {

		if (arr.length == 0)
			return arr;
		else if (arr.length < startFromIndex)
			return new String[0];
		else {
			String[] newSplit = new String[arr.length - startFromIndex];
			System.arraycopy(arr, startFromIndex, newSplit, 0, arr.length - startFromIndex);
			return newSplit;
		}
	}

	public static String[] subArray(String[] arr, int start, int end) {

		//assert start > end;
		//assert start >= 0;
		//assert end < args.length;
		if (arr.length == 0)
			return arr;
		else if (end < start)
			return new String[0];
		else {
			int length = end - start;
			String[] newSplit = new String[length];
			System.arraycopy(arr, start, newSplit, 0, length);
			return newSplit;
		}
	}

	/**
	 * Shortens the string to fit in the specified size.
	 * 
	 * @param str - {@link String} to trim
	 * @param length - length to trim to
	 * @return the shortened string
	 */
	public static String trimMaxLength(String str, int length) {

		if (str.length() < length)
			return str;
		else if (length > 3)
			return str.substring(0, length);
		else
			throw new UnsupportedOperationException("Minimum length of 3 characters.");
	}

	/**
	 * Shortens the string to fit in the specified size with an ellipse "..." at
	 * the end.
	 * 
	 * @param str - {@link String} to fit
	 * @param length - Length of the string, before shortening   
	 * @return the shortened string, followed with ellipses
	 */
	public static String maxLength(String str, int length) {

		if (str.length() < length)
			return str;
		else if (length > 3)
			return str.substring(0, length - 3) + "...";
		else
			throw new UnsupportedOperationException("Minimum length of 3 characters.");
	}

	public static boolean containsIgnoreCase(List<String> arr, String str) {

		for (String s : arr)
			if (s.equalsIgnoreCase(str))
				return true;
		return false;
	}
	
	/**
	 * Replaces underscores with spaces.
	 * 
	 * @param str - the string to change.
	 * @return the string with spaces replacing underscores.
	 */
	public static String remUnderscore (String str) {
		return str.replaceAll("_", " ");
	}
	
	public static String capitalize(String str) {
		if (str == null || str.isEmpty())
			return str;
		
		return str.substring(0, 1).toUpperCase() + str.substring(1);
	}
	
	/**
	 * Capitalizes the beginning of each word, accounting for underscores separating those words
	 * @param string String to capitalize.
	 * @return String with the beginning letter of each word capitalized.
	 */
	public static String capitalizeStrings(String string) {
		return Stream.of(string.split("_")).map(str -> str.substring(0, 1).toUpperCase() + str.substring(1)).collect(Collectors.joining("_"));
	}
	
	public static boolean parseOnOff(String s) throws Exception {

		if (s.equalsIgnoreCase("on"))
			return true;
		else if (s.equalsIgnoreCase("off"))
			return false;
		else
			throw new Exception(Translation.of("msg_err_invalid_input", " on/off."));
	}
	
	public static boolean isAllUpperCase(@NotNull String string) {
		if (string.isEmpty())
			return false;

		for (int i = 0; i < string.length(); i++) {
			if (!Character.isUpperCase(string.charAt(i)))
				return false;
		}
		return true;
	}
	
	public static boolean isAllUpperCase(@NotNull Collection<String> collection) {
		if (collection.isEmpty())
			return false;
		
		for (String string : collection)
			if (!isAllUpperCase(string))
				return false;
		
		return true;
	}
	
	public static List<String> addToList(List<String> list, String addition) {
		List<String> out = new ArrayList<>(list);
		out.add(addition);
		return out;
	}
	
	public static String wrap(String string, int wrapLength, String newlineString) {
		int index = 0;
		StringBuilder stringBuilder = new StringBuilder(string);

		while (index + wrapLength < stringBuilder.length() && (index = stringBuilder.lastIndexOf(" ", index + wrapLength)) != -1) {
			stringBuilder.replace(index, index + 1, newlineString);
			index += newlineString.length();
		}
		
		return stringBuilder.toString();
	}
}
