package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyObject;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;

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
			return TownyUniverse.getInstance().getResident(player.getName()).hasTown();
		} catch(NotRegisteredException ignore) {
		}
		return false;
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		try {
			final Town residentTown = townyUniverse.getResident(player.getUniqueId()).getTown();

			Collection<Player> newRecipients = new HashSet<>();

			for(Player p : recipients) {
				if(townyUniverse.getDataSource().getResident(p.getName()).getTown().equals(residentTown)) {
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