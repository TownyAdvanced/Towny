package com.palmergames.bukkit.towny.tasks;

import java.util.ArrayList;

import com.palmergames.bukkit.towny.NotRegisteredException;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.object.PlotBlockData;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyRegenAPI;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class RepeatingTimerTask extends TownyTimerTask {
	
	public RepeatingTimerTask(TownyUniverse universe) {
		super(universe);
	}
	
	@Override
	public void run() {
		// Perform a single block regen in each regen area, if any are left to do.
		if (TownyRegenAPI.hasPlotChunks()) {
			for (PlotBlockData plotChunk : new ArrayList<PlotBlockData>(TownyRegenAPI.getPlotChunks().values())) {
				if (!plotChunk.restoreNextBlock()) {
					TownyRegenAPI.deletePlotChunk(plotChunk);	
					TownyRegenAPI.deletePlotChunkSnapshot(plotChunk);
				}
			}
		}
		
		// Take a snapshot of the next townBlock and save.
		if (TownyRegenAPI.hasWorldCoords()) {
			try {
				TownBlock townBlock = TownyRegenAPI.getWorldCoord().getTownBlock();
				PlotBlockData plotChunk = new PlotBlockData(townBlock);
    			plotChunk.initialize(); // Create a new snapshot.

    			if (!plotChunk.getBlockList().isEmpty() && !(plotChunk.getBlockList() == null))
    				TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save the snapshot.
    		
    			plotChunk = null;
    			
    			townBlock.setLocked(false);
    			TownyUniverse.getDataSource().saveTownBlock(townBlock);
    			plugin.updateCache();
    			
    			if (!TownyRegenAPI.hasWorldCoords())
    				TownyLogger.log.info("Plot snapshots completed.");
    			
			} catch (NotRegisteredException e) {
				// Not a townblock so ignore.
			}
			
		}
		
		// Perform the next plot_management block_delete
		if (TownyRegenAPI.hasDeleteTownBlockIdQueue()) {
			TownyRegenAPI.doDeleteTownBlockIds(TownyRegenAPI.getDeleteTownBlockIdQueue());
		}
	}
	
}
