package com.palmergames.bukkit.towny.object;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.SpawnPoint.SpawnPointType;

public class Outpost extends ObjectGroup {
	private Town town;
	private Position spawn;
	private Set<TownBlock> townblocks = new HashSet<>();

	public Outpost(UUID id, String name) {
		super(id, name);
	}

	@Override
	public void save() {
		TownyUniverse.getInstance().getDataSource().saveOutpost(this);
	}

	@Override
	public boolean exists() {
		return this.town != null && this.town.exists() && this.town.getOutposts().contains(this);
	}

	public Town getTown() {
		return town;
	}

	public void setTown(Town town) {
		this.town = town;
	}

	public Position getSpawn() {
		return spawn;
	}

	public void setSpawn(Position spawn) {
		// Remove any previously set spawn's particles. 
		if (this.spawn != null)
			TownyUniverse.getInstance().removeSpawnPoint(this.spawn.asLocation());

		this.spawn = spawn;
		TownyUniverse.getInstance().addSpawnPoint(new SpawnPoint(spawn, SpawnPointType.OUTPOST_SPAWN));
	}

	public boolean isOutpostHomeBlock(TownBlock tb) {
		return tb.getWorldCoord().equals(WorldCoord.parseWorldCoord(spawn.asLocation()));
	}

	public int getNumTownBlocks() {
		return townblocks.size();
	}

	public Set<TownBlock> getTownblocks() {
		return townblocks;
	}

	public void addTownblock(TownBlock townblock) {
		if (this.town == null)
			this.town = townblock.getTownOrNull();
		this.townblocks.add(townblock);
	}

	public void removeTownblock(TownBlock townblock) {
		if (isOutpostHomeBlock(townblock))
			TownyUniverse.getInstance().removeSpawnPoint(spawn.asLocation());

		this.townblocks.remove(townblock);
	}

	public boolean hasTownBlocks() {
		return townblocks.size() > 0;
	}

	@Nullable
	public TownBlock getSpawnTownBlock() {
		if (spawn == null)
			return null;
		return WorldCoord.parseWorldCoord(spawn.asLocation()).getTownBlockOrNull();
	}
}
