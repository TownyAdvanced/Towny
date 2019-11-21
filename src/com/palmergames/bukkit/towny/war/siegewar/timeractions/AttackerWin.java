package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.war.siegewar.utils.SiegeWarDbUtil;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.util.ChatTools;

/**
 * @author Goosius
 */
public class AttackerWin {

    public static void attackerWin(Siege siege, Nation winnerNation) {
        SiegeWarDbUtil.updateAndSaveSiegeCompletionValues(siege, SiegeStatus.ATTACKER_WIN, winnerNation);

        TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                TownySettings.getLangString("msg_siege_war_attacker_win"),
                TownyFormatter.getFormattedNationName(winnerNation),
                TownyFormatter.getFormattedTownName(siege.getDefendingTown()))
        ));
    }
}
