package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

 /**
 * @author Artuto
 *
 * Fired after a Resident has been added to a Town rank.
 */
public class TownAddResidentRankEvent extends Event
{
	private static final HandlerList handlers = new HandlerList();
	
    private final Resident resident;
    private final String rank;
    private final Town town;
    
    public TownAddResidentRankEvent(Resident resident, String rank, Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.resident = resident;
        this.rank = rank;
        this.town = town;
    }
    
     /**
     *
     * @return the resident that got the rank
     * */
    public Resident getResident()
    {
        return resident;
    }
    
     /**
     *
     * @return the added rank
     * */
    public String getRank()
    {
        return rank;
    }
    
     /**
     *
     * @return the town this resident is part of
     * */
    public Town getTown()
    {
        return town;
    }

	@Override
	public HandlerList getHandlers() {

		return handlers;
	}
	
	public static HandlerList getHandlerList() {

		return handlers;
	}
}