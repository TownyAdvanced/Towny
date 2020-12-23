package com.palmergames.bukkit.towny.war.siegewar.siege;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.metadata.SiegeMetaDataController;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarTimeUtil;

public class SiegeController {

	@SuppressWarnings("unused")
	private final Towny towny;
	private final static Map<String, Siege> sieges = new ConcurrentHashMap<>();
	private static Map<UUID, Siege> townSiegeMap = new ConcurrentHashMap<>();

	public SiegeController() {
		towny = Towny.getPlugin();
	}

	public static void newSiege(String siegeName) {
		Siege siege = new Siege(siegeName);		

		sieges.put(siegeName.toLowerCase(), siege);
	}

	public static List<Siege> getSieges() {
		return new ArrayList<>(sieges.values());
	}

	public static Siege getSiege(String siegeName) throws NotRegisteredException {
		if(!sieges.containsKey(siegeName.toLowerCase())) {
			throw new NotRegisteredException("Siege not found");
		}
		return sieges.get(siegeName.toLowerCase());
	}

	public static void clearSieges() {
		sieges.clear();
		townSiegeMap.clear();
	}
	
	public static boolean saveSieges() {
		for (Siege siege : sieges.values()) {
			saveSiege(siege);
		}
		return true;
	}
	
	public static void saveSiege(Siege siege) {
		Town town = siege.getDefendingTown();
		SiegeMetaDataController.setSiegeName(town, siege.getName());
		SiegeMetaDataController.setNationUUID(town, siege.getAttackingNation().getUuid().toString());
		SiegeMetaDataController.setTownUUID(town, siege.getDefendingTown().getUUID().toString());
		SiegeMetaDataController.setFlagLocation(town, siege.getFlagLocation().getWorld().getName()
			+ "!" + siege.getFlagLocation().getX()
			+ "!" + siege.getFlagLocation().getY()
			+ "!" + siege.getFlagLocation().getZ());
		SiegeMetaDataController.setStatus(town, siege.getStatus().toString());
		SiegeMetaDataController.setPoints(town, siege.getSiegePoints());
		SiegeMetaDataController.setWarChestAmount(town, siege.getWarChestAmount());
		SiegeMetaDataController.setTownPlundered(town, siege.getTownPlundered());
		SiegeMetaDataController.setTownInvaded(town, siege.getTownInvaded());
		SiegeMetaDataController.setStartTime(town, siege.getStartTime());
		SiegeMetaDataController.setEndTime(town, siege.getScheduledEndTime());
		SiegeMetaDataController.setActualEndTime(town, siege.getActualEndTime());
		SiegeMetaDataController.setSiegeName(town, siege.getAttackingNation().getName() + "#vs#" + siege.getDefendingTown().getName());
	}

	public static void loadSiegeList() {
		for (Town town : TownyUniverse.getInstance().getTowns())
			if (hasSiege(town)) {
				String name = getSiegeName(town);
				if (name != null) {
					newSiege(name);
					setSiege(town, true);
					townSiegeMap.put(town.getUUID(), getSiege(town));
				}
			}
	}

	public static boolean loadSieges() {
		for (Siege siege : sieges.values()) {
			if (!loadSiege(siege)) {
				System.out.println("[SiegeWar] Loading Error: Could not read siege data '" + siege.getName() + "'.");
				return false;
			}
		}
		return true;		
	}
	
	public static boolean loadSiege(Siege siege) {
		String townName = siege.getName().split("#")[2];
		Town town = TownyUniverse.getInstance().getTown(townName);
		if (town == null)
			return false;
		siege.setDefendingTown(town);

		Nation nation = null;
		try {
			nation = TownyUniverse.getInstance().getDataSource().getNation(UUID.fromString(SiegeMetaDataController.getNationUUID(town)));
		} catch (NotRegisteredException ignored) {}
		if (nation == null)
			return false;
		siege.setAttackingNation(nation);
		
		if (SiegeMetaDataController.getFlagLocation(town).isEmpty())
			return false;
		String[] location = SiegeMetaDataController.getFlagLocation(town).split("!");
		World world = Bukkit.getWorld(location[0]);
		double x = Double.parseDouble(location[1]);
		double y = Double.parseDouble(location[2]);
		double z = Double.parseDouble(location[3]);		
		Location loc = new Location(world, x, y, z);
		siege.setFlagLocation(loc);

		if (SiegeMetaDataController.getStatus(town).isEmpty())
			return false;
		siege.setStatus(SiegeStatus.parseString(SiegeMetaDataController.getStatus(town)));		

		siege.setSiegePoints(SiegeMetaDataController.getPoints(town));
		siege.setWarChestAmount(SiegeMetaDataController.getWarChestAmount(town));
		siege.setTownPlundered(SiegeMetaDataController.townPlundered(town));
		siege.setTownInvaded(SiegeMetaDataController.townInvaded(town));

		if (SiegeMetaDataController.getStartTime(town) == 0l)
			return false;
		siege.setStartTime(SiegeMetaDataController.getStartTime(town));

		if (SiegeMetaDataController.getEndTime(town) == 0l)
			return false;
		siege.setScheduledEndTime(SiegeMetaDataController.getEndTime(town));

		siege.setActualEndTime(SiegeMetaDataController.getActualEndTime(town));
		return true;
	}

	//Remove a particular siege, and all associated data
	public static void removeSiege(Siege siege, SiegeSide refundSideIfSiegeIsActive) {
		//If siege is active, initiate siege immunity for town, and return war chest
		if(siege.getStatus().isActive()) {
			siege.setActualEndTime(System.currentTimeMillis());
			SiegeWarTimeUtil.activateSiegeImmunityTimer(siege.getDefendingTown(), siege);

			if(refundSideIfSiegeIsActive == SiegeSide.ATTACKERS)
				SiegeWarMoneyUtil.giveWarChestToAttackingNation(siege);
			else if (refundSideIfSiegeIsActive == SiegeSide.DEFENDERS)
				SiegeWarMoneyUtil.giveWarChestToDefendingTown(siege);
		}

		Town town = siege.getDefendingTown();
		//Remove siege from town
		setSiege(town, false);
		SiegeMetaDataController.removeSiegeMeta(town);
		//Remove siege from maps
		sieges.remove(siege.getName().toLowerCase());
		townSiegeMap.remove(town.getUUID());

		//Save town
		TownyUniverse.getInstance().getDataSource().saveTown(town);
		//Save attacking nation
		TownyUniverse.getInstance().getDataSource().saveNation(siege.getAttackingNation());
		siege = null;
	}

	public static void putTownInSiegeMap(Town town, Siege siege) {
		townSiegeMap.put(town.getUUID(), siege);
	}
	
	public static boolean hasSiege(Town town) {
		return hasSiege(town.getUUID());
	}
	
	public static boolean hasSiege(UUID uuid) {
		return townSiegeMap.containsKey(uuid);
	}
	
	public static boolean hasActiveSiege(Town town) {
		return hasSiege(town) && getSiege(town).getStatus().isActive(); 
	}
	
	public static boolean hasSieges(Nation nation) {
		return !getSieges(nation).isEmpty();
	}
	
	@Nullable
	public static List<Siege> getSieges(Nation nation) {
		List<Siege> siegeList = new ArrayList<>();
		for (Siege siege : sieges.values()) {
			if (siege.getAttackingNation().equals(nation))
				siegeList.add(siege);			
		}
		return siegeList;
	}
	
	@Nullable
	public static Siege getSiege(Town town) {
		if (hasSiege(town.getUUID()))
			return townSiegeMap.get(town.getUUID());
		return null;
	}
	
	@Nullable
	public static Siege getSiege(UUID uuid) {
		if (hasSiege(uuid))
			return townSiegeMap.get(uuid);
		return null;
	}
	
	@Nullable
	public static List<Siege> getSiegesByNationUUID(UUID uuid) {
		List<Siege> siegeList = new ArrayList<>();
		for (Siege siege : sieges.values()) {
			Town town = siege.getDefendingTown();
			if (UUID.fromString(SiegeMetaDataController.getNationUUID(town)).equals(uuid))
				siegeList.add(siege);
		}
		return siegeList;
	}
	
	@Nullable
	public static String getSiegeName(Town town) {
		return SiegeMetaDataController.getSiegeName(town);
	}
	
	public static void setSiege(Town town, boolean bool) {
		SiegeMetaDataController.setSiege(town, bool);
	}

	public static Set<Player> getPlayersInBannerControlSessions() {
		Set<Player> result = new HashSet<>();
		for (Siege siege : sieges.values()) {
			result.addAll(siege.getBannerControlSessions().keySet());
		}
		return result;
	}
    
}
