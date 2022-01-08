package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;

public class RepeatingTimerTask extends TownyTimerTask {

	public RepeatingTimerTask(Towny plugin) {

		super(plugin);
	}

	private Long timerCounter = 0L;

	@Override
	public void run() {

		// Perform a single block regen in each regen area, if any are left to do.
		if (TownyRegenAPI.hasActiveRegenerations()) {
			revertAnotherBlockToWilderness();
		}

		/*
		  The following actions should be performed every second.
		 */

		// Take a snapshot of the next townBlock and save.
		if (TownyRegenAPI.hasWorldCoords()) {
			makeNextPlotSnapshot();
		}

		// Perform the next plot_management block_delete
		if (TownyRegenAPI.hasDeleteTownBlockIdQueue()) {
			TownyRegenAPI.doDeleteTownBlockIds(TownyRegenAPI.getDeleteTownBlockIdQueue());
		}
	}

	private void revertAnotherBlockToWilderness() {
		// only execute if the correct amount of time has passed.
		if (Math.max(1L, TownySettings.getPlotManagementSpeed()) > ++timerCounter)
			return;

		for (PlotBlockData plotBlockData : TownyRegenAPI.getActivePlotBlockDatas())
			if (plotBlockData != null && !plotBlockData.restoreNextBlock())
				TownyRegenAPI.finishPlotBlockData(plotBlockData);

		timerCounter = 0L;
	}

	private void makeNextPlotSnapshot() {
		WorldCoord wc = TownyRegenAPI.getWorldCoord();
		TownBlock townBlock = wc.getTownBlockOrNull();
		if (townBlock == null)
			return;
		PlotBlockData plotChunk = new PlotBlockData(townBlock);
		plotChunk.initialize(); // Create a new snapshot.
		if (!plotChunk.getBlockList().isEmpty() && !(plotChunk.getBlockList() == null))
			TownyRegenAPI.addPlotChunkSnapshot(plotChunk); // Save the snapshot.
		plotChunk = null;
		
		townBlock.setLocked(false);
		townBlock.save();
		plugin.updateCache(townBlock.getWorldCoord());

		if (!TownyRegenAPI.hasWorldCoords())
			TownyMessaging.sendDebugMsg("Plot snapshots completed.");
	}

}
