package com.palmergames.bukkit.towny.chat.types;

import com.palmergames.bukkit.towny.TownyUniverse;
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
		Resident resident = TownyUniverse.getInstance().getDatabaseHandler().getResident(player.getUniqueId());
		
		return Optional.ofNullable(resident)
			.map(Resident::getTown)
			.map(Town::hasNation)
			.orElse(false);
	}

	@Override
	public Collection<Player> getRecipients(Collection<Player> recipients, Player player) {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		Resident resident = townyUniverse.getDatabaseHandler().getResident(player.getUniqueId());
		final Optional<Nation> optionalNation = Optional.ofNullable(resident)
			.map(Resident::getTown)
			.map(Town::getNation);
		
		if (!optionalNation.isPresent()) {
			return recipients;
		}
		
        final Nation nation = optionalNation.get();

        Collection<Player> newRecipients = new HashSet<>();

        for(Player p : recipients) {
        	
        	Resident playerResident = townyUniverse.getDatabaseHandler().getResident(p.getUniqueId());
        	final Optional<Nation> optionalPlayerNation = Optional.ofNullable(playerResident)
				.map(Resident::getTown)
				.map(Town::getNation);
        	
        	if (!optionalPlayerNation.isPresent()) {
        		continue;
			}
        	
            if (!optionalPlayerNation.get().getUniqueIdentifier().equals(nation.getUniqueIdentifier())
                    && !optionalPlayerNation.get().hasAlly(nation)) {
                continue;
            }
            newRecipients.add(p);
        }
        return newRecipients;
	}
}