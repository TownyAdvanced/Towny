package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarBlockUtil;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockBreakEvent;

/**
 * @author Goosius
 */
public class SiegeWarBreakBlockController {

	//While a siege exists, nobody can destroy the siege banner or nearby blocks
	//Returns boolean @skipAdditionalPermChecks
	public static boolean evaluateSiegeWarBreakBlockRequest(Player player, Block block, BlockBreakEvent event)  {
		if (SiegeWarBlockUtil.isBlockNearAnActiveSiegeBanner(block)) {
			event.setCancelled(true);
			TownyMessaging.sendErrorMsg(player, "While the siege is in progress you cannot destroy the siege banner or the block it is attached to.");
			return true;
		} else {
			return false;
		}
	}

}
