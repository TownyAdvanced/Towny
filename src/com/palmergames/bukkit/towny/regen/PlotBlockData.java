package com.palmergames.bukkit.towny.regen;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.regen.block.BlockObject;
import com.palmergames.bukkit.util.BukkitTools;

import de.themoep.idconverter.IdMappings;

import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import java.util.ArrayList;
import java.util.List;

public class PlotBlockData {

	private int defaultVersion = 4;

	private String worldName;
	private TownBlock townBlock;
	private int x, z, size, height, version;

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
					case 3:
					case 4:
						list.add(block.getBlockData().getAsString(true));
						break;
					default:
						list.add(block.getType().getKey().toString());

					}					
				}
		return list;
	}

	/**
	 * Reverts an area to the stored image.
	 * 
	 * @return true if there are more blocks to check.
	 */
	@SuppressWarnings("deprecation")
	public boolean restoreNextBlock() {

		Block block = null;
		int x, y, z, reverse, scale;
		int worldx = getX() * size, worldz = getZ() * size;
		Material blockMat, mat;
		BlockObject storedData;
		World world = this.townBlock.getWorldCoord().getBukkitWorld();

		if (!world.isChunkLoaded(BukkitTools.calcChunk(getX()), BukkitTools.calcChunk(getZ())))
			return true;


		//Scale for the number of elements
		switch (version) {

			case 1:
			case 2:
			case 3:
				scale = 2;
				break;	
			case 4:
				scale = 1;
				break;	
			default:
				scale = 1;
		}

		reverse = (blockList.size() - blockListRestored) / scale;
		
		while (reverse > 0) {
			reverse--; //regen bottom up to stand a better chance of restoring tree's and plants.
			y = height - (reverse % height);
			x = (reverse / height) % size;
			z = (reverse / height / size) % size;
	
			block = world.getBlockAt(worldx + x, y, worldz + z);
			blockMat = block.getType();
			try {
				storedData = getStoredBlockData((blockList.size() - 1) - blockListRestored);
			} catch (IllegalArgumentException e1) {
				TownyMessaging.sendDebugMsg("Towny's revert-on-unclaim feature encountered a block which will not load on the current version of MC. Ignoring and skipping to next block.");
				continue;
			}
			
			switch (version) {
		
				case 1:
				case 2:				
				case 3:
					TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - block " + block.toString());
					TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - storedData.getTypeID() " + storedData.getTypeId());
					TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - storedData.getKey() " + storedData.getKey());
					TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - storedData.getData() " + storedData.getData());
					if(storedData.usesID()) {
						if (storedData.getData() == 0) {
							TownyMessaging.sendDebugMsg("IDmappings - " + Material.getMaterial(IdMappings.getById(String.valueOf(storedData.getTypeId())).getFlatteningType()));
							mat = BukkitTools.getMaterial(storedData.getTypeId());
						} else {
							try {
								mat = Material.getMaterial(IdMappings.getById(storedData.getTypeId() + ":" + storedData.getData()).getFlatteningType());					
							} catch (NullPointerException e) {
								// Sometimes blocks facing causes null lookups, we fall back to the base material.
								mat = Material.getMaterial(IdMappings.getById(String.valueOf(storedData.getTypeId())).getFlatteningType());
							}
						}
					} else {
						mat = Material.matchMaterial(storedData.getKey());
					}
					// Increment based upon number of elements
					blockListRestored += scale;
						
			
					// If this block isn't correct, replace
					// and return as done.
					if (mat == null) {
						TownyMessaging.sendErrorMsg("PlotBlockData:restoreNextBlock() - Material Null, skipping block.");
					} else if (blockMat != mat) {
						TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - blockMat " + blockMat.toString() + " doesn't match mat " + mat.toString());
						if (!this.townBlock.getWorld().isPlotManagementIgnoreIds(mat.name(), storedData.getData())) {
			
							try {
			
									block.setType(mat, false);		
									return true;
							} catch (Exception e) {
								TownyMessaging.sendErrorMsg("Exception in PlotBlockData.java - BlockID found in legacy plotsnapshot which could not be resolved to a Material. ");
							}
			
						} else {					
							block.setType(Material.AIR);
							return true;
						}
			
						return true;
					}
					TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - Blocks match, no replacing needed.");
					break;
				
				case 4:
					blockListRestored += scale;
					
					mat = storedData.getMaterial();
					if (mat == null) {
						TownyMessaging.sendErrorMsg("PlotBlockData:restoreNextBlock() - Material Null, skipping block.");
					} else if (blockMat != mat) {
						TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - blockMat " + blockMat.toString() + " doesn't match mat " + mat.toString());
						if (!this.townBlock.getWorld().isPlotManagementIgnoreIds(mat)) {
							try {								
								block.setType(mat, false);
								block.setBlockData(storedData.getBlockData());
								return true;
							} catch (Exception e) {
								TownyMessaging.sendErrorMsg("Exception in PlotBlockData.java");
								break;
							}
			
						} else {					
							block.setType(Material.AIR);
							return true;
						}
			
					}
					//TownyMessaging.sendDebugMsg("PlotBlockData:restoreNextBlock() - Blocks match, no replacing needed.");
					break;
					
				default:
					TownyMessaging.sendErrorMsg("PlotBlockData:restoreNextBlock() - You should not be seeing this message.");					
					
			}
			
		}
		// reset as we are finished with the regeneration
		resetBlockListRestored();
		return false;
	}

	@SuppressWarnings("deprecation")
	private BlockObject getStoredBlockData(int index) {

		//return based upon version
		switch (version) {

		case 1:
		case 2:
		case 3:
			return new BlockObject(blockList.get(index - 1), (byte) (Integer.valueOf(blockList.get(index)) & 0xff));
		case 4:
			return new BlockObject(blockList.get(index));
		default:
			return new BlockObject(blockList.get(index));
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

}