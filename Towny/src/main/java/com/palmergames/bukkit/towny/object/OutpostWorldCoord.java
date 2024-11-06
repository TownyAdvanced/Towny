package com.palmergames.bukkit.towny.object;

public class OutpostWorldCoord extends WorldCoord {

	final String name;

	public OutpostWorldCoord(String name, WorldCoord coord) {
		super(coord);
		this.name = name;
	}

	public String getName() {
		return name;
	}
}
