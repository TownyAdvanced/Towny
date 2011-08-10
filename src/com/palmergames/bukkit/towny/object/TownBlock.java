package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;

public class TownBlock {
	// TODO: Admin only or possibly a group check
	// private List<Group> groups;
	private TownyWorld world;
	private Town town;
	private Resident resident;
	private int x, z;
	private int isForSale = -1;

	public TownBlock(int x, int z, TownyWorld world) {
		this.x = x;
		this.z = z;
		this.setWorld(world);
	}

	public void setTown(Town town) {
		try {
			if (hasTown())
				this.town.removeTownBlock(this);
		} catch (NotRegisteredException e) {
		}
		this.town = town;
		try {
			town.addTownBlock(this);
		} catch (AlreadyRegisteredException e) {
		} catch (NullPointerException e) {
		}
	}

	public Town getTown() throws NotRegisteredException {
		if (!hasTown())
			throw new NotRegisteredException();
		return town;
	}

	public boolean hasTown() {
		return town != null;
	}

	public void setResident(Resident resident) {
		try {
			if (hasResident())
				this.resident.removeTownBlock(this);
		} catch (NotRegisteredException e) {
		}
		this.resident = resident;
		try {
			resident.addTownBlock(this);
		} catch (AlreadyRegisteredException e) {
		} catch (NullPointerException e) {
		}
	}

	public Resident getResident() throws NotRegisteredException {
		if (!hasResident())
			throw new NotRegisteredException();
		return resident;
	}

	public boolean hasResident() {
		return resident != null;
	}
	
	public boolean isOwner(TownBlockOwner owner) {
		try {
			if (owner == getTown())
				return true;
		} catch (NotRegisteredException e) {
		}
		
		try {
			if (owner == getResident())
				return true;
		} catch (NotRegisteredException e) {
		}
		
		return false;
	}

	public void setForSale(int ForSale) {
		this.isForSale = ForSale;

	}
	
	public int isForSale() {
		return isForSale;
	}
	
	public boolean isHomeBlock() {
		try {
			return getTown().isHomeBlock(this);
		} catch (NotRegisteredException e) {
			return false;
		}
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getX() {
		return x;
	}

	public void setZ(int z) {
		this.z = z;
	}

	public int getZ() {
		return z;
	}

	public Coord getCoord() {
		return new Coord(x, z);
	}
	
	public WorldCoord getWorldCoord() {
		return new WorldCoord(world, x, z);
	}

	public void setWorld(TownyWorld world) {
		this.world = world;
	}

	public TownyWorld getWorld() {
		return world;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this)
			return true;
		if (!(obj instanceof TownBlock))
			return false;

		TownBlock o = (TownBlock) obj;
		return this.getX() == o.getX() && this.getZ() == o.getZ()
				&& this.getWorld() == o.getWorld();
	}
	
	public void clear() {
		setTown(null);
		setResident(null);
		setWorld(null);
	}
	
	@Override
	public String toString() {
		return getWorld().getName() + " ("+getCoord()+")";
	}
}
