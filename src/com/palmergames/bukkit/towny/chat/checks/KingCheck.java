package com.palmergames.bukkit.towny.chat.checks;

import com.palmergames.bukkit.towny.TownyUniverse;
import net.tnemc.tnc.core.common.chat.ChatCheck;
import org.bukkit.entity.Player;

/**
 * @author creatorfromhell
 */
public class KingCheck extends ChatCheck {
	@Override
	public String name() {
		return "isking";
	}

	@Override
	public boolean runCheck(Player player, String checkString) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
        if(townyUniverse.getDataSource().getResident(player.getName()).hasNation()) {
            return townyUniverse.getDataSource().getResident(player.getName()).getTown().getNation().isKing(townyUniverse.getDataSource().getResident(player.getName()));
        }
        return false;
	}
}