package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownySettings;

public class TownBlock {
	// TODO: Admin only or possibly a group check
	// private List<Group> groups;
	private TownyWorld world;
	private Town town;
	private Resident resident;
    private TownBlockType type;
	private int x, z;
	private double plotPrice = -1;
	
	//Plot level permissions
    protected TownyPermission permissions = new TownyPermission();

	public TownBlock(int x, int z, TownyWorld world) {
		this.x = x;
		this.z = z;
		this.setWorld(world);
        this.type = TownBlockType.RESIDENTIAL;
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

	public void setPlotPrice(double ForSale) {
		this.plotPrice = ForSale;

	}
	
	public double getPlotPrice() {
		return plotPrice;
	}
	
	public boolean isForSale() {
		return getPlotPrice() != -1.0;
	}
	
    public void setPermissions(String line) {
        //permissions.reset(); not needed, already done in permissions.load()
        permissions.load(line);
    }

    public TownyPermission getPermissions() {
        return permissions;
    }

    public TownBlockType getType() {
        return type;
    }

    public void setType(TownBlockType type) {
    	if (type != this.type)
    		this.permissions.reset();
        this.type = type;
        // Custom plot settings here
        switch(type) {
        case ARENA:
        	this.permissions.pvp = true;
        }
    }

    public void setType(int typeId) {
        setType(TownBlockType.lookup(typeId));
    }

    public void setType(String typeName) throws TownyException {
        if (typeName.equalsIgnoreCase("reset")) typeName = "default";
        TownBlockType type = TownBlockType.lookup(typeName);
        if (type == null)
            throw new TownyException(TownySettings.getLangString("msg_err_not_block_type"));
        setType(type);
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
	
	public boolean isWarZone() {
		return getWorld().isWarZone(getCoord());
	}
}
