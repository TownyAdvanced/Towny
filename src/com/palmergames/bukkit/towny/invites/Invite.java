package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.exceptions.TownyException;
import org.bukkit.command.CommandSender;

/**
 * An object that represents an invitation.
 * 
 * @author Articdive
 */
public interface Invite {
	/**
	 * Gets the sender of the invitation
	 * 
	 * @return - The {@link CommandSender} of the invitation.
	 */
	CommandSender getDirectSender();

	/**
	 * Gets the receiver of the confirmation.
	 * 
	 * @return - The {@link InviteReceiver} object receiving the invite.
	 */
	InviteReceiver getReceiver();

	/**
	 * Gets the sender of the confirmation.
	 * 
	 * @return - The {@link InviteSender} object that sent the invite.
	 */
	InviteSender getSender();

	/**
	 * Runs the accept code for the given confirmation.
	 * 
	 * @throws TownyException - Sends errors back up to be processed by the caller.
	 */
	void accept() throws TownyException;

	/**
	 * Runs the reject code for the given confirmation.
	 * 
	 * @param fromSender - Tells if invite was revoked (true) or declined (false).
	 */
	void decline(boolean fromSender);
}
