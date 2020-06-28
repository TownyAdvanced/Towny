package com.palmergames.bukkit.towny.utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.UUID;
import org.bukkit.Bukkit;

public class TNUPlayerMapImporter {

	public static HashMap<String, UUID> mappedResidents = new HashMap();
	
	public static void loadTNUPlayermap() {
		
		mappedResidents.clear();
		File file = new File(Bukkit.getPluginManager().getPlugin("TownyNameUpdater").getDataFolder() + File.separator + "playermap.yml");
		String line;
		try (BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
			while ((line = reader.readLine()) != null && !line.equals("")) {
				String[] split = line.split(":");
				UUID uuid = UUID.fromString(split[0]);
				String name = split[1].trim().toLowerCase();
				mappedResidents.put(name, uuid);
			}
				
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
