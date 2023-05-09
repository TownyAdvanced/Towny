package com.palmergames.bukkit.towny.object.inviteobjects;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.command.TownCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.command.CommandSender;

public class PlayerJoinTownInvite extends AbstractInvite<Town, Resident> {

	public PlayerJoinTownInvite(CommandSender directSender, Resident receiver, Town sender) {
		super(directSender, receiver, sender);
	}

	@Override
	public void accept() throws TownyException {
		Resident resident = getReceiver();
		Town town = getSender();
		
		if(!resident.hasTown()){
			TownCommand.townAddResident(town, resident);
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_join_town", resident.getName()));
		} else {
			TownyMessaging.sendMsg(resident, Translatable.of("msg_err_already_res"));
		}
		resident.deleteReceivedInvite(this);
		town.deleteSentInvite(this);
	}

	@Override
	public void decline(boolean fromSender) {
		Resident resident = getReceiver();
		Town town = getSender();
		
		resident.deleteReceivedInvite(this);
		town.deleteSentInvite(this);
		
		if (!fromSender) {
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_deny_invite", resident.getName()));
			TownyMessaging.sendMsg(getReceiver(), Translatable.of("successful_deny"));
		} else {
			TownyMessaging.sendMsg(resident, Translatable.of("town_revoke_invite", town.getName()));
		}
	}
}
