package ca.xshade.bukkit.towny;

import java.io.IOException;
import java.util.ArrayList;

import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.object.TownyWorld;

public class DailyTimerTask extends TownyTimerTask {
	public DailyTimerTask(TownyUniverse universe) {
		super(universe);
	}

	@Override
	public void run() {
		long start = System.currentTimeMillis();
		
		universe.getPlugin().sendDebugMsg("New Day");
		
		// Collect taxes
		if (TownySettings.isUsingIConomy() && TownySettings.isTaxingDaily()) {
			universe.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day_tax")));
			try {
				universe.getPlugin().sendDebugMsg("Collecting Town Taxes");
				universe.collectTownTaxes();
				universe.getPlugin().sendDebugMsg("Collecting Nation Taxes");
				universe.collectNationTaxes();
				universe.getPlugin().sendDebugMsg("Collecting Town Costs");
				universe.collectTownCosts();
				universe.getPlugin().sendDebugMsg("Collecting Nation Costs");
				universe.collectNationCosts();
			} catch (IConomyException e) {
			}
		}
		else
			universe.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day")));
			

		// Automatically delete old residents 
		if (TownySettings.isDeletingOldResidents()) {
			universe.getPlugin().sendDebugMsg("Scanning for old residents...");
			for (Resident resident : new ArrayList<Resident>(universe.getResidents()))
				if ((System.currentTimeMillis() - resident.getLastOnline() > TownySettings.getMaxInactivePeriod()) && !plugin.isOnline(resident.getName())) {
					universe.getPlugin().sendMsg("Deleting resident: " + resident.getName());
					universe.removeResident(resident);
					universe.removeResidentList(resident);
				}
		}
		
		// Backups
		if (TownySettings.isBackingUpDaily())
			try {
				universe.getPlugin().sendDebugMsg("Making backup");
				universe.getDataSource().backup();
			} catch (IOException e) {
				System.out.println("[Towny] Error: Could not create backup.");
				System.out.print(e.getStackTrace());
			}
		
		
		universe.getPlugin().sendDebugMsg("Finished New Day Code");
		universe.getPlugin().sendDebugMsg("Universe Stats:");
		universe.getPlugin().sendDebugMsg("    Residents: " + universe.getResidents().size());
		universe.getPlugin().sendDebugMsg("    Towns: " + universe.getTowns().size());
		universe.getPlugin().sendDebugMsg("    Nations: " + universe.getNations().size());
        for (TownyWorld world : universe.getWorlds())
        	universe.getPlugin().sendDebugMsg("    " + world.getName() + " (townblocks): " + world.getTownBlocks().size());
        
        universe.getPlugin().sendDebugMsg("Memory (Java Heap):");
        universe.getPlugin().sendDebugMsg(String.format("%8d Mb (max)", Runtime.getRuntime().maxMemory()/1024/1024));
        universe.getPlugin().sendDebugMsg(String.format("%8d Mb (total)", Runtime.getRuntime().totalMemory()/1024/1024));
        universe.getPlugin().sendDebugMsg(String.format("%8d Mb (free)", Runtime.getRuntime().freeMemory()/1024/1024));
        universe.getPlugin().sendDebugMsg(String.format("%8d Mb (used=total-free)", (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024));
        plugin.sendDebugMsg("newDay took " + (System.currentTimeMillis() - start) + "ms");
	}
}
