package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.CommandSender;

public class TownJoinNationInvite extends AbstractInvite<Nation, Town> {
	
	public TownJoinNationInvite(CommandSender directSender, Town receiver, Nation sender) {
		super(directSender, receiver, sender);
	}

	@Override
	public void accept() throws TownyException {
		Town town = getReceiver();
		Nation nation = getSender();
		NationCommand.nationAdd(nation, town);
		// Message handled in nationAdd()
		town.deleteReceivedInvite(this);
		nation.deleteSentInvite(this);
	}

	@Override
	public void decline(boolean fromSender) {
		Town town = getReceiver();
		Nation nation = getSender();
		town.deleteReceivedInvite(this);
		nation.deleteSentInvite(this);
		if (!fromSender) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_deny_invite", town.getName()));
		} else {
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("nation_revoke_invite", nation.getName()));
		}
	}
}
