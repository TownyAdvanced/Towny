package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Nameable;

import java.util.Collection;

/**
 * Represents a class that can receive invitations.
 * 
 * @author Articdive
 */
public interface InviteReceiver extends Nameable {

	/**
	 * Gets the collection of invites received.
	 * 
	 * @return An unmodifiable collection of received invites.
	 */
	Collection<Invite> getReceivedInvites();

	/**
	 * Adds an invite to the received collection.
	 *
	 * @param invite The invite to add.
	 * @throws TooManyInvitesException When the invite cap is reached.   
	 */
	void newReceivedInvite(Invite invite) throws TooManyInvitesException;

	/**
	 * Removes an invite from the received collection.
	 * 
	 * @param invite The invite to remove.
	 */
	void deleteReceivedInvite(Invite invite);
}
