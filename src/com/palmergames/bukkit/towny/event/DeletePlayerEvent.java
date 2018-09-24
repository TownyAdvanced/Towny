package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DeletePlayerEvent extends Event implements Cancellable{
    private static final HandlerList handlers = new HandlerList();
    public boolean cancelled = false;
    private String playerName;

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    public DeletePlayerEvent(String player) { this.playerName = player;
    }

    /**
     * @return the deleted player's name.
     */
    public String getPlayerName() {
        return playerName;
    }

    @Override
    public boolean isCancelled() {
        return cancelled;
    }

    @Override
    public void setCancelled(boolean b) {
        this.cancelled = b;
    }
}
