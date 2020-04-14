package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.LoadHandler;
import com.palmergames.bukkit.towny.database.handler.LoadContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

public class BaseTypeHandlers {
	public static final LoadHandler<Integer> INTEGER_HANDLER = new LoadHandler<Integer>() {
		@Override
		public Integer loadString(LoadContext context, String str) {
			return Integer.parseInt(str);
		}

		@Override
		public Integer loadSQL(LoadContext context, Object result) {
			return (Integer) result;
		}
	};
	
	public static final LoadHandler<String> STRING_HANDLER = new LoadHandler<String>() {
		@Override
		public String loadString(LoadContext context, String str) {
			return str;
		}

		@Override
		public String loadSQL(LoadContext context, Object result) {
			return (String)result;
		}
	};
	
	public static final LoadHandler<UUID> UUID_HANDLER = new LoadHandler<UUID>() {
		@Override
		public UUID loadString(LoadContext context, String str) {
			return UUID.fromString(str);
		}

		@Override
		public UUID loadSQL(LoadContext context, Object result) {
			
			// Cast to string.
			String uuidStr = (String)result;
			
			// Initialize it.
			return UUID.fromString(uuidStr);
		}
	};
	
	public static final LoadHandler<List<String>> STRING_LIST_HANDLER = new LoadHandler<List<String>>() {
		@Override
		public List<String> loadString(LoadContext context, String str) {
			
			String strCopy = str.replace("[", "");
			strCopy = strCopy.replace("]", "");
			
			String[] elements = strCopy.split(", ");
			
			return new ArrayList<>(Arrays.asList(elements));
		}

		@Override
		public List<String> loadSQL(LoadContext context, Object result) {
			return null;
		}
	};
}
