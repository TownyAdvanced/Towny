package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.regen.block.BlockObject;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
@SuppressWarnings("deprecation")
public class PlotBlockData {

	private int defaultVersion = 2;

	private String worldName;
	private TownBlock townBlock;
	private int x, z, size, height, version;

	private List<Integer> blockList = new ArrayList<Integer>(); // Stores the original plot blocks
	private int blockListRestored; // counter for the next block to test

	public PlotBlockData(TownBlock townBlock) {

		this.townBlock = townBlock;
		setX(townBlock.getX());
		setZ(townBlock.getZ());
		setSize(TownySettings.getTownBlockSize());
		this.worldName = townBlock.getWorld().getName();
		this.setVersion(defaultVersion);
		setHeight(townBlock.getWorldCoord().getBukkitWorld().getMaxHeight() - 1);
		this.blockListRestored = 0;
	}

	public void initialize() {

		List<Integer> blocks = getBlockArr();
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
	private List<Integer> getBlockArr() {

		List<Integer> list = new ArrayList<Integer>();
		Block block = null;

		World world = this.townBlock.getWorldCoord().getBukkitWorld();
		/*
		 * if (!world.isChunkLoaded(MinecraftTools.calcChunk(getX()),
		 * MinecraftTools.calcChunk(getZ()))) {
		 * return null;
		 * }
		 */
		for (int z = 0; z < size; z++)
			for (int x = 0; x < size; x++)
				for (int y = height; y > 0; y--) { // Top down to account for falling blocks.
					block = world.getBlockAt((getX() * size) + x, y, (getZ() * size) + z);
					switch (defaultVersion) {

					case 1:
					case 2:
						list.add(BukkitTools.getTypeId(block));
						list.add((int) BukkitTools.getData(block));
						break;

					default:
						list.add(BukkitTools.getTypeId(block));
					}

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
		int x, y, z, blockId, reverse, scale;
		int worldx = getX() * size, worldz = getZ() * size;
		BlockObject storedData;
		World world = this.townBlock.getWorldCoord().getBukkitWorld();

		if (!world.isChunkLoaded(BukkitTools.calcChunk(getX()), BukkitTools.calcChunk(getZ())))
			return true;
		
		//Scale for the number of elements
		switch (version) {

		case 1:
		case 2:
			scale = 2;
			break;

		default:
			scale = 1;
		}

		reverse = (blockList.size() - blockListRestored) / scale;

		while (reverse > 0) {

			reverse--; //regen bottom up to stand a better chance of restoring tree's and plants.
			y = height - (reverse % height);
			x = (int) (reverse / height) % size;
			z = ((int) (reverse / height) / size) % size;

			block = world.getBlockAt(worldx + x, y, worldz + z);
			blockId = BukkitTools.getTypeId(block);
			storedData = getStoredBlockData((blockList.size() - 1) - blockListRestored);

			// Increment based upon number of elements
			blockListRestored += scale;

			// If this block isn't correct, replace
			// and return as done.
			if ((blockId != storedData.getTypeId()) || (BukkitTools.getData(block) != storedData.getData())) {

				Material mat = BukkitTools.getMaterial(storedData.getTypeId());

				if ((mat == null) || ((mat != null) && !this.townBlock.getWorld().isPlotManagementIgnoreIds(mat.name(), storedData.getData()))) {

					//System.out.print("regen x: " + x + " y: " + y + " z: " + z + " ID: " + blockId); 

					try {
						//restore based upon version
						switch (version) {

						case 1:
						case 2:
							BukkitTools.setTypeIdAndData(block, storedData.getTypeId(), storedData.getData(), false);

							break;
						default:
							BukkitTools.setTypeId(block, storedData.getTypeId(), false);
						}
						
					} catch (Exception e) {

					}

				} else {
					
					BukkitTools.setTypeId(block, 0, false);
					
				}

				return true;
			}
		}

		// reset as we are finished with the regeneration
		resetBlockListRestored();
		return false;
	}

	private BlockObject getStoredBlockData(int index) {

		//return based upon version
		switch (version) {

		case 1:
		case 2:
			return new BlockObject(blockList.get(index - 1), (byte) (blockList.get(index) & 0xff));

		default:
			return new BlockObject(blockList.get(index), (byte) 0);
		}

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
	public List<Integer> getBlockList() {

		return blockList;
	}

	/**
	 * fills the BlockList
	 * 
	 * @param blockList
	 */
	public void setBlockList(List<Integer> blockList) {

		this.blockList = blockList;
	}

	/**
	 * fills BlockListRestored with zero's to indicate
	 * no blocks have been restored yet
	 */
	public void resetBlockListRestored() {

		blockListRestored = 0;
	}

}