package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;


public class TownPreUnclaimEvent extends Event implements Cancellable {

    private static final HandlerList handlers = new HandlerList();
    private final TownBlock townBlock;
    private final Town town;
    private boolean isCancelled = false;
    private String cancelMessage = Translation.of("msg_err_town_unclaim_canceled");

    @NotNull
    @Override
    public HandlerList getHandlers() {
        return getHandlerList();
    }
    
    public static HandlerList getHandlerList() {
		return handlers;
	}

    /**
     * Event thrown prior to a {@link TownBlock} being unclaimed by a {@link Town}.
     * <p>
     * This is cancellable but it is probably not a good idea to do
     * so without testing.
     *
     * @param town The Town unclaiming the TownBlock.
     * @param townBlock The TownBlock that will be unclaimed.
     */
    public TownPreUnclaimEvent(Town town, TownBlock townBlock) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.town = town;
        this.townBlock = townBlock;

        // Don't even bother with things if town is null.
        if (this.town == null){
            this.cancelMessage = Translation.of("msg_area_not_recog");
            setCancelled(true);
        }
    }

    /**
     * @return Check if the event is cancelled or not, yet.
     */
    @Override
    public boolean isCancelled() {
        return isCancelled;
    }

    /**
     * @param cancelled The incoming boolean that sets if the event is to be cancelled.
     */
    @Override
    public void setCancelled(boolean cancelled) {
        isCancelled = cancelled;
    }
    
    /**
     * @return the {@link Town}.
     */
    @Nullable
    public Town getTown() {
        return town;
    }
    
    /**
    * @return the soon-to-be unclaimed {@link TownBlock}.
    */
    public TownBlock getTownBlock() {
       return townBlock;
    }

    /**
     * @return the cancellation message.
     */
    public String getCancelMessage() {
        return cancelMessage;
    }

    /**
     * Sets the cancellation message. If no message is passed, the previous listener's message or
     * the default value will be used.
     *
     * @param cancelMessage Message to pass to the event when canceling.
     */
    public void setCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }
}