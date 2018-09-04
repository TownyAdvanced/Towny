package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;

import java.util.ArrayList;

public class RepeatingTimerTask extends TownyTimerTask {

	public RepeatingTimerTask(Towny plugin) {

		super(plugin);
	}

	private Long timerCounter = 0L;

	@Override
	public void run() {

		// Perform a single block regen in each regen area, if any are left to do.
		if (TownyRegenAPI.hasPlotChunks()) {
			// only execute if the correct amount of time has passed.			
			if (Math.max(1L, TownySettings.getPlotManagementSpeed()) <= ++timerCounter) {
				for (PlotBlockData plotChunk : new ArrayList<PlotBlockData>(TownyRegenAPI.getPlotChunks().values())) {
					if (!plotChunk.restoreNextBlock()) {
						TownyRegenAPI.deletePlotChunk(plotChunk);
						TownyRegenAPI.deletePlotChunkSnapshot(plotChunk);
					}
				}
				timerCounter = 0L;
			}
		}

		/*
		  The following actions should be performed every second.
		 */
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
				plugin.updateCache(townBlock.getWorldCoord());

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
