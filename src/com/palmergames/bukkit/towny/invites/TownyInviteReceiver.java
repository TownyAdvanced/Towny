package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.object.Nameable;

import java.util.List;

/**
 * @author Articdive
 */
public interface TownyInviteReceiver extends Nameable {

	List<Invite> getReceivedInvites();

	void newReceivedInvite(Invite invite) throws TooManyInvitesException;

	void deleteReceivedInvite(Invite invite);
}
