package com.palmergames.bukkit.towny.war.siegewar.playeractions;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarMoneyUtil;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarSiegeCompletionUtil;
import com.palmergames.util.TimeMgmt;

/**
 * This class is responsible for processing requests to Abandon siege attacks
 *
 * @author Goosius
 */
public class AbandonAttack {

    public static void attackerAbandon(Siege siege) {
		long timeUntilOfficialAbandon = siege.getTimeUntilAbandonConfirmationMillis();

		if(timeUntilOfficialAbandon > 0) {
			//Pending abandon
			siege.setStatus(SiegeStatus.PENDING_ATTACKER_ABANDON);
			TownyUniverse.getInstance().getDataSource().saveSiege(siege);
			TownyMessaging.sendGlobalMessage(
				String.format(Translation.of("msg_siege_war_pending_attacker_abandon"),
					siege.getAttackingNation().getFormattedName(),
					siege.getDefendingTown().getFormattedName(),
					TimeMgmt.getFormattedTimeValue(timeUntilOfficialAbandon)));
		} else {
			//Immediate abandon
			SiegeWarMoneyUtil.giveWarChestToDefendingTown(siege);
			SiegeWarSiegeCompletionUtil.updateSiegeValuesToComplete(siege, SiegeStatus.ATTACKER_ABANDON);
			TownyMessaging.sendGlobalMessage(
				String.format(Translation.of("msg_siege_war_attacker_abandon"),
					siege.getAttackingNation().getFormattedName(),
					siege.getDefendingTown().getFormattedName()));
		}
	}
}
