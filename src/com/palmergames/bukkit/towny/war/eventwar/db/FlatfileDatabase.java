package com.palmergames.bukkit.towny.war.eventwar.db;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.WarUniverse;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.util.FileMgmt;
import com.palmergames.util.StringMgmt;

public class FlatfileDatabase {

	public static void createFolders() {
		FileMgmt.checkOrCreateFolders(
				Towny.getPlugin().getDataFolder() + File.separator + "wars",
				Towny.getPlugin().getDataFolder() + File.separator + "wars" + File.separator + "deleted"
				);
	}
	
	public static boolean loadWarList() {
		TownyMessaging.sendDebugMsg("Loading War List");
		File[] warFiles = receiveObjectFiles("wars", ".txt");
		if (warFiles == null)
			return true;
		
		for (File war : warFiles) {
			String uuid = war.getName().replace(".txt", "");
			WarUniverse.getInstance().newWarInternal(uuid);
		}
		
		return true;
	}

	/**
	 * Util method for gathering towny object .txt files from their parent folder.
	 * ex: "residents" 
	 * @param folder - Towny object folder
	 * @param extension - Extension of the filetype to receive objects from.
	 * @return files - Files from inside the residents\towns\nations folder.
	 */
	private static File[] receiveObjectFiles(String folder, String extension) {
		return new File(Towny.getPlugin().getDataFolder() + File.separator + folder).listFiles(file -> file.getName().toLowerCase().endsWith(extension));
	}
	
	public static boolean loadWars() {
		TownyMessaging.sendDebugMsg("Loading Wars");
		for (War war : WarUniverse.getInstance().getWars()) {
			if (!loadWar(war)) {
				Towny.getPlugin().getLogger().severe("Loading Error: Could not read war data: '" + war.getWarUUID() + "'.");
				return false;
			}
		}
		return true;
	}
	
	
	public static boolean loadWar(War war) {
		String line = "";
		String path = getWarFilename(war);
		File warFile = new File(path);
		if (warFile.exists() && warFile.isFile()) {
			HashMap<String, String> keys = FileMgmt.loadFileIntoHashMap(warFile);
			
			line = keys.get("name");
			if (line != null)
				war.setWarName(line);
			
			line = keys.get("type");
			if (line != null)
				war.setWarType(WarType.valueOf(line));
			
			line = keys.get("spoils");
			if (line != null)
				war.setWarSpoils(Double.valueOf(line));
			
			line = keys.get("ignoredUUIDs");
			if (line != null && !line.isEmpty()) {
				String[] uuids = line.split(",");
				List<UUID> list = new ArrayList<>();
				for (String token : uuids)
					list.add(UUID.fromString(token));
				war.getWarParticipants().setIgnoredUUIDs(list);
			}
			line = keys.get("nationsAtStart");
			if (line != null)
				war.setNationsAtStart(Integer.valueOf(line));
			line = keys.get("townsAtStart");
			if (line != null)
				war.setTownsAtStart(Integer.valueOf(line));
			line = keys.get("residentsAtStart");
			if (line != null)
				war.setResidentsAtStart(Integer.valueOf(line));
		}
		
		return true;
	}
	
	public static String getWarFilename(War war) {
		return Towny.getPlugin().getDataFolder() + File.separator + "wars" + File.separator + war.getWarUUID() + ".txt";
	}
	
	
	public boolean saveWars() {
		TownyMessaging.sendDebugMsg("Saving Wars");
		for (War war : WarUniverse.getInstance().getWars())
			saveWar(war);
		return true;
	}
	
	public static boolean saveWar(War war) {
		
		List<String> list = new ArrayList<>();
		
		list.add("name=" + war.getWarName());
		list.add("type=" + war.getWarType().name());
		list.add("spoils=" + war.getWarSpoils());
		list.add("nationsAtStart=" + war.getNationsAtStart());
		list.add("townsAtStart=" + war.getTownsAtStart());
		list.add("residentsAtStart=" + war.getResidentsAtStart());
		list.add("ignoredUUIDs=" + StringMgmt.join(war.getWarParticipants().getUUIDsToIgnore(),","));
		
//		this.queryQueue.add(new FlatFileSaveTask(list, getWarFilename(war)));
		TownyUniverse.getInstance().getDataSource().saveWar(list, getWarFilename(war));
		return true;
	}
	
	public static void deleteWar(War war) {
		File file = new File(getWarFilename(war));
		TownyUniverse.getInstance().getDataSource().deleteWar(file);
	}
}
