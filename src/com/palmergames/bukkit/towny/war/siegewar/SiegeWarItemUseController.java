package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDistanceUtil;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerBucketEmptyEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;

/**
 * This class intercepts 'item use' events coming from the towny player listener class
 *
 * The class evaluates the event, and determines if it is siege related,
 * and processes as appropriate.
 *
 * @author Goosius
 */
public class SiegeWarItemUseController {

	/**
	 * This method looks as a player bucket empty event,
	 * and cancels it if it is not allowed in a siege zone.
	 * 
	 * @param event the bucket emptying event
	 * @return true if subsequent event checks should be skipped
	 */
	public static boolean evaluatePlayerBucketEmptyEvent(PlayerBucketEmptyEvent event)
	{
		try {
			//Check for forbidden bucket emptying activity
			if(TownySettings.isWarSiegeZoneBucketEmptyingRestrictionsEnabled()) {
				for(Material forbiddenMaterial: TownySettings.getWarSiegeZoneBucketEmptyingRestrictionsMaterials()) {
					if(event.getBucket() == forbiddenMaterial) {
						TownBlock townBlock = TownyAPI.getInstance().getTownBlock(event.getBlock().getLocation());
						if(townBlock == null && SiegeWarDistanceUtil.isLocationInActiveSiegeZone(event.getBlock().getLocation()))
						{
							event.setCancelled(true);
							TownyMessaging.sendErrorMsg(event.getPlayer(), TownySettings.getLangString("msg_war_siege_zone_bucket_emptying_forbidden"));
							return true;
						}
						break; //A forbidden material was found, but other conditions were not met.
					}
				}
			}

			//Event not affected
			return false;

		} catch (Exception e) {
			System.out.println("Problem evaluating siege player bucket empty request");
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * This method looks as a player consuming something.
	 * and modifies it based on siege considerations
	 *
	 * @param event the item consume
	 * @return true if subsequent event checks should be skipped
	 */
	public static boolean evaluatePlayerConsumeEvent(PlayerItemConsumeEvent event)
	{
		try {
			//Prevent milk bucket usage while attempting to gain banner control
			if(event.getItem().getType() == Material.MILK_BUCKET) {
				for(Siege siege: TownyUniverse.getInstance().getDataSource().getSieges()) {
					if(siege.getBannerControlSessions().containsKey(event.getPlayer())) {
						event.setCancelled(true);
						TownyMessaging.sendErrorMsg(event.getPlayer(), TownySettings.getLangString("msg_war_siege_zone_milk_bucket_forbidden_while_attempting_banner_control"));
						return true;
					}
				}
			}

			//Event not affected
			return false;

		} catch (Exception e) {
			System.out.println("Problem evaluating siege player consume event");
			e.printStackTrace();
			return false;
		}
	}
}

