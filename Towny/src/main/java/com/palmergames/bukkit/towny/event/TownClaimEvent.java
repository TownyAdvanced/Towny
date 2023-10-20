package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.Warning;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * This event is no longer called.
 * @deprecated since 0.99.6.4 use {@link com.palmergames.bukkit.towny.event.town.TownClaimEvent} instead.
 */
@Deprecated
@Warning(reason = "Event is no longer called. Event has been moved to the com.palmergames.bukkit.towny.event.town package.")
public class TownClaimEvent extends Event  {

    private static final HandlerList handlers = new HandlerList();
    
    private final TownBlock townBlock;
    private Resident resident;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
    
    public static HandlerList getHandlerList() {
		return handlers;
	}

    public TownClaimEvent(TownBlock townBlock, Player player) {
    	super(!Bukkit.getServer().isPrimaryThread());
        this.townBlock = townBlock;
        this.resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
	}

    /**
     *
     * @return the new TownBlock.
     */
    public TownBlock getTownBlock() {
        return townBlock;
    }

	/**
	 * Gets the resident claiming the townblock.
	 * 
	 * @return The resident claiming.
	 */
	public Resident getResident() {
		return resident;
	}

	/**
	 * @return the Town which claimed this TownBlock.
	 */
	public Town getTown() {
		return resident.getTownOrNull();
	}
}