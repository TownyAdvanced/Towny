package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadHandler;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

public class BaseTypeHandlers {
	public static final LoadHandler<Integer> INTEGER_HANDLER = (context, str) -> Integer.parseInt(str);
	
	public static final LoadHandler<String> STRING_HANDLER = (context, str) -> str;
	
	public static final LoadHandler<UUID> UUID_HANDLER = (context, str) -> UUID.fromString(str);
	
	public static final LoadHandler<List<String>> STRING_LIST_HANDLER = (context, str) -> {
		
		String strCopy = str.replace("[", "")
							.replace("]", "");
		
		String[] elements = strCopy.split(", ");
		
		return new ArrayList<>(Arrays.asList(elements));
	};
}
