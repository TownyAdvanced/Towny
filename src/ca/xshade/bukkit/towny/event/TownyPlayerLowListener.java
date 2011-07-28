package ca.xshade.bukkit.towny.event;

import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerChatEvent;
import org.bukkit.event.player.PlayerListener;

import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;


/**
 * Handle events for all Player related events
 * 
 * @author Shade
 * 
 */
public class TownyPlayerLowListener extends PlayerListener {
	private final Towny plugin;

	public TownyPlayerLowListener(Towny instance) {
		this.plugin = instance;
	}

	@Override
	public void onPlayerChat(PlayerChatEvent event) {
		if (event.isCancelled())
			return;
		
		Player player = event.getPlayer();
		
		// Setup the chat prefix BEFORE we speak.
		if (TownySettings.isUsingChatPrefix()) {
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				String colour, formatedName = "";
				if (resident.isKing())
					colour = Colors.Gold;
				else if (resident.isMayor())
					colour = Colors.LightBlue;
				else
					colour = "";
				formatedName = colour + plugin.getPermissionNode(resident, "prefix") + plugin.getTownyUniverse().getFormatter().getNamePrefix(resident)
					+ player.getName() + plugin.getTownyUniverse().getFormatter().getNamePostfix(resident) + plugin.getPermissionNode(resident, "suffix")
					+ Colors.White;
				player.setDisplayName(formatedName);
			} catch (NotRegisteredException e) {
				plugin.log("Not Registered");
			}
		}
		
		if (plugin.hasPlayerMode(player, "tc"))
			parseTownChatCommand(player, event.getMessage());
		else if (plugin.hasPlayerMode(player, "nc")) 
			parseNationChatCommand(player, event.getMessage());
		else {
			// All chat modes are disabled, or this is open chat.
			return;
		}
		event.setCancelled(true);
	}
	
	//TODO: Below is copy pasta :/

	public void parseTownChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			String line = Colors.Blue + "[" + town.getName() + "] "
					+ player.getDisplayName() + ": "
					+ Colors.LightBlue + msg;
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	public void parseNationChatCommand(Player player, String msg) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Nation nation = resident.getTown().getNation();
			String line = Colors.Gold + "[" + nation.getName() + "] "
					+ player.getDisplayName() + ": "
					+ Colors.Yellow + msg;
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(line));
		} catch (NotRegisteredException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
}