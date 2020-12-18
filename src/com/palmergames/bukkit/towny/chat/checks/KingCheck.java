package com.palmergames.bukkit.towny.chat.checks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
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
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		try {
			if(resident != null && resident.hasNation()) {
				return resident.getTown().getNation().isKing(resident);
			}
		} catch(NotRegisteredException ignore) {
		}
		return false;
	}
}