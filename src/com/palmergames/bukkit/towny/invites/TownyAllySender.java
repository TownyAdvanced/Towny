package com.palmergames.bukkit.towny.invites;

import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;

import java.util.List;

public interface TownyAllySender {
	void newSentAllyInvite(Invite invite) throws TooManyInvitesException ;
	void deleteSentAllyInvite(Invite invite);
	List<Invite> getSentAllyInvites();
}
