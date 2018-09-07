package com.palmergames.bukkit.towny.regen.block;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

import com.palmergames.bukkit.util.BukkitTools;

/**
 * @author ElgarL
 * 
 */
public class BlockMobSpawner extends BlockObject {

	private EntityType mobType;
	private int delay;

	/**
	 * @param type
	 */
	public BlockMobSpawner(EntityType type) {

		super(BukkitTools.getMaterialId(Material.SPAWNER));
		this.mobType = type;
	}

	/**
	 * Get the mob type.
	 * 
	 * @return the EntityType this spawner is set for.
	 */
	public EntityType getSpawnedType() {

		return mobType;
	}

	/**
	 * Set the mob type.
	 * 
	 * @param mobType
	 */
	public void setSpawnedType(EntityType mobType) {

		this.mobType = mobType;
	}

	/**
	 * @return the delay
	 */
	public int getDelay() {

		return delay;
	}

	/**
	 * @param i the delay to set
	 */
	public void setDelay(int i) {

		this.delay = i;
	}

}
