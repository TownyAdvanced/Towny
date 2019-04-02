package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.NationAddTownEvent;
import com.palmergames.bukkit.towny.event.NationRemoveTownEvent;
import com.palmergames.bukkit.towny.event.NationTagChangeEvent;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.invites.TownyAllySender;
import com.palmergames.bukkit.towny.invites.TownyInviteReceiver;
import com.palmergames.bukkit.towny.invites.TownyInviteSender;
import com.palmergames.bukkit.towny.invites.exceptions.TooManyInvitesException;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.war.flagwar.TownyWar;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

public class Nation extends TownyEconomyObject implements ResidentList, TownyInviteSender, TownyInviteReceiver, TownyAllySender {

	private static final String ECONOMY_ACCOUNT_PREFIX = TownySettings.getNationAccountPrefix();

	//private List<Resident> assistants = new ArrayList<Resident>();
	private List<Town> towns = new ArrayList<Town>();
	private List<Nation> allies = new ArrayList<Nation>();
	private List<Nation> enemies = new ArrayList<Nation>();
	private Town capital;
	private double taxes, spawnCost;
	private boolean neutral = false;
	private String nationBoard = "/nation set board [msg]", tag;
	public UUID uuid;
	private long registered;
	private Location nationSpawn;
	private boolean isPublic;

	public Nation(String name) {

		setName(name);
		tag = "";
        isPublic = TownySettings.getNationDefaultPublic();
	}

	public void setTag(String text) throws TownyException {

		if (text.length() > 4)
			throw new TownyException("Tag too long");
		this.tag = text.toUpperCase();
		if (this.tag.matches(" "))
			this.tag = "";
		Bukkit.getPluginManager().callEvent(new NationTagChangeEvent(this.tag));
		setChangedName(true);
	}

	public String getTag() {

		return tag;
	}

	public boolean hasTag() {

		return !tag.isEmpty();
	}

	public void addAlly(Nation nation) throws AlreadyRegisteredException {

		if (hasAlly(nation))
			throw new AlreadyRegisteredException();
		else {
			try {
				removeEnemy(nation);
			} catch (NotRegisteredException e) {
			}
			getAllies().add(nation);
		}
	}

	public boolean removeAlly(Nation nation) throws NotRegisteredException {

		if (!hasAlly(nation))
			throw new NotRegisteredException();
		else
			return getAllies().remove(nation);
	}

	public boolean removeAllAllies() {

		for (Nation ally : new ArrayList<Nation>(getAllies()))
			try {
				removeAlly(ally);
				ally.removeAlly(this);
			} catch (NotRegisteredException e) {
			}
		return getAllies().size() == 0;
	}

	public boolean hasAlly(Nation nation) {

		return getAllies().contains(nation);
	}

	public boolean IsAlliedWith(Nation nation) {

		return getAllies().contains(nation);
	}

	public void addEnemy(Nation nation) throws AlreadyRegisteredException {

		if (hasEnemy(nation))
			throw new AlreadyRegisteredException();
		else {
			try {
				removeAlly(nation);
			} catch (NotRegisteredException e) {
			}
			getEnemies().add(nation);
		}

	}

	public boolean removeEnemy(Nation nation) throws NotRegisteredException {

		if (!hasEnemy(nation))
			throw new NotRegisteredException();
		else
			return getEnemies().remove(nation);
	}

	public boolean removeAllEnemies() {

		for (Nation enemy : new ArrayList<Nation>(getEnemies()))
			try {
				removeEnemy(enemy);
				enemy.removeEnemy(this);
			} catch (NotRegisteredException e) {
			}
		return getAllies().size() == 0;
	}

	public boolean hasEnemy(Nation nation) {

		return getEnemies().contains(nation);
	}

	public List<Town> getTowns() {

		return towns;
	}

	public boolean isKing(Resident resident) {

		return hasCapital() ? getCapital().isMayor(resident) : false;
	}

	public boolean hasCapital() {

		return getCapital() != null;
	}

	public boolean hasAssistant(Resident resident) {

		return getAssistants().contains(resident);
	}

	public boolean isCapital(Town town) {

		return town == getCapital();
	}

	public boolean hasTown(String name) {

		for (Town town : towns)
			if (town.getName().equalsIgnoreCase(name))
				return true;
		return false;
	}

	public boolean hasTown(Town town) {

		return towns.contains(town);
	}

	public void addTown(Town town) throws AlreadyRegisteredException {

		if (hasTown(town))
			throw new AlreadyRegisteredException();
		else if (town.hasNation())
			throw new AlreadyRegisteredException();
		else {
			towns.add(town);
			town.setNation(this);
			
			BukkitTools.getPluginManager().callEvent(new NationAddTownEvent(town, this));
		}
	}

//	public void addAssistant(Resident resident) throws AlreadyRegisteredException {
//
//		if (hasAssistant(resident))
//			throw new AlreadyRegisteredException();
//		else
//			getAssistants().add(resident);
//	}

//	public void removeAssistant(Resident resident) throws NotRegisteredException {
//
//		if (!hasAssistant(resident))
//			throw new NotRegisteredException();
//		else
//			assistants.remove(resident);
//	}

	public void setCapital(Town capital) {

		this.capital = capital;
		try {
			TownyPerms.assignPermissions(capital.getMayor(), null);
		} catch (Exception e) {
			// Dummy catch to prevent errors on startup when setting nation.
		}
	}

	public Town getCapital() {

		return capital;
	}

	public Location getNationSpawn() throws TownyException {
		if(nationSpawn == null){
			throw new TownyException("Nation has not set a spawn location.");
		}

		return nationSpawn;
	}

	public boolean hasNationSpawn(){
		return (nationSpawn != null);
	}

	public void setNationSpawn(Location spawn) throws TownyException {
		Coord spawnBlock = Coord.parseCoord(spawn);

		TownBlock townBlock = TownyUniverse.getDataSource().getWorld(spawn.getWorld().getName()).getTownBlock(spawnBlock);
		if(TownySettings.getBoolean(ConfigNodes.GNATION_SETTINGS_CAPITAL_SPAWN)){
			if(this.capital == null){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_capital"));
			}
			if(!townBlock.hasTown()){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_capital"));
			}
			if(townBlock.getTown() != this.getCapital()){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_capital"));
			}
		} else {
			if(!townBlock.hasTown()){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_nationtowns"));
			}

			if(!towns.contains(townBlock.getTown())){
				throw new TownyException(TownySettings.getLangString("msg_err_spawn_not_within_nationtowns"));
			}
		}

		this.nationSpawn = spawn;
	}

	/**
	 * Only to be called from the Loading methods.
	 *
	 * @param nationSpawn
	 */
	public void forceSetNationSpawn(Location nationSpawn){
		this.nationSpawn = nationSpawn;
	}

	//TODO: Remove
	public boolean setAllegiance(String type, Nation nation) {

		try {
			if (type.equalsIgnoreCase("ally")) {
				removeEnemy(nation);
				addAlly(nation);
				if (!hasEnemy(nation) && hasAlly(nation))
					return true;
			} else if (type.equalsIgnoreCase("peaceful") || type.equalsIgnoreCase("neutral")) {
				removeEnemy(nation);
				removeAlly(nation);
				if (!hasEnemy(nation) && !hasAlly(nation))
					return true;
			} else if (type.equalsIgnoreCase("enemy")) {
				removeAlly(nation);
				addEnemy(nation);
				if (hasEnemy(nation) && !hasAlly(nation))
					return true;
			}
		} catch (AlreadyRegisteredException x) {
			return false;
		} catch (NotRegisteredException e) {
			return false;
		}

		return false;
	}

//	public void setAssistants(List<Resident> assistants) {
//
//		this.assistants = assistants;
//	}
//
	public List<Resident> getAssistants() {

		List<Resident> assistants = new ArrayList<Resident>();
		
		for (Town town: towns)
		for (Resident assistant: town.getResidents()) {
			if (assistant.hasNationRank("assistant"))
				assistants.add(assistant);
		}
		return assistants;
	}

	public void setEnemies(List<Nation> enemies) {

		this.enemies = enemies;
	}

	public List<Nation> getEnemies() {

		return enemies;
	}

	public void setAllies(List<Nation> allies) {

		this.allies = allies;
	}

	public List<Nation> getAllies() {

		return allies;
	}

	public int getNumTowns() {

		return towns.size();
	}

	public int getNumResidents() {

		int numResidents = 0;
		for (Town town : getTowns())
			numResidents += town.getNumResidents();
		return numResidents;
	}

	public void removeTown(Town town) throws EmptyNationException, NotRegisteredException {

		if (!hasTown(town))
			throw new NotRegisteredException();
		else {

			boolean isCapital = town.isCapital();
			remove(town);

			if (getNumTowns() == 0) {
				throw new EmptyNationException(this);
			} else if (isCapital) {
				int numResidents = 0;
				Town tempCapital = null;
				for (Town newCapital : getTowns())
					if (newCapital.getNumResidents() > numResidents) {
						tempCapital = newCapital;
						numResidents = newCapital.getNumResidents();
					}

				if (tempCapital != null) {
					setCapital(tempCapital);
				}

			}
		}
	}

	private void remove(Town town) {

		//removeAssistantsIn(town);
		try {
			town.setNation(null);
		} catch (AlreadyRegisteredException e) {
		}
		towns.remove(town);
		
		BukkitTools.getPluginManager().callEvent(new NationRemoveTownEvent(town, this));
	}

	private void removeAllTowns() {

		for (Town town : new ArrayList<Town>(towns))
			remove(town);
	}

//	public boolean hasAssistantIn(Town town) {
//
//		for (Resident resident : town.getResidents())
//			if (hasAssistant(resident))
//				return true;
//		return false;
//	}
//
//	private void removeAssistantsIn(Town town) {
//
//		for (Resident resident : new ArrayList<Resident>(town.getResidents()))
//			if (hasAssistant(resident))
//				try {
//					removeAssistant(resident);
//				} catch (NotRegisteredException e) {
//				}
//	}

	public void setTaxes(double taxes) {

		if (taxes > TownySettings.getMaxTax())
			this.taxes = TownySettings.getMaxTax();
		else
			this.taxes = taxes;
	}

	public double getTaxes() {

		setTaxes(taxes); //make sure the tax level is right.
		return taxes;
	}

	public void clear() {

		//TODO: Check cleanup
		removeAllAllies();
		removeAllEnemies();
		removeAllTowns();
		capital = null;
	}

	/**
	 * Method for rechecking town distances to a new nation capital/moved nation capital homeblock.
	 * @throws TownyException
	 */
	public void recheckTownDistance() throws TownyException {
		if(capital != null) {
			if (TownySettings.getNationRequiresProximity() > 0) {
				final Coord capitalCoord = capital.getHomeBlock().getCoord();
				Iterator it = towns.iterator();
				while(it.hasNext()) {
					Town town = (Town) it.next();
					Coord townCoord = town.getHomeBlock().getCoord();
					if (!capital.getHomeBlock().getWorld().getName().equals(town.getHomeBlock().getWorld().getName())) {
						it.remove();
						continue;
					}

					double distance = Math.sqrt(Math.pow(capitalCoord.getX() - townCoord.getX(), 2) + Math.pow(capitalCoord.getZ() - townCoord.getZ(), 2));
					if (distance > TownySettings.getNationRequiresProximity()) {
						town.setNation(null);
						it.remove();
						continue;
					}
				}
			}
		}
	}

	public void setNeutral(boolean neutral) throws TownyException {

		if (!TownySettings.isDeclaringNeutral() && neutral)
			throw new TownyException(TownySettings.getLangString("msg_err_fight_like_king"));
		else {
			if (neutral) {
				for (Resident resident : getResidents()) {
					TownyWar.removeAttackerFlags(resident.getName());
				}
			}
			this.neutral = neutral;
		}
	}

	public boolean isNeutral() {

		return neutral;
	}

	public void setKing(Resident king) throws TownyException {

		if (!hasResident(king))
			throw new TownyException(TownySettings.getLangString("msg_err_king_not_in_nation"));
		if (!king.isMayor())
			throw new TownyException(TownySettings.getLangString("msg_err_new_king_notmayor"));
		setCapital(king.getTown());
	}

	public boolean hasResident(Resident resident) {

		for (Town town : getTowns())
			if (town.hasResident(resident))
				return true;
		return false;
	}
	
	public void collect(double amount) throws EconomyException {
		
		if (TownySettings.isUsingEconomy()) {
			double bankcap = TownySettings.getNationBankCap();
			if (bankcap > 0) {
				if (amount + this.getHoldingBalance() > bankcap) {
					TownyMessaging.sendNationMessage(this, String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
					return;
				}
			}
			
			this.collect(amount, null);
		}

	}

	public void withdrawFromBank(Resident resident, int amount) throws EconomyException, TownyException {

		//if (!isKing(resident))// && !hasAssistant(resident))
		//	throw new TownyException(TownySettings.getLangString("msg_no_access_nation_bank"));

		if (TownySettings.isUsingEconomy()) {
			if (!payTo(amount, resident, "Nation Withdraw"))
				throw new TownyException(TownySettings.getLangString("msg_err_no_money"));
		} else
			throw new TownyException(TownySettings.getLangString("msg_err_no_economy"));
	}

	@Override
	public List<Resident> getResidents() {

		List<Resident> out = new ArrayList<Resident>();
		for (Town town : getTowns())
			out.addAll(town.getResidents());
		return out;
	}

	@Override
	public List<String> getTreeString(int depth) {

		List<String> out = new ArrayList<String>();
		out.add(getTreeDepth(depth) + "Nation (" + getName() + ")");
		out.add(getTreeDepth(depth + 1) + "Capital: " + getCapital().getName());
		
		List<Resident> assistants = getAssistants();
		
		if (assistants.size() > 0)
			out.add(getTreeDepth(depth + 1) + "Assistants (" + assistants.size() + "): " + Arrays.toString(assistants.toArray(new Resident[0])));
		if (getAllies().size() > 0)
			out.add(getTreeDepth(depth + 1) + "Allies (" + getAllies().size() + "): " + Arrays.toString(getAllies().toArray(new Nation[0])));
		if (getEnemies().size() > 0)
			out.add(getTreeDepth(depth + 1) + "Enemies (" + getEnemies().size() + "): " + Arrays.toString(getEnemies().toArray(new Nation[0])));
		out.add(getTreeDepth(depth + 1) + "Towns (" + getTowns().size() + "):");
		for (Town town : getTowns())
			out.addAll(town.getTreeString(depth + 2));
		return out;
	}

	@Override
	public boolean hasResident(String name) {

		for (Town town : getTowns())
			if (town.hasResident(name))
				return true;
		return false;
	}

    @Override
    protected World getBukkitWorld() {
        if (hasCapital() && getCapital().hasWorld()) {
            return BukkitTools.getWorld(getCapital().getWorld().getName());
        } else {
            return super.getBukkitWorld();
        }
    }


	@Override
	public String getEconomyName() {
		return StringMgmt.trimMaxLength(Nation.ECONOMY_ACCOUNT_PREFIX + getName(), 32);
	}

	@Override
	public List<Resident> getOutlaws() {

		List<Resident> out = new ArrayList<Resident>();
		for (Town town : getTowns())
			out.addAll(town.getOutlaws());
		return out;
	}

	public UUID getUuid() {
		return uuid;
	}

	public void setUuid(UUID uuid) {
		this.uuid = uuid;
	}

	public boolean hasValidUUID() {
		if (uuid != null) {
			return true;
		} else {
			return false;
		}
	}

	public long getRegistered() {
		return registered;
	}

	public void setRegistered(long registered) {
		this.registered = registered;
	}

	@Override
	public List<Invite> getReceivedInvites() {
		return receivedinvites;
	}

	@Override
	public void newReceivedInvite(Invite invite) throws TooManyInvitesException {
		if (receivedinvites.size() <= (InviteHandler.getReceivedInvitesMaxAmount(this) -1)) {
			receivedinvites.add(invite);
		} else {
			throw new TooManyInvitesException(String.format(TownySettings.getLangString("msg_err_nation_has_too_many_requests"),this.getName()));
		}
	}

	@Override
	public void deleteReceivedInvite(Invite invite) {
		receivedinvites.remove(invite);
	}

	@Override
	public List<Invite> getSentInvites() {
		return sentinvites;
	}

	@Override
	public void newSentInvite(Invite invite) throws TooManyInvitesException {
		if (sentinvites.size() <= (InviteHandler.getSentInvitesMaxAmount(this) -1)) {
			sentinvites.add(invite);
		} else {
			throw new TooManyInvitesException(TownySettings.getLangString("msg_err_nation_sent_too_many_invites"));
		}
	}

	@Override
	public void deleteSentInvite(Invite invite) {
		sentinvites.remove(invite);
	}

	private List<Invite> receivedinvites = new ArrayList<Invite>();
	private List<Invite> sentinvites = new ArrayList<Invite>();
	private List<Invite> sentallyinvites = new ArrayList<Invite>();


	@Override
	public void newSentAllyInvite(Invite invite) throws TooManyInvitesException {
		if (sentallyinvites.size() <= InviteHandler.getSentAllyRequestsMaxAmount(this) -1) {
			sentallyinvites.add(invite);
		} else {
			throw new TooManyInvitesException(TownySettings.getLangString("msg_err_nation_sent_too_many_requests"));
		}
	}

	@Override
	public void deleteSentAllyInvite(Invite invite) {
		sentallyinvites.remove(invite);
	}

	@Override
	public List<Invite> getSentAllyInvites() {
		return sentallyinvites;
	}
	
	public void setNationBoard(String nationBoard) {

		this.nationBoard = nationBoard;
	}

	public String getNationBoard() {
		return nationBoard;
	}

    public void setPublic(boolean isPublic) {

        this.isPublic = isPublic;
    }

    public boolean isPublic() {

        return isPublic;
    }
    
	public void setSpawnCost(double spawnCost) {

		this.spawnCost = spawnCost;
	}

	public double getSpawnCost() {

		return spawnCost;
	}
}
