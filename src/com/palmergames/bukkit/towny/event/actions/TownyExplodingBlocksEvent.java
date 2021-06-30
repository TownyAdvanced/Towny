package com.palmergames.bukkit.towny.event.actions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Entity;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * TownyExplodingBlockEvents are thrown when an explosion occurs 
 * in a Towny world, causing block damage.
 * 
 * <br> - When a bed/respawn anchor explodes causing block damage.
 * <br> - When a an entity explodes causing block damage.
 * 
 * @author LlmDl
 */
public class TownyExplodingBlocksEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final List<Block> vanillaBlockList;
	private final List<Block> filteredBlockList;
	private final Material material;
	private final Entity entity;
	private List<Block> blockList = new ArrayList<Block>();
	private boolean isChanged = false;
	private Event event;

	/**
	 * Event thrown when blocks are exploded by a block or entity.
	 * 
	 * @param vanillaBlockList - List of Blocks which were involved in the original explosion.
	 * @param townyFilteredList - List of Blocks Towny has already filtered, these blocks will explode unless {@link #setBlockList(List)} is used.
	 * @param mat - Material which caused the block explosion or null if it is an entity explosion.
	 * @param entity - Entity which caused the entity explosion or null if it is a block explosion.
	 * @param bukkitExplodeEvent - The Bukkit Explosion Event that caused this explosion. 
	 */
	public TownyExplodingBlocksEvent(List<Block> vanillaBlockList, List<Block> townyFilteredList, Material mat, Entity entity, Event bukkitExplodeEvent) {
		this.vanillaBlockList = vanillaBlockList;
		this.filteredBlockList = townyFilteredList;
		this.material = mat;
		this.entity = entity;
		this.event = bukkitExplodeEvent;
	}

	public static HandlerList getHandlerList() {
		return handlers;
	}

	public HandlerList getHandlers() {
		return handlers;
	}

	/**
	 * Used to set the list of Blocks which will be allowed to explode by Towny.
	 * 
	 * @param blockList - List of Blocks which will explode.
	 */
	public void setBlockList(List<Block> blockList) {
		this.blockList.clear();
		this.blockList.addAll(blockList);
		this.filteredBlockList.clear();
		this.filteredBlockList.addAll(blockList);
		this.isChanged = true;
	}

	/**
	 * The block list that will ultimately be used to set which blocks are allowed, as long as setBlockList() has been used by someone.
	 * 
	 * @return blockList - List of blocks which will be used by Towny ultimately.
	 */
	@Nullable
	public List<Block> getBlockList() {
		return blockList;
	}

	/**
	 * The blocklist of blocks exploding because of the explosion event that triggered this event.
	 * 
	 * @return vanillaBlockList - The total number of blocks in this explosion.
	 */
	public final List<Block> getVanillaBlockList() {
		return vanillaBlockList;
	}

	/**
	 * The list of already-filtered blocks Towny has determined will be allowed to explode.
	 * 
	 * @return filteredBlockList - Already allowed blocks.
	 */
	@Nullable
	public final List<Block> getTownyFilteredBlockList() {
		return filteredBlockList;
	}

	/**
	 * Has any plugin or Towny's WarZoneListener modified the list of blocks to explode?
	 * 
	 * @return true if the list has been altered.
	 */
	public boolean isChanged() {
		return isChanged;
	}
	
	/**
	 * Whether this event has a material.
	 * 
	 * @return true if this was a block explosion.
	 */
	public boolean hasMaterial() {
		return material != null;
	}

	/**
	 * Returns the material causing the block explosion.
	 * 
	 * @return material responsible for explosion or null if it is a entity explosion.
	 */
	@Nullable
	public Material getMaterial() {
		return material;
	}

	/**
	 * Whether this event has a entity.
	 * 
	 * @return true is this was an entity explosion.
	 */
	public boolean hasEntity() {
		return entity != null;
	}
	
	/**
	 * Returns the entity causing the entity explosion.
	 * 
	 * @return entity responsible for explosion or null if it is a block explosion.
	 */
	@Nullable
	public Entity getEntity() {
		return entity;
	}

	/**
	 * Returns the bukkit event which originally fired when the explosion happened.
	 * 
	 * @return event which can be either an EntityExplodeEvent or a BlockExplodeEvent.
	 */
	public Event getBukkitExplodeEvent() {
		return event;
	}
}
