package com.palmergames.bukkit.towny.event.nation;

import com.palmergames.bukkit.towny.event.CancellableTownyEvent;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.event.HandlerList;
import org.jetbrains.annotations.NotNull;

/**
 * Cancellable event that gets fired before a town is invited to a nation.
 * @since 0.100.2.14
 */
public class NationPreInviteTownEvent extends CancellableTownyEvent {
	private static final HandlerList HANDLER_LIST = new HandlerList();
	
	private final Invite invite;

    public NationPreInviteTownEvent(Invite invite) {
        this.invite = invite;
        setCancelMessage("You cannot invite this town to your nation.");
    }

    public Invite getInvite() {
        return invite;
    }

    /**
     * Convenience method for getting the town that was invited.
     * @return The {@link Town} that was invited.
     */
    public Town getInvitedTown() {
        return (Town) invite.getReceiver();
    }

    /**
     * Convenience method for getting the town that sent the invite.
     * @return The {@link Town} that the resident is invited to join.
     */
    public Nation getNation() {
        return (Nation) invite.getSender();
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