package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.TownyUniverse;
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
		try {
			return TownyUniverse.getDataSource().getResident(player.getName()).hasTown();
		} catch(NotRegisteredException ignore) {

		}
		return false;
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		try {
			final UUID town = TownyUniverse.getDataSource().getResident(player.getName()).getTown().getUuid();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				if(TownyUniverse.getDataSource().getResident(p.getName()).getTown().getUuid().equals(town)) {
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