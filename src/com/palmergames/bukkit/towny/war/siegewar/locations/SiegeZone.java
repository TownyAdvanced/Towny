package com.palmergames.bukkit.towny.war.siegewar.locations;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class represents a "Siege Zone".
 *
 * A siege zone is an "attack front" of a siege, a particular zone where one nation is attacking the town.
 * The zone is centred on the "Siege Banner", which was placed when the attack started.
 * 
 * This class keeps track of the 'siege points', which usually determine who wins a siege.
 * 
 * The defending town requires negative siege points to win.
 * An attacking nation requires positive siegepoints to win.
 *
 * @author Goosius
 */
public class SiegeZone {

    private Location siegeBannerLocation;
    private Nation attackingNation;
    private Town defendingTown;
    private int siegePoints;
	private double warChestAmount;
	private List<Resident> bannerControllingResidents;
	private SiegeSide bannerControllingSide;
	private Map<Player, BannerControlSession> bannerControlSessions;

	public SiegeZone() {
        attackingNation = null;
        defendingTown = null;
        siegePoints = 0;
        siegeBannerLocation = null;
		warChestAmount = 0;
		bannerControllingResidents = new ArrayList<>();
		bannerControllingSide = SiegeSide.NOBODY;
		bannerControlSessions = new HashMap<>();
    }

    public SiegeZone(Nation attackingNation, Town defendingTown) {
        this.defendingTown = defendingTown;
        this.attackingNation = attackingNation;
        siegePoints = 0;
        siegeBannerLocation = null;
		warChestAmount = 0;
		bannerControllingResidents = new ArrayList<>();
		bannerControllingSide = SiegeSide.NOBODY;
		bannerControlSessions = new HashMap<>();
    }

    public String getName() {
        return attackingNation.getName().toLowerCase() + "#vs#" + defendingTown.getName().toLowerCase();
    }

    public static String generateName(String attackingNationName, String defendingTownName) {
        return attackingNationName.toLowerCase() + "#vs#" + defendingTownName.toLowerCase();
    }
    
    public static String[] generateTownAndNationName(String siegeZoneName) {
        return siegeZoneName.split("#vs#");
    }

    public com.palmergames.bukkit.towny.war.siegewar.locations.Siege getSiege() {
        return defendingTown.getSiege();
    }

    public Nation getAttackingNation() {
        return attackingNation;
    }

    public Location getFlagLocation() {
        return siegeBannerLocation;
    }

    public void setFlagLocation(Location location) {
        this.siegeBannerLocation = location;
    }

    public Integer getSiegePoints() {
        return siegePoints;
    }

    public void setSiegePoints(int siegePoints) {
        this.siegePoints = siegePoints;
    }
    
    public void setAttackingNation(Nation attackingNation) {
        this.attackingNation = attackingNation;
    }

    public void setDefendingTown(Town defendingTown) {
        this.defendingTown = defendingTown;
    }

    public Town getDefendingTown() {
        return defendingTown;
    }

    public void adjustSiegePoints(int adjustment) {
        siegePoints += adjustment;
    }

	public double getWarChestAmount() {
    	return warChestAmount;
	}

	public void setWarChestAmount(double warChestAmount) {
		this.warChestAmount = warChestAmount;
	}

	public List<Resident> getBannerControllingResidents() {
		return new ArrayList<>(bannerControllingResidents);
	}

	public void addBannerControllingResident(Resident resident) {
		bannerControllingResidents.add(resident);
	}

	public void clearBannerControllingResidents() {
		bannerControllingResidents.clear();
	}

	public SiegeSide getBannerControllingSide() {
		return bannerControllingSide;
	}

	public void setBannerControllingSide(SiegeSide bannerControllingSide) {
		this.bannerControllingSide = bannerControllingSide;
	}

	public Map<Player, BannerControlSession> getBannerControlSessions() {
		return new HashMap<>(bannerControlSessions);
	}

	public void removeBannerControlSession(BannerControlSession bannerControlSession) {
		bannerControlSessions.remove(bannerControlSession.getPlayer());
	}

	public void addBannerControlSession(Player player, BannerControlSession bannerControlSession) {
		bannerControlSessions.put(player, bannerControlSession);
	}
}
