package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
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
    private boolean isHomeblock = false;
    private boolean isOutpost = false;
    private String cancelMessage = Translation.of("msg_claim_error");

    @Override
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }

    public TownPreClaimEvent(Town _town, TownBlock _townBlock, Player _player, boolean _isOutpost, boolean _isHomeblock) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = _town;
        this.townBlock = _townBlock;
        this.player = _player;
        this.isOutpost = _isOutpost;
        this.isHomeblock = _isHomeblock;
    }

    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * Cancels the claiming of a townblock. If a group of townblocks are being claimed 
     * using a single command, and one cancellation occurs, all of the townblock claims
     * will be cancelled.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }

    /**
     * Whether the townblock being claimed will be an outpost.
     * @return true if it will become an outpost.
     */
    public boolean isOutpost() {
    	return isOutpost;
    }
    
    /**
     * Whether the townblock being claimed will be a homeblock.
     * 
     * If this is being thrown because a town is about to be made, the Town object
     * will be unfinished. Many parts of the Town object will throw errors if accessed.
     * 
     * If there are multiple blocks being claimed using the larger selection commands,
     * this will return true for every block in the selection, only the first would become a homeblock. 
     * Cancelling the event will cause all claims to be cancelled.
     * 
     * @return true if the townblock will become a homeblock, or if many townblocks are being claimed at once. 
     */
    public boolean isHomeBlock() {
    	return isHomeblock;
    }
    
    /**
     * The TownBlock which is being claimed.
     * 
     * If {@link #isHomeblock} is true, then this could be the first TownBlock claimed by
     * a town upon Town-creation. In this scenario the Town object has not finished 
     * initializing and many methods in the TownBlock object could return errors when used.
     * 
     * @return the new TownBlock.
     */
    public TownBlock getTownBlock() {
        return townBlock;
    }

    /**
     * The town which is claiming this TownBlock
     * 
     * If {@link #isHomeblock} is true, then this could be the first TownBlock claimed by
     * a town upon Town-creation. In this scenario the Town object has not finished 
     * initializing and many methods in the Town object could return errors when used.
     * 
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
