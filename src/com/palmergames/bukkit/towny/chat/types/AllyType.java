package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;

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
			return TownyUniverse.getDataSource().getResident(player.getName()).hasTown() && TownyUniverse.getDataSource().getResident(player.getName()).getTown().hasNation();
		} catch(NotRegisteredException ignore) {

		}
		return false;
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		try {
			final Nation nation = TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				if(TownyUniverse.getDataSource().getResident(p.getName()).getTown().getNation().getUuid().equals(nation.getUuid())
						|| TownyUniverse.getDataSource().getResident(p.getName()).getTown().getNation().hasAlly(nation)) {
					newRecipients.add(p);
				}
			}
			return newRecipients;
		} catch(NotRegisteredException ignore) {
		}
		return recipients;
	}
}