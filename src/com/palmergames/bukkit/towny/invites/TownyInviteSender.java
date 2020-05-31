package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Nameable;

import java.util.List;

/**
 * @author Articdive
 */
public interface TownyInviteSender extends Nameable {

	List<Invite> getSentInvites();

	void newSentInvite(Invite invite) throws TooManyInvitesException;

	void deleteSentInvite(Invite invite);
}
