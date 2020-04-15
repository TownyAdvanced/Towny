package com.palmergames.bukkit.towny.chat.checks;

import com.palmergames.bukkit.towny.TownyUniverse;
import net.tnemc.tnc.core.common.chat.ChatCheck;
import org.bukkit.entity.Player;

/**
 * @author creatorfromhell
 */
public class MayorCheck extends ChatCheck {
	@Override
	public String name() {
		return "ismayor";
	}

	@Override
	public boolean runCheck(Player player, String checkString) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
        if(townyUniverse.getDataSource().getResident(player.getName()).hasTown()) {
            return townyUniverse.getDataSource().getResident(player.getName()).getTown().isMayor(townyUniverse.getDataSource().getResident(player.getName()));
        }
        return false;
	}
}