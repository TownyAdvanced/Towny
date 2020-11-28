package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarSettings;
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
	 * @param town the town doing the claiming   
	 * */
	public static void verifySiegeEffectsOnClaiming(Player player, Town town) throws TownyException{

		//If the claimer's town is under siege, they cannot claim any land
		if (SiegeWarSettings.getWarSiegeBesiegedTownClaimingDisabled()
			&& town.hasSiege()
			&& town.getSiege().getStatus().isActive()) {
			throw new TownyException(Translation.of("msg_err_siege_besieged_town_cannot_claim"));
		}

		//If the land is too near any active siege zone, it cannot be claimed.
		if(SiegeWarSettings.getWarSiegeEnabled()
			&& SiegeWarSettings.getWarSiegeClaimingDisabledNearSiegeZones())
		{
			boolean nearActiveSiegeZone = false;
			for(Siege siege: TownyUniverse.getInstance().getDataSource().getSieges()) {
				try {
					if (siege.getStatus().isActive()
						&& SiegeWarDistanceUtil.isInSiegeZone(player, siege)) {
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
				throw new TownyException(Translation.of("msg_err_siege_claim_too_near_siege_zone"));
			}
		}
	}
}
