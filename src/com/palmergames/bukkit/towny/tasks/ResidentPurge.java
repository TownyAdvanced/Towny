package com.palmergames.bukkit.towny.tasks;

import java.util.ArrayList;

import org.bukkit.command.CommandSender;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Resident;


/**
 * @author ElgarL
 *
 */
public class ResidentPurge extends Thread {
	
	Towny plugin;
	private CommandSender sender = null;
	long deleteTime;
	
    /**
     * @param plugin reference to towny
     */
    public ResidentPurge(Towny plugin, CommandSender sender, long deleteTime) {
        super();
        this.plugin = plugin;
        this.deleteTime = deleteTime;
        this.setPriority(MIN_PRIORITY);
    }
    
    @Override
	public void run() {
    	
    	int count = 0;
    	
    	message("Scanning for old residents...");
        for (Resident resident : new ArrayList<Resident>(plugin.getTownyUniverse().getResidents())) {
                if (!resident.isNPC()
                	&& (System.currentTimeMillis() - resident.getLastOnline() > (this.deleteTime)) && !plugin.isOnline(resident.getName())
                	&& !plugin.isOnline(resident.getName())) {
                	count++;
                	message("Deleting resident: " + resident.getName());
                	plugin.getTownyUniverse().removeResident(resident);
                	plugin.getTownyUniverse().removeResidentList(resident);

                }
        }
        
        message("Resident purge complete: " + count + " deleted.");					

    }
    
    private void message(String msg) {
    	
    	if (this.sender != null)
        	TownyMessaging.sendMessage(this.sender, msg);
        else
        	TownyMessaging.sendMsg(msg);
    	
    }
}
    
