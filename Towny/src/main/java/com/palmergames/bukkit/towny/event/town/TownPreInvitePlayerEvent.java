package com.palmergames.bukkit.towny.event.town;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Cancellable event that gets fired before a resident is invited to a town.
 * @since 0.96.7.12
 */
public class TownPreInvitePlayerEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
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

	public static HandlerList getHandlerList() {
		return HANDLER_LIST;
	}

	@NotNull
	@Override
	public HandlerList getHandlers() {
		return HANDLER_LIST;
	}
}