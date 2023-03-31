package com.palmergames.bukkit.towny.tasks;

import org.bukkit.Bukkit;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.WorldCoordEntityRemover;
import com.palmergames.bukkit.towny.regen.WorldCoordMaterialRemover;

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

		// Try to perform the next plot_management entity_delete
		if (WorldCoordEntityRemover.hasQueue()) {
			tryDeleteTownBlockEntityQueue();
		}

		// Try to perform the next plot_management block_delete
		if (WorldCoordMaterialRemover.hasQueue()) {
			tryDeleteTownBlockMaterials();
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

	private void tryDeleteTownBlockEntityQueue() {
		if (WorldCoordEntityRemover.getActiveQueueSize() >= 10)
			return;
		// Remove WC from larger queue.
		WorldCoord wc = WorldCoordEntityRemover.getWorldCoordFromQueue();
		if (wc == null)
			return;
		// Remove a WC from the active queue and remove the entities from it.
		WorldCoordEntityRemover.doDeleteTownBlockEntities(wc);
	}

	private void tryDeleteTownBlockMaterials() {
		if (WorldCoordMaterialRemover.getActiveQueueSize() >= 10)
			return;
		// Get WC from larger queue.
		WorldCoord wc = WorldCoordMaterialRemover.getWorldCoordFromQueue();
		if (wc == null)
			return;
		// Tell it to regen.
		Bukkit.getScheduler().runTaskAsynchronously(plugin, () -> WorldCoordMaterialRemover.queueUnclaimMaterialsDeletion(wc));
	}
}
