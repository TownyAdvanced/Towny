package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.locations.SiegeZone;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.util.ChatTools;

/**
 * This class is responsible for processing siege attacker wins
 *
 * @author Goosius
 */
public class AttackerWin {

	/**
	 * This method triggers siege values to be updated for an attacker win
	 * 
	 * @param siege the siege
	 * @param winnerNation the winning nation
	 */
	public static void attackerWin(Siege siege, Nation winnerNation) {
        SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege, SiegeStatus.ATTACKER_WIN, winnerNation);

		TownyMessaging.sendGlobalMessage(String.format(
			TownySettings.getLangString("msg_siege_war_attacker_win"),
			TownyFormatter.getFormattedNationName(winnerNation),
			TownyFormatter.getFormattedTownName(siege.getDefendingTown())
		));

		SiegeWarMoneyUtil.giveWarChestsToWinnerNation(siege, winnerNation);
    }
}
