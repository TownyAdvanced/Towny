/**
 * 
 */
package com.palmergames.bukkit.towny.regen.block;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;

/**
 * @author ElgarL
 * 
 */
public class BlockMobSpawner extends BlockObject {

	private EntityType mobType;
	private int delay;

	/**
	 * @param typeID
	 */
	public BlockMobSpawner(EntityType type) {

		super(Material.MOB_SPAWNER.getId());
		this.mobType = type;
	}

	/**
	 * Get the mob type.
	 * 
	 * @return
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
