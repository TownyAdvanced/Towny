package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.block.BlockObject;
import com.palmergames.bukkit.util.BukkitTools;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;

public class PlotBlockData {

	/* 
	 * Version number changes when something requires the PlotBlockData to be altered.
	 * ----------
	 * version 5: Required to accomodate worlds with y lower than 0, MC 1.19.
	 * version 4: Required to accept BlockData an support MC 1.14 and newer.
	 * version 3: Required when blocks were no longer available as type:data ints.
	 * version 1 & 2: Older than time itself.
	 */
	private final int defaultVersion = 5;

	private final String worldName;
	private final TownBlock townBlock;
	private int x, z, size, height, minHeight, version;

	private List<String> blockList = new ArrayList<>(); // Stores the original plot blocks
	private int blockListRestored; // counter for the next block to test

	public PlotBlockData(TownBlock townBlock) {

		this.townBlock = townBlock;
		setX(townBlock.getX());
		setZ(townBlock.getZ());
		setSize(TownySettings.getTownBlockSize());
		this.worldName = townBlock.getWorld().getName();
		this.setVersion(defaultVersion);
		setHeight(townBlock.getWorldCoord().getBukkitWorld().getMaxHeight() - 1);
		setMinHeight(townBlock.getWorldCoord().getBukkitWorld().getMinHeight());
		this.blockListRestored = 0;
	}

	public void initialize() {

		List<String> blocks = getBlockArr();
		if (blocks != null) {
			setBlockList(blocks); //fill array
			resetBlockListRestored();
		}
	}

	/**
	 * Fills an array with the current Block types from the plot.
	 * 
	 * @return
	 */
	private List<String> getBlockArr() {

		List<String> list = new ArrayList<>();
		Block block = null;

		World world = this.townBlock.getWorldCoord().getBukkitWorld();
		for (int z = 0; z < size; z++)
			for (int x = 0; x < size; x++)
				for (int y = height; y > minHeight; y--) { // Top down to account for falling blocks.
					block = world.getBlockAt((getX() * size) + x, y, (getZ() * size) + z);
					list.add(block.getBlockData().getAsString(true));
				}
		return list;
	}

	/**
	 * Reverts an area to the stored image.
	 * 
	 * @return true if there are more blocks to check.
	 */
	public boolean restoreNextBlock() {

		Block block = null;
		int x, y, z, reverse;
		int worldx = getX() * size, worldz = getZ() * size;
		int yRange = Math.abs(minHeight) + height;
		Material blockMat, mat;
		BlockObject storedData;
		World world = this.townBlock.getWorldCoord().getBukkitWorld();

		if (!world.isChunkLoaded(BukkitTools.calcChunk(getX()), BukkitTools.calcChunk(getZ())))
			return true;

		//Catch old snapshots which will not regenerate correctly.
		switch (version) {
			case 4, 5:
				break;
			default:
				TownyMessaging.sendErrorMsg("Towny found a plotsnapshot which is from a version too old to use!");
				return false;
		}

		reverse = (blockList.size() - blockListRestored);

		while (reverse > 0) {
			reverse--; //regen bottom up to stand a better chance of restoring tree's and plants.
			blockListRestored++;
			y = height - (reverse % yRange);
			x = (reverse / yRange) % size;
			z = (reverse / yRange / size) % size;
	
			block = world.getBlockAt(worldx + x, y, worldz + z);
			blockMat = block.getType();
			try {
				storedData = getStoredBlockData(blockList.size() - blockListRestored);
			} catch (IllegalArgumentException e1) {
				TownyMessaging.sendDebugMsg("Towny's revert-on-unclaim feature encountered a block which will not load on the current version of MC. Ignoring and skipping to next block.");
				continue;
			}

			mat = storedData.getMaterial();

			// Catch mat being null or currently existing block being the same.
			if (mat == null || blockMat == mat) {
				continue;
			}

			// Catch this being a ignored material that isn't allowed to be reverted, setting it to air.
			if (this.townBlock.getWorld().isPlotManagementIgnoreIds(mat)) {
				block.setType(Material.AIR);
				return true;
			}

			// Actually set the block back to what we have in the snapshot.
			try {
				block.setType(mat, false);
				block.setBlockData(storedData.getBlockData());
				return true;
			} catch (Exception e) {
				TownyMessaging.sendErrorMsg("Exception in PlotBlockData.java");
				break;
			}
		}
		// reset as we are finished with the regeneration
		resetBlockListRestored();
		return false;
	}

	private BlockObject getStoredBlockData(int index) {
		return new BlockObject(blockList.get(index));
	}

	public int getX() {

		return x;
	}

	public void setX(int x) {

		this.x = x;
	}

	public int getZ() {

		return z;
	}

	public void setZ(int z) {

		this.z = z;
	}

	public int getSize() {

		return size;
	}

	public void setSize(int size) {

		this.size = size;
	}

	public int getHeight() {

		return height;
	}

	public void setHeight(int height) {

		this.height = height;
	}

	public int getMinHeight() {

		return minHeight;
	}

	public void setMinHeight(int minHeight) {
		this.minHeight = minHeight;
	}

	public String getWorldName() {

		return worldName;
	}

	/**
	 * @return the version
	 */
	public int getVersion() {

		return version;
	}

	/**
	 * @param version the version to set
	 */
	public void setVersion(int version) {

		this.version = version;
	}

	/**
	 * @return the blockList
	 */
	public List<String> getBlockList() {

		return blockList;
	}

	/**
	 * fills the BlockList
	 * 
	 * @param blockList - BlockList (List&lt;String&gt;)
	 */
	public void setBlockList(List<String> blockList) {

		this.blockList = blockList;
	}

	/**
	 * fills BlockListRestored with zero's to indicate
	 * no blocks have been restored yet
	 */
	public void resetBlockListRestored() {

		blockListRestored = 0;
	}

	public WorldCoord getWorldCoord() {
		return townBlock.getWorldCoord();
	}

}