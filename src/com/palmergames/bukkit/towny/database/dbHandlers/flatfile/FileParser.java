package com.palmergames.bukkit.towny.database.dbHandlers.flatfile;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.database.dbHandlers.TypeAdapter;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.utils.PrimitiveLoader;
import com.palmergames.bukkit.towny.utils.ReflectionUtil;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadContext;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.LoadHandler;
import com.palmergames.bukkit.towny.database.dbHandlers.flatfile.object.Handler;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

public class FileParser {

	public HashMap<Type, TypeAdapter<?>> loadHandlers = new HashMap<>();
	public HashMap<Field, Method> setters = new HashMap<>();
	public DatabaseHandler handler;
}
