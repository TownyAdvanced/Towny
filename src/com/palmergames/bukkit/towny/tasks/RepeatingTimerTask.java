package com.palmergames.bukkit.towny.tasks;

import java.util.ArrayList;

import com.palmergames.bukkit.towny.object.PlotBlockData;
import com.palmergames.bukkit.towny.object.TownyRegenAPI;
import com.palmergames.bukkit.towny.object.TownyUniverse;

public class RepeatingTimerTask extends TownyTimerTask {
	
	public RepeatingTimerTask(TownyUniverse universe) {
		super(universe);
	}
	
	@Override
	public void run() {
		if (TownyRegenAPI.hasPlotChunks())
			for (PlotBlockData plotChunk : new ArrayList<PlotBlockData>(TownyRegenAPI.getPlotChunks().values())) {
				if (!plotChunk.restoreNextBlock()) {
					TownyRegenAPI.deletePlotChunk(plotChunk);	
					TownyRegenAPI.deletePlotChunkSnapshot(plotChunk);
				}
			}
	}
	
}
