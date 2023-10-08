package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.entity.Player;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Runs before town banks are charged
 * Provides raw town block
 * */
public class TownPreClaimEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();

    private final TownBlock townBlock;
    private final Town town;
    private final Player player;
    private boolean isHomeblock = false;
    private boolean isOutpost = false;
	private boolean isOverClaim = false;

	/**
	 * This event runs when a town is made and when a town attempts to claim land.
	 * <p>
	 * When a selection of townblocks are claimed, if even one townblock is
	 * prevented using this event, all the claims will be prevented.
	 * <p>
	 * This event will throw an error message to the player that requires 2 %s, the
	 * first of which will be the number of blocked claims in a selection. The
	 * second of which is the total number of townblocks that were in the selection.
	 * <p>
	 * If you are using {@link #setCancelMessage(String)} on this event be sure to
	 * supply two instances of %s.
	 * 
	 * @param town Town which is claiming.
	 * @param townBlock TownBlock which is being claimed.
	 * @param player Player who is doing the claiming.
	 * @param isOutpost True if the TownBlock will become an outpost.
	 * @param isHomeblock True if the TownBlock will become a homeblock.
	 * @param isOverClaim True if the claim has resulted from takeoverclaim.
	 */
    public TownPreClaimEvent(Town town, TownBlock townBlock, Player player, boolean isOutpost, boolean isHomeblock, boolean isOverClaim) {
        this.town = town;
        this.townBlock = townBlock;
        this.player = player;
        this.isOutpost = isOutpost;
        this.isHomeblock = isHomeblock;
		this.isOverClaim = isOverClaim;
        setCancelMessage(Translation.of("msg_claim_error"));
    }

	/**
	 * Whether the claim has resulted from takeoverclaim.
	 * @return true if the claim resulted from /t takeoverclaim.
	 */
	public boolean isOverClaim() {
		return isOverClaim;
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}
