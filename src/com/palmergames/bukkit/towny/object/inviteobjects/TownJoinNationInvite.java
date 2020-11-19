package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.command.NationCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import org.bukkit.command.CommandSender;

import java.util.ArrayList;
import java.util.List;

public class TownJoinNationInvite extends AbstractInvite<Nation, Town> {
	
	public TownJoinNationInvite(CommandSender directSender, Town receiver, Nation sender) {
		super(directSender, receiver, sender);
	}

	@Override
	public void accept() throws TownyException {
		Town town = getReceiver();
		List<Town> towns = new ArrayList<>();
		towns.add(town);
		Nation nation = getSender();

		if(TownySettings.getWarSiegeEnabled()
			&& TownySettings.getWarCommonPeacefulTownsEnabled()
			&& (town.isPeaceful() || town.getDesiredPeacefulnessValue())) {
			//Player can only get here in some edge-case scenario/attempted exploit. So no attempted exception or messaging.
		} else {
			NationCommand.nationAdd(nation, towns);
		}
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
			TownyMessaging.sendPrefixedNationMessage(nation, Translation.of("msg_deny_invite", town.getName()));
		} else {
			TownyMessaging.sendPrefixedTownMessage(town, Translation.of("nation_revoke_invite", nation.getName()));
		}
	}
}
