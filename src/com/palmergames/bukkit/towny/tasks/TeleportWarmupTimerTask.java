package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.EconomyException;
import com.palmergames.bukkit.towny.TownyException;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;

import java.util.ArrayDeque;
import java.util.Queue;

import org.bukkit.Chunk;

/**
 * @author dumptruckman
 */
public class TeleportWarmupTimerTask extends TownyTimerTask {

    private static Queue<Resident> teleportQueue;

    public TeleportWarmupTimerTask(TownyUniverse universe) {
        super(universe);
        teleportQueue = new ArrayDeque<Resident>();
    }

    @Override
    public void run() {
        long currentTime = System.currentTimeMillis();

        while (true) {
            Resident resident = teleportQueue.peek();
            if (resident == null) break;
            if (currentTime > resident.getTeleportRequestTime() + (TownySettings.getTeleportWarmupTime() * 1000)) {
                resident.clearTeleportRequest();
                try {
                	// Make sure the chunk we teleport to is loaded.
                    Chunk chunk = resident.getTeleportDestination().getSpawn().getWorld().getChunkAt(resident.getTeleportDestination().getSpawn().getBlock());
                	if (!chunk.isLoaded()) chunk.load();
                    TownyUniverse.getPlayer(resident).teleport(resident.getTeleportDestination().getSpawn());
                } catch (TownyException ignore) { }
                teleportQueue.poll();
            } else {
                break;
            }
        }
    }

    public static void requestTeleport(Resident resident, Town town, double cost) {
    	resident.setTeleportRequestTime();
        resident.setTeleportDestination(town);
        try {
        teleportQueue.add(resident);
        } catch (NullPointerException e) {
        	System.out.println("[Towny] Error: Null returned from teleport queue.");
        	System.out.println(e.getStackTrace());
        }
    }

    public static void abortTeleportRequest(Resident resident) {
    		if (resident != null && teleportQueue.contains(resident)) {
    			teleportQueue.remove(resident);
    			if ((resident.getTeleportCost() != 0) && (TownySettings.isUsingEconomy())) {
    				try {
						resident.collect(resident.getTeleportCost(), TownySettings.getLangString("msg_cost_spawn_refund"));
						resident.setTeleportCost(0);
						TownyMessaging.sendResidentMessage(resident, TownySettings.getLangString("msg_cost_spawn_refund"));
					} catch (EconomyException e) {
						// Economy error trap
						e.printStackTrace();
					} catch (TownyException e) {
						// Resident not registered exception.
					}
    				
    			}
    		}
    }
}
