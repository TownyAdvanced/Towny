package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.regen.PlotBlockData;
import com.palmergames.bukkit.towny.regen.PlotSnapshotQueue;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.regen.WorldCoordEntityRemover;
import com.palmergames.bukkit.towny.regen.WorldCoordMaterialRemover;
import com.palmergames.bukkit.towny.regen.WorldCoordQueue;

import java.util.Arrays;
import java.util.Collection;

public class RepeatingTimerTask extends TownyTimerTask {
	static final Collection<WorldCoordQueue> queues = Arrays.asList(WorldCoordMaterialRemover.getInstance(), WorldCoordEntityRemover.getInstance(), PlotSnapshotQueue.getInstance());

	public RepeatingTimerTask(Towny plugin) {

		super(plugin);
	}

	private long revertCounter = 0L;
	private long tickCount = 0;

	@Override
	public void run() {

		// Perform a single block regen in each regen area, if any are left to do.
		if (++tickCount % 20 == 0 && TownyRegenAPI.hasActiveRegenerations()) {
			tickCount = 0;
			revertAnotherBlockToWilderness();
		}

		queues.forEach(WorldCoordQueue::pollQueue);
	}

	private void revertAnotherBlockToWilderness() {
		// only execute if the correct amount of time has passed.
		if (Math.max(1L, TownySettings.getPlotManagementSpeed()) > ++revertCounter)
			return;

		for (PlotBlockData plotBlockData : TownyRegenAPI.getActivePlotBlockDatas()) {
			plugin.getScheduler().run(plotBlockData.getWorldCoord().getLowerMostCornerLocation(), () -> {
				if (!plotBlockData.restoreNextBlock())
					TownyRegenAPI.finishPlotBlockData(plotBlockData);
			});
		}

		revertCounter = 0L;
	}
}
