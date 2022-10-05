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
public class NationType extends ChatType {
	public NationType() {
		super("nation", "<gray>[<aqua>$town<gray>]$display: <white>$message");
	}

	@Override
	public boolean canChat(Player player) {
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
		return resident != null && resident.hasNation();
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		// Shouldn't happen
		if (resident == null || !resident.hasNation())
			return recipients;
		
		final UUID nation = resident.getNationOrNull().getUUID();

		Collection<Player> newRecipients = new HashSet<>();

		for(Player p : recipients) {
			Resident playerRes = townyUniverse.getResident(p.getUniqueId());
			if(playerRes != null && playerRes.hasNation() && playerRes.getNationOrNull().getUUID().equals(nation)) {
				newRecipients.add(p);
			}
		}
		return newRecipients;
	}
}