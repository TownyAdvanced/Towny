package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.UUID;

/**
 * @author creatorfromhell
 */
public class TownType extends ChatType {
	public TownType() {
		super("town", "<aqua>$display: <white>$message");
	}

	@Override
	public boolean canChat(Player player) {
		Resident res = TownyUniverse.getInstance().getResident(player.getUniqueId());
		return res != null && res.hasTown();
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		if (resident == null || !resident.hasTown())
			return recipients;
		
		final UUID town = resident.getTownOrNull().getUUID();

		Collection<Player> newRecipients = new HashSet<>();

		for(Player p : recipients) {
			Resident playerResident = townyUniverse.getResident(p.getUniqueId());
			if(playerResident != null && playerResident.hasTown() && playerResident.getTownOrNull().getUUID().equals(town)) {
				newRecipients.add(p);
			}
		}
		return newRecipients;
	}
}