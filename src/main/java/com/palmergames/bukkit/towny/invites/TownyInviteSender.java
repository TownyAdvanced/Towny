package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;

import java.util.List;

/**
 * @author Articdive
 */
public interface TownyInviteSender {
	String getName();

	List<Invite> getSentInvites();

	void newSentInvite(Invite invite) throws TooManyInvitesException;

	void deleteSentInvite(Invite invite);
}
