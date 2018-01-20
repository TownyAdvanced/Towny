package com.palmergames.bukkit.towny.invites;

import java.util.List;

/**
 * @author Articdive
 */
public interface TownyInviteSender {
	List<Invite> getSentInvites();

	void newSentInvite(Invite invite);

	void deleteSentInvite(Invite invite);
}
