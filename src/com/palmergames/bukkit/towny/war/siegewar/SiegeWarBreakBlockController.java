package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * This class intercepts 'break block' events coming from the towny block listener class
 *
 * The class evaluates the event, and determines if it siege related.
 * For example, if somebody tries to destroy a siege attack banner, they will be prevented.
 * 
 * @author Goosius
 */
public class SiegeWarBreakBlockController {

	/**
	 * Evaluates a block break request.
	 * While a siege is in progress, nobody can destroy the siege banner or nearby blocks
	 *
	 * @param player The player breaking the block
	 * @param block The block about to be broken
	 * @param event The event object related to the block break    	
	 * @return true if subsequent perm checks for the event should be skipped
	 */
	public static boolean evaluateSiegeWarBreakBlockRequest(Player player, Block block, BlockBreakEvent event)  {
		if (SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(block)) {
			event.setCancelled(true);
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_cannot_destroy_siege_banner"));
			return true;
		} else {
			return false;
		}
	}

}
