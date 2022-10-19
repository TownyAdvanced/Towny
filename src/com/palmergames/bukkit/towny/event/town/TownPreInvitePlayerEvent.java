package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

/**
 * Cancellable event that gets fired before a resident is invited to a town.
 * @since 0.96.7.12
 */
public class TownPreInvitePlayerEvent extends CancellableTownyEvent {
    private final Invite invite;

    public TownPreInvitePlayerEvent(Invite invite) {
        this.invite = invite;
        setCancelMessage("You cannot invite this player to your town.");
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
}