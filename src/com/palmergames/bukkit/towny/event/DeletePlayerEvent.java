package com.palmergames.bukkit.towny.event;

import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

public class DeletePlayerEvent extends Event {
    private static final HandlerList handlers = new HandlerList();

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
}
