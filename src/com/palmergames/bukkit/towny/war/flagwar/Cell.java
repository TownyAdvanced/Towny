package com.palmergames.bukkit.towny.war.flagwar;

import org.bukkit.Location;

import com.palmergames.bukkit.towny.object.Coord;

public class Cell {

	private String worldName;
	private int x, z;

	public Cell(String worldName, int x, int z) {

		this.worldName = worldName;
		this.x = x;
		this.z = z;
	}

	public Cell(Cell cell) {

		this.worldName = cell.getWorldName();
		this.x = cell.getX();
		this.z = cell.getZ();
	}

	public Cell(Location location) {

		this(Cell.parse(location));
	}

	public int getX() {

		return x;
	}

	public void setX(int x) {

		this.x = x;
	}

	public int getZ() {

		return z;
	}

	public void setZ(int z) {

		this.z = z;
	}

	public String getWorldName() {

		return worldName;
	}

	public void setWorldName(String worldName) {

		this.worldName = worldName;
	}

	public static Cell parse(String worldName, int x, int z) {

		int cellSize = Coord.getCellSize();
		int xresult = x / cellSize;
		int zresult = z / cellSize;
		boolean xneedfix = x % cellSize != 0;
		boolean zneedfix = z % cellSize != 0;
		return new Cell(worldName, xresult - (x < 0 && xneedfix ? 1 : 0), zresult - (z < 0 && zneedfix ? 1 : 0));
	}

	public static Cell parse(Location loc) {

		return parse(loc.getWorld().getName(), loc.getBlockX(), loc.getBlockZ());
	}

	@Override
	public int hashCode() {

		int hash = 17;
		hash = hash * 27 + (worldName == null ? 0 : worldName.hashCode());
		hash = hash * 27 + x;
		hash = hash * 27 + z;
		return hash;
	}

	@Override
	public boolean equals(Object obj) {

		if (obj == this)
			return true;
		if (!(obj instanceof Cell))
			return false;

		Cell that = (Cell) obj;
		return this.x == that.x && this.z == that.z && (this.worldName == null ? that.worldName == null : this.worldName.equals(that.worldName));
	}

	public boolean isUnderAttack() {

		return FlagWar.isUnderAttack(this);
	}

	public CellUnderAttack getAttackData() {

		return FlagWar.getAttackData(this);
	}
}
