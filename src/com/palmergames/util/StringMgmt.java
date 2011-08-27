package com.palmergames.util;

import java.util.List;

/**
 * Useful functions related to strings, or arrays of them.
 * 
 * @author Shade (Chris H)
 * @version 1.4
 */

public class StringMgmt {
	
	@SuppressWarnings("rawtypes")
	public static String join(List arr) {
		return join(arr, " ");
	}
	
	
	@SuppressWarnings("rawtypes")
	public static String join(List arr, String separator) {
		if (arr == null || arr.size() == 0)
			return "";
		String out = arr.get(0).toString();
		for (int i = 1; i < arr.size(); i++)
			out += separator + arr.get(i);
		return out;
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
	
	public static String[] remFirstArg(String[] arr) {
		return remArgs(arr, 1);
	}
	
	public static String[] remLastArg(String[] arr) {
		return subArray(arr, 0, arr.length-1);
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
	 * Shortens the string to fit in the specified size with an elipse "..." at the end.
	 * @return the shortened string
	 */
	public static String maxLength(String str, int length) {
		if (str.length() < length)
			return str;
		else if (length > 3)
			return str.substring(0, length-3) + "...";
		else
			throw new UnsupportedOperationException("Minimum length of 3 characters.");
	}
	
	public static boolean containsIgnoreCase(List<String> arr, String str) {
		for (String s : arr)
			if (s.equalsIgnoreCase(str))
				return true;
		return false;
	}
}
