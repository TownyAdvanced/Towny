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
public class NationType extends ChatType {
	public NationType() {
		super("nation", "<gray>[<aqua>$town<gray>]$display: <white>$message");
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
			final UUID nation = TownyUniverse.getDataSource().getResident(player.getName()).getTown().getNation().getUuid();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				if(TownyUniverse.getDataSource().getResident(p.getName()).getTown().getNation().getUuid().equals(nation)) {
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