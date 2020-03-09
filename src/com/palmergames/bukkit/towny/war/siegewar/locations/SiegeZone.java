package com.palmergames.bukkit.towny.war.siegewar.locations;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;

/**
 * This class represents a "Siege Zone".
 *
 * A siege zone is an "attack front" of a siege, a particular zone where one nation is attacking the town.
 * The zone is centred on the "Siege Banner", which was placed when the attack started.
 * 
 * This class keeps track of the 'siege points', which usually determine who wins a siege.
 * 
 * The defending town requires negative siege points in all siegezones to win.  
 * Both defending town residents, and defending nation members can contribute.
 * An attacking nation requires positive siegepoints in their siegezone to win.
 * 
 * For a player to get siegepoints adjusted in their favour,
 * they need to 'occupy' the wilderness area within one town-block-length of the siege banner.
 * This is done simply by remaining on the ground in that area (and not flying or invisible.)
 *
 * @author Goosius
 */
public class SiegeZone {

    private Location siegeBannerLocation;
    private Nation attackingNation;
    private Town defendingTown;
    private int siegePoints;
    private Map<Player, Long> attackerPlayerScoreTimeMap; //player, time when they will score
    private Map<Player, Long> defenderPlayerScoreTimeMap; //player, time when they will score
	private Map<Player, Long> playerAfkTimeMap;  //player, time they will be considered afk
	private double warChestAmount;
	
    public SiegeZone() {
        attackingNation = null;
        defendingTown = null;
        siegePoints = 0;
        siegeBannerLocation = null;
        attackerPlayerScoreTimeMap = new HashMap<>();
        defenderPlayerScoreTimeMap = new HashMap<>();
		playerAfkTimeMap = new HashMap<>();
		warChestAmount = 0;
    }

    public SiegeZone(Nation attackingNation, Town defendingTown) {
        this.defendingTown = defendingTown;
        this.attackingNation = attackingNation;
        siegePoints = 0;
        siegeBannerLocation = null;
        attackerPlayerScoreTimeMap = new HashMap<>();
        defenderPlayerScoreTimeMap = new HashMap<>();
		playerAfkTimeMap = new HashMap<>();
		warChestAmount = 0;
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

    public Map<Player, Long> getAttackerPlayerScoreTimeMap() {
        return attackerPlayerScoreTimeMap;
    }

    public Map<Player, Long> getDefenderPlayerScoreTimeMap() {
        return defenderPlayerScoreTimeMap;
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

	public Map<Player, Long> getPlayerAfkTimeMap() {
		return playerAfkTimeMap;
	}

	public void setPlayerAfkTimeMap(Map<Player, Long> playerAfkTimeMap) {
		this.playerAfkTimeMap = playerAfkTimeMap;
	}

	public double getWarChestAmount() {
    	return warChestAmount;
	}

	public void setWarChestAmount(double warChestAmount) {
		this.warChestAmount = warChestAmount;
	}
}
