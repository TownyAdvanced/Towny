package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
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
		try {
			return resident != null && resident.hasTown() && resident.getTown().hasNation();
		} catch(NotRegisteredException ignore) {

		}
		return false;
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident = townyUniverse.getResident(player.getUniqueId());
		
		// Shouldn't happen
		if (resident == null)
			return recipients;
		
		try {
			final UUID nation = resident.getTown().getNation().getUUID();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				Resident playerRes = townyUniverse.getResident(p.getUniqueId());
				if(playerRes != null && playerRes.getTown().getNation().getUUID().equals(nation)) {
					newRecipients.add(p);
				}
			}
			return newRecipients;
		} catch(NotRegisteredException e) {
			e.printStackTrace();
		}
		return recipients;
	}
}