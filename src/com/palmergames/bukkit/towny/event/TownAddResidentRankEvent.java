package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

 /**
 * @author Artuto
 *
 * Fired after a Resident has been added to a Town rank.
 */
public class TownAddResidentRankEvent extends Event
{
    private Resident resident;
    private String rank;
    private Town town;
    
    public TownAddResidentRankEvent(Resident resident, String rank, Town town)
    {
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
}
