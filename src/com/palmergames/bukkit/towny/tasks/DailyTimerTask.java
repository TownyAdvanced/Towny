package com.palmergames.bukkit.towny.tasks;

import java.io.IOException;
import java.util.ArrayList;

import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;

public class DailyTimerTask extends TownyTimerTask {
        public DailyTimerTask(TownyUniverse universe) {
                super(universe);
        }

        @Override
        public void run() {
                long start = System.currentTimeMillis();
                
                TownyMessaging.sendDebugMsg("New Day");
                
                // Collect taxes
                if (plugin.isEcoActive() && TownySettings.isTaxingDaily()) {
                	TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day_tax")));
                        try {
                        	TownyMessaging.sendDebugMsg("Collecting Town Taxes");
                                universe.collectTownTaxes();
                                TownyMessaging.sendDebugMsg("Collecting Nation Taxes");
                                universe.collectNationTaxes();
                                TownyMessaging.sendDebugMsg("Collecting Town Costs");
                                universe.collectTownCosts();
                                TownyMessaging.sendDebugMsg("Collecting Nation Costs");
                                universe.collectNationCosts();
                        } catch (EconomyException e) {
                        } catch (TownyException e) {
                                // TODO king exception
                                e.printStackTrace();
                        }
                }
                else
                	TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day")));
                        

                // Automatically delete old residents 
                if (TownySettings.isDeletingOldResidents()) {
                	TownyMessaging.sendDebugMsg("Scanning for old residents...");
                        for (Resident resident : new ArrayList<Resident>(universe.getResidents())) {
                                if (!resident.isNPC()
                                	&& (System.currentTimeMillis() - resident.getLastOnline() > (TownySettings.getDeleteTime()*1000))
                                	&& !plugin.isOnline(resident.getName())) {
                                	TownyMessaging.sendMsg("Deleting resident: " + resident.getName());
                                    universe.removeResident(resident);
                                    universe.removeResidentList(resident);

                                }
                        }
                }
                
                // Backups
                TownyMessaging.sendDebugMsg("Cleaning up old backups.");
                TownyUniverse.getDataSource().cleanupBackups();
                if (TownySettings.isBackingUpDaily())
                        try {
                        	TownyMessaging.sendDebugMsg("Making backup.");
                            TownyUniverse.getDataSource().backup();
                        } catch (IOException e) {
                        	TownyMessaging.sendErrorMsg("Could not create backup.");
                            System.out.print(e.getStackTrace());
                        }
                
                TownyMessaging.sendDebugMsg("Finished New Day Code");
                TownyMessaging.sendDebugMsg("Universe Stats:");
                TownyMessaging.sendDebugMsg("    Residents: " + universe.getResidents().size());
                TownyMessaging.sendDebugMsg("    Towns: " + universe.getTowns().size());
                TownyMessaging.sendDebugMsg("    Nations: " + universe.getNations().size());
        for (TownyWorld world : universe.getWorlds())
        	TownyMessaging.sendDebugMsg("    " + world.getName() + " (townblocks): " + world.getTownBlocks().size());
        
        		TownyMessaging.sendDebugMsg("Memory (Java Heap):");
        		TownyMessaging.sendDebugMsg(String.format("%8d Mb (max)", Runtime.getRuntime().maxMemory()/1024/1024));
        		TownyMessaging.sendDebugMsg(String.format("%8d Mb (total)", Runtime.getRuntime().totalMemory()/1024/1024));
        		TownyMessaging.sendDebugMsg(String.format("%8d Mb (free)", Runtime.getRuntime().freeMemory()/1024/1024));
        		TownyMessaging.sendDebugMsg(String.format("%8d Mb (used=total-free)", (Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory())/1024/1024));
        		TownyMessaging.sendDebugMsg("newDay took " + (System.currentTimeMillis() - start) + "ms");
        }
}
