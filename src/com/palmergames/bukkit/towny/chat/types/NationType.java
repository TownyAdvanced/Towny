package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyObject;
import net.tnemc.tnc.core.common.chat.ChatType;
import org.bukkit.entity.Player;

import java.util.Collection;
import java.util.HashSet;
import java.util.Optional;
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
		Resident resident = TownyUniverse.getInstance().getDatabaseHandler().getResident(player.getUniqueId());
		
		return Optional.ofNullable(resident)
			.map(Resident::getTown)
			.map(Town::hasNation)
			.orElse(false);
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		final UUID nation = Optional.ofNullable(townyUniverse.getDatabaseHandler().getResident(player.getUniqueId()))
			.map(Resident::getTown)
			.map(Town::getNation)
			.map(TownyObject::getUniqueIdentifier)
			.orElse(null);
       
		if (nation == null) {
			return recipients;
		}

        Collection<Player> newRecipients = new HashSet<>();

        for(Player p : recipients) {
        	
        	Optional<UUID> optionalUUID = Optional.ofNullable(townyUniverse.getDatabaseHandler().getResident(p.getUniqueId()))
				.map(Resident::getTown)
				.map(Town::getNation)
				.map(TownyObject::getUniqueIdentifier);
        	
        	if (!optionalUUID.isPresent()) {
        		return recipients;
			}
            if(optionalUUID.get().equals(nation)) {
                newRecipients.add(p);
            }
        }
        
        return newRecipients;
	}
}