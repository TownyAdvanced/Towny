package com.palmergames.bukkit.towny.invites;

import java.util.List;

/**
 * @author Articdive
 */
public interface TownyInviteReceiver {
	List<Invite> getReceivedInvites();

	void newReceivedInvite(Invite invite);

	void deleteReceivedInvite(Invite invite);
}
