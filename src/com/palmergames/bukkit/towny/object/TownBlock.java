package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.PlotChangeOwnerEvent;
import com.palmergames.bukkit.towny.event.PlotChangeTypeEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import org.bukkit.Bukkit;

public class TownBlock {

	// TODO: Admin only or possibly a group check
	// private List<Group> groups;
	private TownyWorld world;
	private Town town;
	private Resident resident;
	private TownBlockType type;
	private String name;
	private int x, z;
	private double plotPrice = -1;
	private boolean locked = false;
	private boolean outpost = false;

	//Plot level permissions
	protected TownyPermission permissions = new TownyPermission();
	protected boolean isChanged;
	
	public TownBlock(int x, int z, TownyWorld world) {

		this.x = x;
		this.z = z;
		this.setWorld(world);
		this.resident = null;
		this.type = TownBlockType.RESIDENTIAL;
		isChanged = false;
		this.name = "";
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
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a town.", world.getName(), x, z));
		return town;
	}

	public boolean hasTown() {

		return town != null;
	}

	public void setResident(Resident resident) {
		boolean successful;
		try {
			if (hasResident())
				this.resident.removeTownBlock(this);
		} catch (NotRegisteredException e) {
		}
		this.resident = resident;
		try {
			resident.addTownBlock(this);
			successful = true;
		} catch (AlreadyRegisteredException | NullPointerException e) {
			successful = false;
		}
		if (successful && resident != null) { //Should not cause a NPE, is checkingg if resident is null and
			// if "this.resident" returns null (Unclaimed / Wilderness) the PlotChangeOwnerEvent changes it to: "undefined"
			Bukkit.getPluginManager().callEvent(new PlotChangeOwnerEvent(this.resident, resident, this));
		}
		this.resident = resident;
	}

	public Resident getResident() throws NotRegisteredException {

		if (!hasResident())
			throw new NotRegisteredException(String.format("The TownBlock at (%s, %d, %d) is not registered to a resident.", world.getName(), x, z));
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

		/*
		 * Return our perms
		 */
		return permissions;
	}

	/**
	 * Have the permissions been manually changed.
	 * 
	 * @return the isChanged
	 */
	public boolean isChanged() {

		return isChanged;
	}

	/**
	 * Flag the permissions as changed.
	 * 
	 * @param isChanged the isChanged to set
	 */
	public void setChanged(boolean isChanged) {

		this.isChanged = isChanged;
	}

	/**
	 * @return the outpost
	 */
	public boolean isOutpost() {

		return outpost;
	}

	/**
	 * @param outpost the outpost to set
	 */
	public void setOutpost(boolean outpost) {

		this.outpost = outpost;
	}

	public TownBlockType getType() {

		return type;
	}

	
	
	public void setType(TownBlockType type) {
		if (type != this.type)
			this.permissions.reset();

		if (type != null){
			Bukkit.getPluginManager().callEvent(new PlotChangeTypeEvent(this.type, type, this));
		}
		this.type = type;

		// Custom plot settings here
		switch (type) {
		
		case RESIDENTIAL:
			
			if (this.hasResident()) {
				setPermissions(this.resident.permissions.toString());
			} else {
				setPermissions(this.town.permissions.toString());
			}
			
			break;
			
		case COMMERCIAL:
			
			//setPermissions("residentSwitch,allySwitch,outsiderSwitch");
			if (this.hasResident()) {
				setPermissions(this.resident.permissions.toString());
			} else {
				setPermissions(this.town.permissions.toString());
			}
			
			break;
			
		case ARENA:
			
			setPermissions("pvp");
			break;
			
		case EMBASSY:
			
			if (this.hasResident()) {
				setPermissions(this.resident.permissions.toString());
			} else {
				setPermissions(this.town.permissions.toString());
			}
			
			break;
			
		case WILDS:
			
			if (this.hasResident()) {
				setPermissions(this.resident.permissions.toString());
			} else {
				setPermissions(this.town.permissions.toString());
			}
			
			break;
		
		case SPLEEF:
			
			setPermissions("denyAll");
			break;

		case INN:
			
			setPermissions("residentSwitch,allySwitch,outsiderSwitch");
			break;
			
		case JAIL:
			
			setPermissions("denyAll");
			break;
		
		case FARM:
			
			if (this.hasResident()) {
				setPermissions(this.resident.permissions.toString());
			} else {
				setPermissions(this.town.permissions.toString());
			}
			
			break;
		
		case BANK:
			
			if (this.hasResident()) {
				setPermissions(this.resident.permissions.toString());
			} else {
				setPermissions(this.town.permissions.toString());
			}
			break;
		}
			
		
		// Set the changed status.
		this.setChanged(false);
				
	}

	public void setType(int typeId) {

		setType(TownBlockType.lookup(typeId));
	}

	public void setType(String typeName) throws TownyException {

		if (typeName.equalsIgnoreCase("reset"))
			typeName = "default";					
		
		TownBlockType type = TownBlockType.lookup(typeName);
		
		if (type == null)
			throw new TownyException(TownySettings.getLangString("msg_err_not_block_type"));
		
		if (this.isJail())
			this.getTown().removeJailSpawn(this.getCoord());
		
		setType(type);
		
	}

	public boolean isHomeBlock() {

		try {
			return getTown().isHomeBlock(this);
		} catch (NotRegisteredException e) {
			return false;
		}
	}
	
	public void setName(String newName) {

		this.name = newName.replace("_", " ");
	}

	public String getName() {

		return this.name;
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

		return new WorldCoord(world.getName(), x, z);
	}

	/**
	 * Is the TownBlock locked
	 * 
	 * @return the locked
	 */
	public boolean isLocked() {

		return locked;
	}

	/**
	 * @param locked is the to locked to set
	 */
	public void setLocked(boolean locked) {

		this.locked = locked;
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
		return this.getX() == o.getX() && this.getZ() == o.getZ() && this.getWorld() == o.getWorld();
	}

	public void clear() {

		setTown(null);
		setResident(null);
		setWorld(null);
	}

	@Override
	public String toString() {

		return getWorld().getName() + " (" + getCoord() + ")";
	}

	public boolean isWarZone() {

		return getWorld().isWarZone(getCoord());
	}

	public boolean isJail() {

		return this.getType() == TownBlockType.JAIL;
	}
}