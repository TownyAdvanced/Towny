package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class BaseTypeHandlers {
	public static final LoadHandler<Integer> INTEGER_HANDLER = Integer::parseInt;
	
	public static final LoadHandler<String> STRING_HANDLER = (str) -> str;
	
	public static final LoadHandler<List<String>> STRING_LIST_HANDLER = (str) -> {
		
		String strCopy = str.replace("[", "")
							.replace("]", "");
		
		String[] elements = strCopy.split(", ");
		
		return new ArrayList<>(Arrays.asList(elements));
	};
}
