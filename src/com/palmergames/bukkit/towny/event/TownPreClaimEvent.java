package com.palmergames.bukkit.towny.event;

import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Runs before town banks are charged
 * Provides raw town block
 * */
public class TownPreClaimEvent extends Event implements Cancellable{

    private static final HandlerList handlers = new HandlerList();
    private TownBlock townBlock;
    private Town town;
    private boolean isCancelled = false;

    @Override
    public HandlerList getHandlers() {

        return handlers;
    }

    public static HandlerList getHandlerList() {

        return handlers;
    }

    public TownPreClaimEvent(Town _town, TownBlock _townBlock) {
        this.town = _town;
        this.townBlock = _townBlock;
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
}
