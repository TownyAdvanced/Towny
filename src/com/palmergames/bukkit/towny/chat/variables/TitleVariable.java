package com.palmergames.bukkit.towny.chat.variables;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import net.tnemc.tnc.core.common.chat.ChatVariable;
import org.bukkit.entity.Player;

import java.util.Optional;

/**
 * @author creatorfromhell
 */
public class TitleVariable extends ChatVariable {
	@Override
	public String name() {
		return "$title";
	}

	@Override
	public String parse(Player player, String message) {
		Resident resident = TownyUniverse.getInstance().getDatabaseHandler().getResident(player.getUniqueId());
        return Optional.ofNullable(resident)
			.map(Resident::getTitle)
			.orElse("");
	}
}