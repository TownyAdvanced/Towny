package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Bukkit;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

/**
 * Cancellable event that gets fired before a resident is invited to a town.
 * @since 0.96.7.12
 */
public class TownPreInvitePlayerEvent extends Event implements Cancellable {
    private static final HandlerList handlers = new HandlerList();
    private final Invite invite;
    private boolean cancelled = false;
    private String cancelMessage = "Sorry, but this event was cancelled.";

    public TownPreInvitePlayerEvent(Invite invite) {
        super(!Bukkit.getServer().isPrimaryThread());
        this.invite = invite;
    }

    public Invite getInvite() {
        return invite;
    }

    /**
     * Convenience method for getting the resident that was invited.
     * @return The {@link Resident} that was invited.
     */
    public Resident getInvitedResident() {
        return (Resident) invite.getReceiver();
    }

    /**
     * Convenience method for getting the town that sent the invite.
     * @return The {@link Town} that the resident is invited to join.
     */
    public Town getTown() {
        return (Town) invite.getSender();
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public String getCancelMessage() {
        return cancelMessage;
    }

    public void setCancelMessage(String cancelMessage) {
        this.cancelMessage = cancelMessage;
    }

    public static HandlerList getHandlerList() {
        return handlers;
    }

    @Override
    public HandlerList getHandlers() {
        return handlers;
    }
}