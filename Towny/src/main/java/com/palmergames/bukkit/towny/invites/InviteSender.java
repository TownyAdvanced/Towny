package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Nameable;

import java.util.Collection;

/**
 * Represents a class that can receive invitations. 
 * 
 * @author Articdive
 */
public interface InviteSender extends Nameable {

	/**
	 * Gets the collection of outgoing invites.
	 * 
	 * @return The outgoing invites.
	 */
	Collection<Invite> getSentInvites();

	/**
	 * Adds an outgoing invite.
	 * 
	 * @param invite The outgoing invite to add.
	 * @throws TooManyInvitesException When the outgoing invite limit has been reached.
	 */
	void newSentInvite(Invite invite) throws TooManyInvitesException;

	/**
	 * Removes an outgoing invite.
	 * 
	 * @param invite The invite to remove from the outgoing collection.
	 */
	void deleteSentInvite(Invite invite);
}
