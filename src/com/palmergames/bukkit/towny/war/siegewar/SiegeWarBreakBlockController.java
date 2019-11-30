package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
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
			System.out.println("B");
			event.setCancelled(true);
			TownyMessaging.sendErrorMsg(player, TownySettings.getLangString("msg_err_siege_war_cannot_destroy_siege_banner"));
			return true;
		} else {
			return false;
		}
	}

}
