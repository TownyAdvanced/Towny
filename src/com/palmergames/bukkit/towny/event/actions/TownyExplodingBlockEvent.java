package com.palmergames.bukkit.towny.event.actions;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.block.Block;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.Nullable;

/**
 * Part of the API which lets Towny's war and other plugins modify Towny's
 * plot-permission-decision outcomes.
 * 
 * Explosion events are thrown when an explosion occurs in a Towny world.
 * 
 * @author LlmDl
 */
public class TownyExplodingBlockEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final List<Block> vanillaBlockList;
	private final List<Block> filteredBlockList;
	private List<Block> blockList = new ArrayList<Block>();
	private boolean isChanged = false;

	/**
	 * Event thrown when blocks are exploded by a block or entity.
	 * 
	 * @param vanillaBlockList - List of Blocks which were involved in the original explosion.
	 * @param townyFilteredList - List of Blocks Towny has already filtered, these blocks will explode unless {@link #setBlockList(List)} is used.
	 */
	public TownyExplodingBlockEvent(List<Block> vanillaBlockList, List<Block> townyFilteredList) {
		this.vanillaBlockList = vanillaBlockList;
		this.filteredBlockList = townyFilteredList;
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
	
}
