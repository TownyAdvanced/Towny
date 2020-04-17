package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;

/**
 * @author creatorfromhell
 */
public class AllyType extends ChatType {
	public AllyType() {
		super("ally", "<gray>[<aqua>$nation<gray>]: <white>$message");
	}

	@Override
	public boolean canChat(Player player) {
		try {
			Resident r = TownyUniverse.getInstance().getResident(player.getUniqueId());
			return  r.hasTown() && r.getTown().hasNation();
		} catch(NotRegisteredException ignore) {
		}
		return false;
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			final Nation nation = townyUniverse.getResident(player.getName()).getTown().getNation();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				if (!townyUniverse.getResident(p.getUniqueId()).getTown().getNation().equals(nation)
						&& !townyUniverse.getResident(p.getUniqueId()).getTown().getNation().hasAlly(nation)) {
					continue;
				}
				newRecipients.add(p);
			}
			return newRecipients;
		} catch(NotRegisteredException ignore) {
		}
		return recipients;
	}
}