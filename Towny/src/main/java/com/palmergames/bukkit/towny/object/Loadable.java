package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.jetbrains.annotations.ApiStatus.Internal;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.metadata.DataFieldIO;
import com.palmergames.util.JavaUtil;

@Internal
public abstract class Loadable {

	protected TownBlock parseTownBlockFromDB(String input) throws NumberFormatException, NotRegisteredException {
		String[] tokens = input.split(getSplitter(input));
		final World world = parseWorld(tokens[0]);

		if (world == null) {
			throw new NotRegisteredException("TownBlock tried to load an invalid world! World name: " + tokens[0]);
		}

		TownyUniverse universe = TownyUniverse.getInstance();
		return universe.getTownBlock(new WorldCoord(world.getName(), Integer.parseInt(tokens[1].trim()), Integer.parseInt(tokens[2].trim())));
	}
	
	private @Nullable World parseWorld(final String keyOrName) {
		NamespacedKey worldKey = NamespacedKey.fromString(keyOrName);
		World world = null;

		if (worldKey != null) {
			world = Bukkit.getWorld(worldKey);
		}

		if (world == null) {
			world = Bukkit.getWorld(keyOrName); // Legacy worlds used Name instead of Key.
		}

		return world;
	}

	@Nullable
	protected Town parseTownFromDB(String line) {
		final UUID townUUID = JavaUtil.parseUUIDOrNull(line);
		TownyUniverse universe = TownyUniverse.getInstance();

		Town town = null;
		if (townUUID != null && universe.hasTown(townUUID))
			town = universe.getTown(townUUID);
		else if (universe.hasTown(line))
			town = universe.getTown(line);
		else if (universe.getReplacementNameMap().containsKey(line))
			town = universe.getTown(universe.getReplacementNameMap().get(line));

		return town;
	}

	@Nullable
	protected Nation parseNationFromDB(String line) {
		final UUID nationUUID = JavaUtil.parseUUIDOrNull(line);
		TownyUniverse universe = TownyUniverse.getInstance();

		Nation nation = null;
		if (nationUUID != null && universe.hasNation(nationUUID))
			nation = universe.getNation(nationUUID);
		else if (universe.hasNation(line))
			nation = universe.getNation(line);
		else if (universe.getReplacementNameMap().containsKey(line))
			nation = universe.getNation(universe.getReplacementNameMap().get(line));

		return nation;
	}

	@Nullable
	protected Location parseSpawnLocationFromDB(String raw) {
		String[] tokens = raw.split(getSplitter(raw));
		if (tokens.length >= 4)
			try {
				final World world = parseWorld(tokens[0]);
				if (world == null) {
					return null;
				}

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

	@Nullable
	protected List<Resident> parseResidentsFromDB(String line) {
		List<Resident> residents = new ArrayList<>();
		try {
			residents = TownyAPI.getInstance().getResidents(toUUIDArray(line.split(getSplitter(line))));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			residents = TownyAPI.getInstance().getResidents(line.split(getSplitter(line)));
		}
		return residents;
	}

	@Nullable
	protected List<Town> parseTownsFromDB(String line) {
		List<Town> towns = new ArrayList<>();
		try {
			towns = TownyAPI.getInstance().getTowns(toUUIDArray(line.split(getSplitter(line))));
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			towns = TownyAPI.getInstance().getTowns(line.split(getSplitter(line)));
		}
		return towns;
	}

	@Nullable
	protected List<Nation> parseNationsFromDB(String line) {
		List<Nation> nations = new ArrayList<>();
		try {
			nations = TownyAPI.getInstance().getNations(toUUIDArray(line.split(getSplitter(line)))); 
		} catch (IllegalArgumentException e) { // Legacy DB used Names instead of UUIDs.
			nations = TownyAPI.getInstance().getNations(line.split(getSplitter(line)));
		}
		return nations;
	}

	protected String getOrDefault(Map<String, String> keys, String key, String string) {
		return Objects.requireNonNullElse(keys.get(key), string);
	}

	protected boolean getOrDefault(Map<String, String> keys, String key, boolean bool) {
		if (!keys.containsKey(key))
			return bool;
		String value = keys.get(key);
		if (value.equalsIgnoreCase("true") || value.equalsIgnoreCase("false"))
			return Boolean.parseBoolean(value);
		return value.equalsIgnoreCase("1");
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
			} catch (Exception ignored) {}
		return mats;
	}

	/**
	 * Legacy SQL DB used # instead of ,.
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
		@NotNull NamespacedKey worldKey = tb.getWorld().getBukkitWorld().getKey();
		return worldKey + "," + tb.getX() + "," + tb.getZ();
	}

	protected String parseLocationForSaving(Location loc) {
		@NotNull NamespacedKey worldKey = loc.getWorld().getKey();
		return String.format("%s,%s,%s,%s,%s,%s", worldKey, loc.getX(), loc.getY(), loc.getZ(), loc.getPitch(), loc.getYaw());
	}

	protected List<UUID> toUUIDList(Collection<Resident> residents) {
		return residents.stream().filter(Resident::hasUUID).map(Resident::getUUID).collect(Collectors.toList());
	}
}
