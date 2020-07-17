package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Runs before town banks are charged
 * Provides raw town block
 * */
public class TownPreClaimEvent extends Event implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    private final TownBlock townBlock;
    private final Town town;
    private final Player player;
    private boolean isCancelled = false;
    private String cancelMessage = TownySettings.getLangString("msg_claim_error");

    @Override
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }

    public TownPreClaimEvent(Town _town, TownBlock _townBlock, Player _player) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = _town;
        this.townBlock = _townBlock;
        this.player = _player;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    /**
     *
     * @return the new TownBlock.
     */
    public TownBlock getTownBlock() {
        return townBlock;
    }

    /**
     * @return the town
     * */
    public Town getTown() {
        return town;
    }
    
    /**
     * Useful to send the player a message. 
     *
     * @return the player who's having their claim canceled.
     */
    public Player getPlayer() {
    	return player;
    }
    
    /** 
     * Returns the cancelMessage for this event.
     * 
     * @return the cancelMessage.
     */
	public String getCancelMessage() {
		return cancelMessage;
	}

	/**
	 * Message should requires two variables using %s placeholders, akin to the default message. 
	 * 
	 * Default message: &nbsp;&quot;&amp;cAnother plugin stopped the claim of (%s)/(%s) town blocks, could not complete the operation.&quot;
	 * 
	 * @param cancelMessage the message which will be shown for cancelled events.
	 */
	public void setCancelMessage(String cancelMessage) {
		this.cancelMessage = cancelMessage;
	}
}
