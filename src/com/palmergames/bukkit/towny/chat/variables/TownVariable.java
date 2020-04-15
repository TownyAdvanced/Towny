package com.palmergames.bukkit.towny.chat.variables;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyObject;
import net.tnemc.tnc.core.common.chat.ChatVariable;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * @author creatorfromhell
 */
public class TownVariable extends ChatVariable {
	@Override
	public String name() {
		return "$town";
	}

	@Override
	public String parse(Player player, String message) {
		return Optional.ofNullable(TownyUniverse.getInstance().getDatabaseHandler().getResident(player.getUniqueId()))
			.map(Resident::getTown)
			.map(TownyObject::getName)
			.orElse("");
	}
}