package com.palmergames.bukkit.wallgen;

import org.bukkit.Location;

public class WallSection {
	private int rotation, type;
	private Location point;

	public WallSection(Location point, int rotation, int type) {
		this.setPoint(point);
		this.setRotation(rotation);
		this.setType(type);
	}

	public void setPoint(Location point) {
		this.point = point;
	}

	public Location getPoint() {
		return point;
	}

	public void setRotation(int rotation) {
		this.rotation = rotation;
	}

	public int getRotation() {
		return rotation;
	}

	public void setType(int type) {
		this.type = type;
	}

	public int getType() {
		return type;
	}
}