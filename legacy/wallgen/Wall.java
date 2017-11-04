package com.palmergames.bukkit.wallgen;

import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Location;
import org.bukkit.Material;

import java.util.ArrayList;
import java.util.List;

public class Wall {

	private List<WallSection> wallSections = new ArrayList<WallSection>();
	private int blockType, height, walkwayHeight;

	public void clear() {

		wallSections.clear();
	}

	public int getBlockType() {

		return blockType;
	}

	public void setBlockType(int blockType) {

		this.blockType = blockType;
	}

	public int getHeight() {

		return height;
	}

	public void setHeight(int height) {

		this.height = height;
	}

	public int getWalkwayHeight() {

		return walkwayHeight;
	}

	public void setWalkwayHeight(int walkwayHeight) {

		this.walkwayHeight = walkwayHeight;
	}

	public Wall() {

		blockType = BukkitTools.getMaterialId(Material.COBBLESTONE);
		height = 2;
		walkwayHeight = 0;
	}

	public List<WallSection> getWallSections() {

		return wallSections;
	}

	public WallSection getWallSection(Location point) {

		for (WallSection wallSection : getWallSections())
			if (wallSection.getPoint().equals(point))
				return wallSection;
		return null;
	}

	public void setWallSections(List<WallSection> wallSections) {

		this.wallSections = wallSections;

	}

	public boolean hasWallSection(WallSection wallSection) {

		return wallSections.contains(wallSection);
	}

	public void addWallSection(WallSection wallSection) {

		wallSections.add(wallSection);
	}

	public void removeWallSection(WallSection wallSection) {

		wallSections.remove(wallSection);
	}
}
