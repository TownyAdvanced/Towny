package com.palmergames.bukkit.towny.invites;

/**
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

}
