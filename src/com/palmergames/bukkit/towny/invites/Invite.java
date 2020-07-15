package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.exceptions.TownyException;

/**
 * An object that represents an invitation.
 * 
 * @author Articdive
 */
public interface Invite {

	/**
	 * @return - Playername of who sent the invite or null (Console).
	 */
	String getDirectSender();

	/**
	 * @return - Resident, Town or Nation as a TownyEconomyObject.
	 */
	TownyInviteReceiver getReceiver();

	/**
	 * @return - Resident, Town or Nation as TownyEconomyObject.
	 */
	TownyInviteSender getSender();

	/**
	 * @throws TownyException - Sends errors back up to be processed by the caller.
	 */
	void accept() throws TownyException;

	/**
	 * @param fromSender - Tells if invite was revoked (true) or declined (false).
	 */
	void decline(boolean fromSender);
}
