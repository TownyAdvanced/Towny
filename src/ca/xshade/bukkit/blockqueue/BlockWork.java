package ca.xshade.bukkit.blockqueue;

import org.bukkit.World;

public class BlockWork {
	private World world;
	private int id, x, y, z;
	private byte data;
	
	public BlockWork(World world, int id, int x, int y, int z, byte data) {
		this.world = world;
		this.id = id;
		this.x = x;
		this.y = y;
		this.z = z;
		this.data = data;
	}
	
	public World getWorld() {
		return world;
	}

	public int getId() {
		return id;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public int getZ() {
		return z;
	}

	public byte getData() {
		return data;
	}
}
