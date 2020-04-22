package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import org.bukkit.entity.Player;

/**
 * This class contains utility functions related to claiming/unclaiming
 *
 * @author Goosius
 */
public class SiegeWarClaimUtil {

	/**
	 * This method verifies if claim is allowed
	 * Accounting for sieges
	 * Throws a towny exception if not allowed
	 *
	 * @param player the player doing the claiming
	 * */
	public static void verifySiegeEffectsOnClaiming(Player player, Town town) throws TownyException{

		//If the claimer's town is under siege, they cannot claim any land
		if (TownySettings.getWarSiegeBesiegedTownClaimingDisabled()
			&& town.hasSiege()
			&& town.getSiege().getStatus() == SiegeStatus.IN_PROGRESS) {
			throw new TownyException(TownySettings.getLangString("msg_err_siege_besieged_town_cannot_claim"));
		}

		//If the land is too near any active siege zone, it cannot be claimed.
		if(TownySettings.getWarSiegeEnabled()
			&& TownySettings.getWarSiegeClaimingDisabledNearSiegeZones())
		{
			boolean nearActiveSiegeZone = false;
			int claimDisableDistance = TownySettings.getWarSiegeClaimDisableDistanceBlocks();
			for(Siege siege: TownyUniverse.getInstance().getDataSource().getSieges()) {
				try {
					if (siege.getStatus() == SiegeStatus.IN_PROGRESS && siege.getFlagLocation().distance(player.getLocation()) < claimDisableDistance) {
						nearActiveSiegeZone = true;
						break;
					}
				} catch (Exception e) {
					//Problem with this particular siegezone. Ignore siegezone
					try {
						System.out.println("Problem with verifying claim against the following siege zone" + siege.getName() + ". Claim allowed.");
					} catch (Exception e2) {
						System.out.println("Problem with verifying claim against a siege zone (name could not be read). Claim allowed");
					}
					e.printStackTrace();
				}
			}

			if(nearActiveSiegeZone) {
				throw new TownyException(TownySettings.getLangString("msg_err_siege_claim_too_near_siege_zone"));
			}
		}
	}
}
