package com.palmergames.bukkit.towny.object.settingsbuilders;

import java.util.List;
import java.util.stream.Collectors;

import org.bukkit.command.CommandSender;

import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.util.StringMgmt;

public class ResidentSettingsBuilder {

	private final Resident resident;
	private CommandSender sender;
	private TownyLogger logger = TownyLogger.getInstance();

	public ResidentSettingsBuilder(Resident resident) {
		this.resident = resident;
	}

	public ResidentSettingsBuilder bySender(CommandSender sender) {
		this.sender = sender;
		return this;
	}

	public ResidentSettingsBuilder setTown(Town town) {
		try {
			this.resident.setTown(town);
		} catch (AlreadyRegisteredException e) {
			return this;
		}
		logger.logSettingChange(resident, sender, "town", town.getName());
		return this;
	}

	public ResidentSettingsBuilder setTownRanks(List<String> ranks) {
		resident.setTownRanks(ranks);
		logger.logSettingChange(resident, sender, "town ranks", StringMgmt.join(ranks, ", "));
		return this;
	}

	public ResidentSettingsBuilder setNationRanks(List<String> ranks) {
		resident.setNationRanks(ranks);
		logger.logSettingChange(resident, sender, "nation ranks", StringMgmt.join(ranks, ", "));
		return this;
	}

	public ResidentSettingsBuilder setLastOnline(long lastOnline) {
		resident.setLastOnline(lastOnline);
		logger.logSettingChange(resident, sender, "last online", lastOnline);
		return this;
	}

	public ResidentSettingsBuilder setRegistered(long registered) {
		resident.setRegistered(registered);
		logger.logSettingChange(resident, sender, "registered", registered);
		return this;
	}

	public ResidentSettingsBuilder setTitle(String title) {
		resident.setTitle(title);
		logger.logSettingChange(resident, sender, "title", title);
		return this;
	}

	public ResidentSettingsBuilder setSurname(String surname) {
		resident.setSurname(surname);
		logger.logSettingChange(resident, sender, "surname", surname);
		return this;
	}

	public ResidentSettingsBuilder setAbout(String about) {
		resident.setAbout(about);
		logger.logSettingChange(resident, sender, "about", about);
		return this;
	}

	public ResidentSettingsBuilder setFriends(List<Resident> newFriends) {
		resident.setFriends(newFriends);
		logger.logSettingChange(resident, sender, "friends", 
				StringMgmt.join(newFriends.stream().map(Resident::getName).collect(Collectors.toList()),", "));
		return this;
	}

	public ResidentSettingsBuilder removeFriend(Resident friend) {
		if (!resident.hasFriend(friend))
			return this;
		resident.removeFriend(friend);
		logger.logSettingChange(resident, sender, "removed friend", friend.getName());
		return this;
	}

	public ResidentSettingsBuilder addFriend(Resident friend){
		if (resident.hasFriend(friend) || resident.equals(friend) || friend.isNPC())
			return this;
		resident.addFriend(friend);
		logger.logSettingChange(resident, sender, "added friend", friend.getName());
		return this;
	}

	public void save() {
		resident.save();
	}
}
