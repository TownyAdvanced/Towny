package com.palmergames.bukkit.towny.war.siegewar.timeractions;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.war.siegewar.SiegeWarUtil;
import com.palmergames.bukkit.towny.war.siegewar.locations.Siege;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeStatus;
import com.palmergames.bukkit.util.ChatTools;

/**
 * @author Goosius
 */
public class DefenderWin
{
    public static void defenderWin(Siege siege, Town winnerTown) {
        SiegeWarUtil.updateAndSaveSiegeCompletionValues(siege, SiegeStatus.DEFENDER_WIN, null);

        TownyMessaging.sendGlobalMessage(ChatTools.color(String.format(
                TownySettings.getLangString("msg_siege_war_defender_win"),
                TownyFormatter.getFormattedTownName(winnerTown)
        )));
    }

}
