package com.palmergames.bukkit.towny.object;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.InviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;

public class Alliance implements Nameable, InviteSender, ResidentList {

	private UUID uuid;
	private String name;
	private List<UUID> membersUUIDs = new ArrayList<>();
	private List<UUID> enemiesUUIDs = new ArrayList<>();
	private long registered;
	private Nation founder;
	private UUID founderUUID;
	private final List<Invite> sentAllianceInvites = new ArrayList<>();

	public Alliance(UUID uuid) {
		this.setUUID(uuid);
	}
	
	public Alliance(UUID uuid, String name, Nation founder) {
		this.setUUID(uuid);
		this.setName(name);
		this.setFounder(founder);
	}

	/**
	 * @return the uuid
	 */
	public UUID getUUID() {
		return uuid;
	}

	/**
	 * @param uuid the uuid to set
	 */
	public void setUUID(UUID uuid) {
		this.uuid = uuid;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * @return the founder
	 */
	public Nation getFounder() {
		return founder;
	}

	/**
	 * @param founder the founder to set
	 */
	public void setFounder(Nation founder) {
		this.founder = founder;
	}

	/**
	 * @return the founderUUID
	 */
	public UUID getFounderUUID() {
		return founderUUID;
	}

	/**
	 * @param founderUUID the founderUUID to set
	 */
	public void setFounderUUID(UUID founderUUID) {
		this.founderUUID = founderUUID;
	}

	/**
	 * @return the registered
	 */
	public long getRegistered() {
		return registered;
	}

	/**
	 * @param registered the registered to set
	 */
	public void setRegistered(long registered) {
		this.registered = registered;
	}

	/**
	 * @return the members as a list of Nations
	 */
	public List<Nation> getMembers() {
		return membersUUIDs.stream()
			.map(uuid -> TownyAPI.getInstance().getNation(uuid))
			.collect(Collectors.toList());
	}

	/**
	 * @return the members list of UUIDs
	 */
	public List<UUID> getMembersUUIDs() {
		return membersUUIDs;
	}

	/**
	 * @param members the members to set
	 */
	public void setMembersUUIDs(List<UUID> membersUUIDs) {
		this.membersUUIDs = membersUUIDs;
	}

	public boolean addMember(Nation nation) {
		return this.membersUUIDs.add(nation.getUUID());
	}
	
	public boolean removeMember(Nation nation) {
		return this.membersUUIDs.remove(nation.getUUID());
	}
	
	public boolean hasNation(Nation nation) {
		return this.membersUUIDs.contains(nation.getUUID());
	}

	/**
	 * @return the enemies as a list of Alliances
	 */
	public List<Alliance> getEnemies() {
		return enemiesUUIDs.stream()
			.map(uuid -> TownyAPI.getInstance().getAlliance(uuid))
			.collect(Collectors.toList());
	}
	
	/**
	 * @return the enemiesUUIDs
	 */
	public List<UUID> getEnemiesUUIDs() {
		return enemiesUUIDs;
	}

	public boolean hasEnemy(Alliance alliance) {
		return enemiesUUIDs.contains(alliance.getUUID());
	}
	
	/**
	 * @param enemiesUUIDs the enemiesUUIDs to set
	 */
	public void setEnemiesUUIDs(List<UUID> enemiesUUIDs) {
		this.enemiesUUIDs = enemiesUUIDs;
	}

	public boolean addEnemy(Alliance alliance) {
		return this.enemiesUUIDs.add(alliance.getUUID());
	}
	
	public boolean removeEnemy(Alliance alliance) {
		return this.enemiesUUIDs.remove(alliance.getUUID());
	}
	
	public List<Town> getTowns() {
		List<Town> towns = new ArrayList<>();
		for (Nation nation : new ArrayList<>(getMembers())) {
			towns.addAll(nation.getTowns());
		}
		return towns;
	}

	public void save() {
		TownyUniverse.getInstance().getDataSource().saveAlliance(this);
	}
	
	public boolean hasPlayer(Player player) {
		return hasResident(TownyAPI.getInstance().getResident(player));
	}

	public boolean hasResident(Resident res) {
		if (res == null || !res.hasTown())
			return false;

		return hasTown(res.getTownOrNull());
	}

	@Override
	public boolean hasResident(String name) {
		for (Town town : getTowns())
			if (town.hasResident(name))
				return true;
		return false;
	}

	public boolean hasTown(Town town) {
		if (town == null || !town.hasNation())
			return false;
		
		return hasNation(town.getNationOrNull());
	}
	
	public boolean hasTownBlock(TownBlock townBlock) {
		if (townBlock == null || townBlock.getTownOrNull() == null)
			return false;
		
		return hasTown(townBlock.getTownOrNull());
	}

	@Override
	public Collection<Invite> getSentInvites() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void newSentInvite(Invite invite) throws TooManyInvitesException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void deleteSentInvite(Invite invite) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public Collection<Resident> getResidents() {
		List<Resident> out = new ArrayList<>();
		for (Town town : getTowns())
			out.addAll(town.getResidents());
		return Collections.unmodifiableList(out);
	}

	@Override
	public Collection<Resident> getOutlaws() {
		List<Resident> out = new ArrayList<>();
		for (Town town : getTowns())
			out.addAll(town.getOutlaws());
		return Collections.unmodifiableList(out);
	}

	public void newSentAllianceInvite(Invite invite) throws TooManyInvitesException {
		if (sentAllianceInvites.size() <= InviteHandler.getSentAllyRequestsMaxAmount(this) - 1) {
			sentAllianceInvites.add(invite);
		} else {
			throw new TooManyInvitesException(Translation.of("msg_err_alliace_sent_too_many_requests"));
		}
	}
	
	public void deleteSentAllianceInvite(Invite invite) {
		sentAllianceInvites.remove(invite);
	}
	
	public List<Invite> getSentAllianceInvites() {
		return Collections.unmodifiableList(sentAllianceInvites);
	}

	public Collection<TownBlock> getTownBlocks() {
		List<TownBlock> townBlocks = new ArrayList<>();
		for (Town town : this.getTowns())
			townBlocks.addAll(town.getTownBlocks());
		
		return Collections.unmodifiableCollection(townBlocks);
	}
}
