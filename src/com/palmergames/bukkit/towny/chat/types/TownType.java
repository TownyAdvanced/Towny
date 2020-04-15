package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyObject;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Optional;
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
		Resident resident = TownyUniverse.getInstance().getDatabaseHandler().getResident(player.getUniqueId());
        return Optional.ofNullable(resident)
			.map(Resident::hasTown)
			.orElse(false);
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		Resident resident = townyUniverse.getDatabaseHandler().getResident(player.getUniqueId());
		
		final UUID town = Optional.ofNullable(resident)
			.map(Resident::getTown)
			.map(TownyObject::getUniqueIdentifier)
			.orElse(null);
		
		if (town == null) {
			return recipients;
		}
        
        Collection<Player> newRecipients = new HashSet<>();

        for(Player p : recipients) {
        	Resident playerResident = townyUniverse.getDatabaseHandler().getResident(p.getUniqueId());
        	final UUID ID = Optional.ofNullable(playerResident)
				.map(Resident::getTown)
				.map(TownyObject::getUniqueIdentifier)
				.orElse(null);
        	
        	if (ID == null) {
        		continue;
			}
        	
            if(ID.equals(town)) {
                newRecipients.add(p);
            }
        }
        return newRecipients;
	}
}