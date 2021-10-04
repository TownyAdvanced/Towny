package com.palmergames.bukkit.towny.tasks;

import java.io.File;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.util.FileMgmt;

/**
 * Task run in Async which deletes old backups
 * and other outdated things. 
 * 
 * @author Siris, LlmDl
 */
public class CleanupTask implements Runnable {

	private String dataFolderPath = TownyUniverse.getInstance().getRootFolder() + File.separator + "data" + File.separator;
	
	@Override
	public void run() {
		cleanupBackups();
		cleanupPlotBlockData();
	}
	
	private void cleanupBackups() {
		
        long deleteAfter = TownySettings.getBackupLifeLength();
        if (deleteAfter >= 0) {
        	Towny.getPlugin().getLogger().info("Cleaning up old backups...");

        	if (FileMgmt.deleteOldBackups(new File(TownyUniverse.getInstance().getRootFolder() + File.separator + "backup"), deleteAfter))
        		Towny.getPlugin().getLogger().info("Successfully cleaned backups.");
        	else
        		Towny.getPlugin().getLogger().info("Could not delete old backups.");
        }
	}

	/**
	 * Method to clean out old un-zipped PlotBlockData .data files.
	 * 
	 * Parses over the world folders in the plot-block-data file for 
	 * old .data files and sends them to be zipped, after which they
	 * are deleted.
	 */
	private void cleanupPlotBlockData() {
		File plotBlockDataFolder = new File(dataFolderPath + "plot-block-data");
		File[] worldFolders = plotBlockDataFolder.listFiles(File::isDirectory);
		for (File worldfolder : worldFolders) {
			File worldFolder = new File(dataFolderPath + "plot-block-data" + File.separator + worldfolder.getName());
			File[] plotBlockDataFiles = worldFolder.listFiles((file)->file.getName().endsWith(".data"));
			for (File plotBlockDataFile : plotBlockDataFiles) {
				FileMgmt.zipFile(plotBlockDataFile, plotBlockDataFile.getParent() + File.separator + plotBlockDataFile.getName().replace("data", "zip"));
				FileMgmt.deleteFile(plotBlockDataFile);
			}
		}		
	}
}

