package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Location;
import org.bukkit.World;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.metadata.DataFieldIO;
import com.palmergames.bukkit.util.BukkitTools;

public abstract class Loadable {

	protected TownBlock parseTownBlockFromDB(String input) throws NumberFormatException, NotRegisteredException {
		String[] tokens = input.split(getSplitter(input));
		TownyUniverse universe = TownyUniverse.getInstance();
		try {
			UUID uuid = UUID.fromString(tokens[0]);
			if (universe.getWorld(uuid) == null)
				throw new NotRegisteredException("TownBlock tried to load an invalid world!");
			return universe.getTownBlock(new WorldCoord(universe.getWorld(uuid).getName(), uuid, Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			if (universe.getWorld(tokens[0]) == null)
				throw new NotRegisteredException("TownBlock tried to load an invalid world!");
			return universe.getTownBlock(new WorldCoord(tokens[0], Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
		}
	}

	@Nullable
	protected List<Resident> getResidentsFromDB(String line) {
		List<Resident> residents = new ArrayList<>();
		try {
			residents = TownyAPI.getInstance().getResidents(toUUIDArray(line.split("#")));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			residents = TownyAPI.getInstance().getResidents(line.split(","));
		}
		return residents;
	}

	@Nullable
	protected List<Town> getTownsFromDB(String line) {
		List<Town> towns = new ArrayList<>();
		try {
			towns = TownyAPI.getInstance().getTowns(toUUIDArray(line.split("#")));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			towns = TownyAPI.getInstance().getTowns(line.split(","));
		}
		return towns;
	}

	@Nullable
	protected List<Nation> getNationsFromDB(String line) {
		List<Nation> nations = new ArrayList<>();
		try {
			nations = TownyAPI.getInstance().getNations(toUUIDArray(line.split("#"))); 
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			nations = TownyAPI.getInstance().getNations(line.split(","));
		}
		return nations;
	}

	@Nullable
	protected Location parseSpawnLocationFromDB(String raw) {
		String[] tokens = raw.split(getSplitter(raw));
		if (tokens.length >= 4)
			try {
				World world = null;
				try {
					world = BukkitTools.getWorld(UUID.fromString(tokens[0]));
				} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
					world = BukkitTools.getWorld(tokens[0]);
				}
				if (world == null)
					return null;
				double x = Double.parseDouble(tokens[1]);
				double y = Double.parseDouble(tokens[2]);
				double z = Double.parseDouble(tokens[3]);

				Location loc = new Location(world, x, y, z);
				if (tokens.length == 6) {
					loc.setPitch(Float.parseFloat(tokens[4]));
					loc.setYaw(Float.parseFloat(tokens[5]));
				}
				return loc;
			} catch (NumberFormatException | NullPointerException ignored) {
			}
		return null;
	}

	protected boolean getOrDefault(Map<String, String> keys, String key, boolean bool) {
		return Boolean.parseBoolean(keys.getOrDefault(key, String.valueOf(bool)));
	}

	protected long getOrDefault(Map<String, String> keys, String key, long num) {
		return Long.parseLong(keys.getOrDefault(key, String.valueOf(num)));
	}

	protected double getOrDefault(Map<String, String> keys, String key, double num) {
		return Double.parseDouble(keys.getOrDefault(key, String.valueOf(num)));
	}

	protected int getOrDefault(Map<String, String> keys, String key, int num) {
		return Integer.parseInt(keys.getOrDefault(key, String.valueOf(num)));
	}

	protected boolean hasData(String line) {
		return line != null && !line.isEmpty();
	}

	protected UUID[] toUUIDArray(String[] uuidArray) throws IllegalArgumentException {
		UUID[] uuids = new UUID[uuidArray.length];

		for (int i = 0; i < uuidArray.length; i++)
			uuids[i] = UUID.fromString(uuidArray[i]);

		return uuids;
	}

	protected List<String> toList(String string) {
		List<String> mats = new ArrayList<>();
		if (string != null)
			try {
				for (String s : string.split(getSplitter(string)))
					if (!s.isEmpty())
						mats.add(s);
			} catch (Exception ignored) {
			}
		return mats;
	}

	/**
	 * Legacy DB used , instead of #.
	 * @param raw Text from DB
	 * @return splitter character.
	 */
	protected String getSplitter(String raw) {
		return raw.contains("#") ? "#" : ",";
	}

	protected String serializeMetadata(TownyObject obj) {
		return DataFieldIO.serializeCDFs(obj.getMetadata());
	}

	protected String getTownBlockForSaving(TownBlock tb) {
		return tb.getWorld().getUUID() + "#" + tb.getX() + "#" + tb.getZ();
	}

	protected String parseLocationForSaving(Location loc) {
		return loc.getWorld().getUID() + "#" 
				+ loc.getX() + "#"
				+ loc.getY() + "#"
				+ loc.getZ() + "#"
				+ loc.getPitch() + "#"
				+ loc.getYaw();
	}

	protected List<UUID> toUUIDList(Collection<Resident> residents) {
		return residents.stream().filter(Resident::hasUUID).map(Resident::getUUID).collect(Collectors.toList());
	}
}
