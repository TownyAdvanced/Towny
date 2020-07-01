package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadContext;
import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.object.Nation;
import org.apache.commons.lang.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class NationListHandler implements LoadHandler<List<Nation>> {
	@Override
	public List<Nation> loadString(LoadContext context, String str) {
		List<Nation> nations = new ArrayList<>();
		String strArr = StringUtils.substringBetween(str, "[", "]");
		String[] elements = strArr.split(", ");
		
		for (String element : elements) {
			nations.add(context.fromStoredString(element, Nation.class));
		}
		
		return nations;
	}
}
