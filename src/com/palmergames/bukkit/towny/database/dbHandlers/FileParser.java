package com.palmergames.bukkit.towny.database.dbHandlers;

import com.palmergames.bukkit.towny.database.handler.DatabaseHandler;
import com.palmergames.bukkit.towny.database.type.TypeAdapter;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.HashMap;

public class FileParser {

	public HashMap<Type, TypeAdapter<?>> loadHandlers = new HashMap<>();
	public HashMap<Field, Method> setters = new HashMap<>();
	public DatabaseHandler handler;
}
