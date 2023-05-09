package com.palmergames.bukkit.towny.object.metadata;

import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;

/**
 * @author LlmDL
 * @since 0.98.4.4
 */
public class LocationDataField extends CustomDataField<Location> {

	public LocationDataField(String key) {
		super(key);
	}

	public LocationDataField(String key, Location location) {
		super(key, location);
	}

	public LocationDataField(String key, Location location, String label) {
		super(key, location, label);
	}

	@Override
	public String getTypeID() {
		return typeID();
	}

	public static String typeID() {
		return "towny_locationdf";
	}

	@Override
	protected String serializeValueToString() {
		return getValue().getWorld().getUID() + "," + getValue().getX() + "," + getValue().getY() + ","
				+ getValue().getZ() + "," + getValue().getPitch() + "," + getValue().getYaw() + ",";
	}

	@Override
	public void setValueFromString(String strValue) {
		Location location = null;
		String[] tokens = strValue.split(",");
		if (tokens.length >= 4)
			try {
				location = new Location(Bukkit.getWorld(UUID.fromString(tokens[0])), Double.parseDouble(tokens[1]),
						Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3]));
				if (tokens.length == 6) {
					location.setPitch(Float.parseFloat(tokens[4]));
					location.setYaw(Float.parseFloat(tokens[5]));
				}
			} catch (NumberFormatException | NullPointerException ignored) {
			}
		setValue(location);
	}

	@Override
	protected String displayFormattedValue() {
		return getValue().getWorld().getName() + "," + getValue().getX() + "," + getValue().getY() + ","
				+ getValue().getZ() + "," + getValue().getPitch() + "," + getValue().getYaw();
	}

	@Override
	public @NotNull CustomDataField<Location> clone() {
		return new LocationDataField(getKey(), getValue(), this.label);
	}

}
