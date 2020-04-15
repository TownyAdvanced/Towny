package com.palmergames.bukkit.towny.chat.variables;

import com.palmergames.bukkit.towny.TownyUniverse;
import net.tnemc.tnc.core.common.chat.ChatVariable;
import org.bukkit.entity.Player;

/**
 * @author creatorfromhell
 */
public class NationVariable extends ChatVariable {
	@Override
	public String name() {
		return "$nation";
	}

	@Override
	public String parse(Player player, String message) {
        return TownyUniverse.getInstance().getDataSource().getResident(player.getName()).getTown().getNation().getName();
    }
}