package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownyAPI;
import org.apache.commons.lang3.Validate;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.util.NumberConversions;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;

public class Position {
	private final TownyWorld world;
	private final double x;
	private final double y;
	private final double z;
	private final float yaw;
	private final float pitch;
	
	@ApiStatus.Internal
	protected Position(TownyWorld world, double x, double y, double z, float yaw, float pitch) {
		this.world = world;
		this.x = x;
		this.y = y;
		this.z = z;
		this.yaw = yaw;
		this.pitch = pitch;
	}
	
	public TownyWorld world() {
		return this.world;
	}
	
	public double x() {
		return this.x;
	}
	
	public double y() {
		return this.y;
	}
	
	public double z() {
		return this.z;
	}
	
	public float yaw() {
		return this.yaw;
	}
	
	public float pitch() {
		return this.pitch;
	}
	
	public int blockX() {
		return NumberConversions.floor(this.x);
	}
	
	public int blockY() {
		return NumberConversions.floor(this.y);
	}
	
	public int blockZ() {
		return NumberConversions.floor(this.z);
	}
	
	public WorldCoord worldCoord() {
		return WorldCoord.parseWorldCoord(world.getName(), blockX(), blockZ());
	}
	
	public static Position positionOf(@NotNull TownyWorld world, double x, double y, double z) {
		return positionOf(world, x, y, z, 0F, 0F);
	}
	
	public static Position positionOf(@NotNull TownyWorld world, double x, double y, double z, float yaw, float pitch) {
		Validate.notNull(world, "world cannot be null");
		return new Position(world, x, y, z, yaw, pitch);
	}
	
	@NotNull
	public Location asLocation() {
		return new Location(world.getBukkitWorld(), this.x, this.y, this.z, this.yaw, this.pitch);
	}
	
	@NotNull
	public Position blockPosition() {
		return new Position(this.world, blockX(), blockY(), blockZ(), 0F, 0F);
	}
	
	public static Position ofLocation(@NotNull Location location) {
		Validate.notNull(location, "location cannot be null");
		
		final World bukkitWorld = location.getWorld();
		if (bukkitWorld == null)
			throw new IllegalArgumentException("Cannot instantiate position for a location with no associated bukkit world.");
		
		final TownyWorld world = TownyAPI.getInstance().getTownyWorld(bukkitWorld);
		if (world == null)
			throw new IllegalArgumentException("Could not find towny world for world " + bukkitWorld.getName() + ".");
		
		return new Position(world, location.getX(), location.getY(), location.getZ(), location.getYaw(), location.getPitch());
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Position position)) return false;
		
		if (Double.compare(position.x, x) != 0) return false;
		if (Double.compare(position.y, y) != 0) return false;
		if (Double.compare(position.z, z) != 0) return false;
		if (Float.compare(position.yaw, yaw) != 0) return false;
		if (Float.compare(position.pitch, pitch) != 0) return false;
		return world.getName().equals(position.world.getName());
	}

	@Override
	public int hashCode() {
		int result;
		long temp;
		result = world.hashCode();
		temp = Double.doubleToLongBits(x);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(z);
		result = 31 * result + (int) (temp ^ (temp >>> 32));
		result = 31 * result + (yaw != 0.0f ? Float.floatToIntBits(yaw) : 0);
		result = 31 * result + (pitch != 0.0f ? Float.floatToIntBits(pitch) : 0);
		return result;
	}

	@NotNull
	public String[] serialize() {
		final String[] data = new String[6];
		
		data[0] = this.world.getName();
		data[1] = String.valueOf(this.x);
		data[2] = String.valueOf(this.y);
		data[3] = String.valueOf(this.z);
		data[4] = String.valueOf(this.yaw);
		data[5] = String.valueOf(this.pitch);
		
		return data;
	}
	
	public static Position deserialize(@NotNull final String[] data) throws NumberFormatException {
		Validate.notNull(data, "data cannot be null");
		
		final TownyWorld world = TownyAPI.getInstance().getTownyWorld(data[0]);
		
		if (world == null)
			throw new IllegalArgumentException("World '" + data[0] + "' is not recognized by towny.");
		
		double x = Double.parseDouble(data[1]);
		double y = Double.parseDouble(data[2]);
		double z = Double.parseDouble(data[3]);
		float yaw = 0F;
		float pitch = 0F;
		
		if (data.length == 6) {
			yaw = Float.parseFloat(data[4]);
			pitch = Float.parseFloat(data[5]);
		}
		
		return new Position(world, x, y, z, yaw, pitch);
	}
}
