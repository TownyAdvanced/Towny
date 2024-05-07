package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.util.DrawUtil;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.Arrays;
import java.util.function.Consumer;

public class CellBorder extends WorldCoord {

	public boolean[] border;

	public enum Section {
		N(Type.WALL), NE(Type.CORNER), E(Type.WALL), SE(Type.CORNER), S(
				Type.WALL), SW(Type.CORNER), W(Type.WALL), NW(Type.CORNER);

		public enum Type {
			WALL, CORNER
		}

		private final Type type;

		Section(Type type) {

			this.type = type;
		}

		public Type getType() {

			return type;
		}

		public static int numParts() {

			return values().length;
		}
	}

	public CellBorder(WorldCoord worldCoord, boolean[] border) {

		super(worldCoord);
		this.border = border;
	}

	public void setBorderAt(CellBorder.Section s, boolean b) {

		border[s.ordinal()] = b;
	}

	public boolean hasBorderAt(CellBorder.Section s) {

		return border[s.ordinal()];
	}

	public boolean[] getBorder() {

		return border;
	}

	public boolean hasAnyBorder() {

		for (boolean b : border)
			if (b)
				return true;
		return false;
	}

	public int getBlockX() {

		return getX() * getCellSize();
	}

	public int getBlockZ() {

		return getZ() * getCellSize();
	}

	public void runBorderedOnSurface(int wallHeight, int cornerHeight, Consumer<Location> locationConsumer) {

		int x = getBlockX(); // positive x is east, negative x is west
		int z = getBlockZ(); // positive z is south, negative z is north
		int w = Coord.getCellSize() - 1;
		World world = getBukkitWorld();

		for (Section section : Section.values()) {
			if (border[section.ordinal()]) {
				if ((section.getType() == Section.Type.WALL && wallHeight > 0) || section.getType() == Section.Type.CORNER && cornerHeight > 0) {
					switch (section) {
					case N:
						DrawUtil.runOnSurface(world, x, z, x, z + w, wallHeight, locationConsumer);
						break;
					case NE:
						DrawUtil.runOnSurface(world, x, z, x, z, cornerHeight, locationConsumer);
						break;
					case E:
						DrawUtil.runOnSurface(world, x, z, x + w, z, wallHeight, locationConsumer);
						break;
					case SE:
						DrawUtil.runOnSurface(world, x + w, z, x + w, z, cornerHeight, locationConsumer);
						break;
					case S:
						DrawUtil.runOnSurface(world, x + w, z, x + w, z + w, wallHeight, locationConsumer);
						break;
					case SW:
						DrawUtil.runOnSurface(world, x + w, z + w, x + w, z + w, cornerHeight, locationConsumer);
						break;
					case W:
						DrawUtil.runOnSurface(world, x, z + w, x + w, z + w, wallHeight, locationConsumer);
						break;
					case NW:
						DrawUtil.runOnSurface(world, x, z + w, x, z + w, cornerHeight, locationConsumer);
						break;
					default:
					}
				}
			}
		}
	}

	@Override
	public String toString() {

		return super.toString() + Arrays.toString(getBorder());
	}
}