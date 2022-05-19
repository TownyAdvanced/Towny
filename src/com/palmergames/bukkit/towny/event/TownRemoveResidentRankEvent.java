package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;

import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

 /**
 * @author Artuto
 *
 * Fired after a Resident has been removed from a Town rank.
 */
public class TownRemoveResidentRankEvent extends Event implements Cancellable{
	private static final HandlerList handlers = new HandlerList();

    private final Resident resident;
    private final String rank;
    private final Town town;
	private boolean isCancelled = false;
	private String cancelMessage = Translation.of("msg_err_command_disable");

    
    public TownRemoveResidentRankEvent(Resident resident, String rank, Town town) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.resident = resident;
        this.rank = rank;
        this.town = town;
    }
    
     /**
     *
     * @return the resident that got removed the rank
     * */
    public Resident getResident()
    {
        return resident;
    }
    
     /**
     *
     * @return the removed rank
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
	
	public void setCancelMessage(String s) {
		cancelMessage = s;
	}

	public String getCancelMessage() {
		return cancelMessage;
	}

	@Override
	public boolean isCancelled() {
		return isCancelled;
	}

	@Override
	public void setCancelled(boolean b) {
		this.isCancelled = b;
	}
}