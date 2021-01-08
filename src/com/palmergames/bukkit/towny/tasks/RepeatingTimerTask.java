package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;

public class RepeatingTimerTask extends TownyTimerTask {
	private static final Logger LOGGER = LogManager.getLogger(Towny.class);
	
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
						TownyMessaging.sendDebugMsg("Revert on unclaim complete for " + plotChunk.getWorldName() + " " + plotChunk.getX() +"," + plotChunk.getZ());
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

				if (!plotChunk.getBlockList().isEmpty() && !(plotChunk.getBlockList() == null)) {
					TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save the snapshot.
				}
				
				townBlock.setLocked(false);
				townBlock.save();
				plugin.updateCache(townBlock.getWorldCoord());

				if (!TownyRegenAPI.hasWorldCoords()) {
					LOGGER.info("Plot snapshots completed.");
				}

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
