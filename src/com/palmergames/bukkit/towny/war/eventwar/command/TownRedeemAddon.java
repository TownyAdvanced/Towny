package com.palmergames.bukkit.towny.war.eventwar.command;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyCommandAddonAPI.CommandType;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.AddonCommand;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.utils.NameUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarType;
import com.palmergames.bukkit.towny.war.eventwar.db.WarMetaDataController;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.util.StringMgmt;

public class TownRedeemAddon implements TabExecutor {

	public TownRedeemAddon() {
		AddonCommand townRedeemTokenCommand = new AddonCommand(CommandType.TOWN, "redeem", this);
		TownyCommandAddonAPI.addSubCommand(townRedeemTokenCommand);
	}

	private CommandSender sender;

	private static final List<String> townRedeemTabCompletes = Arrays.asList(
			"help",
			"token"
	);

	private static final List<String> townRedeemTokenTabCompletes = Arrays.asList(
			"riot",
			"townwar",
			"civilwar",
			"nationwar",
			"worldwar"
	);
	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		switch (args[0].toLowerCase()) {
			case "token":
				if (args.length == 2)
					return NameUtil.filterByStart(townRedeemTokenTabCompletes, args[1]);
				else
					return Collections.emptyList();

			default:
				if (args.length == 1)
					return NameUtil.filterByStart(townRedeemTabCompletes, args[0]);
		}
		return Collections.emptyList();
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		this.sender = sender;

		if (args.length == 0)
			showHelp();
		else if (args[0].equalsIgnoreCase("help"))
			parseHelpCommand();
		else 
			parseRedeemCommand(StringMgmt.remFirstArg(args), sender);

		return true;
	}
	
	private void parseHelpCommand() {
		// TODO Auto-generated method stub
		// Send something smart to the player explaining how tokens are redeemed.
	}

	private void showHelp() {
		sender.sendMessage(ChatTools.formatTitle("/town redeem"));
		sender.sendMessage(ChatTools.formatCommand("/town redeem", "help", ""));
		sender.sendMessage(ChatTools.formatCommand("/town redeem token", "RIOT", ""));		
		sender.sendMessage(ChatTools.formatCommand("/town redeem token", "TOWNWAR", ""));
		sender.sendMessage(ChatTools.formatCommand("/town redeem token", "CIVILWAR", ""));
		sender.sendMessage(ChatTools.formatCommand("/town redeem token", "NATIONWAR", ""));
		sender.sendMessage(ChatTools.formatCommand("/town redeem token", "WORLDWAR", ""));
	}

	private void parseRedeemCommand(String[] args, CommandSender sender) {
		try {
			Player player = null;
			if (sender instanceof Player)
				player = (Player) sender;
			else 
				throw new TownyException("Not for console!");
			
			Resident res = TownyAPI.getInstance().getResident(player.getUniqueId());
			if (res == null)
				throw new TownyException("Not registered.");
			
			if (!res.hasTown())
				throw new TownyException("You don't have a town.");
			
			if (args.length == 0) {
				showHelp();
				return;
			}
			
			WarType type = null;
			switch (args[0].toLowerCase()) {
				case "riot":
					type = WarType.RIOT;
					break;
				case "townwar":
					type = WarType.TOWNWAR;
					break;
				case "civilwar":
					type = WarType.CIVILWAR;
					break;
				case "nationwar":
					type = WarType.NATIONWAR;
					break;
				case "worldwar":
					type = WarType.WORLDWAR;
					break;
				default:
					throw new TownyException("Invalid war type!");
			}
			Town town = res.getTownOrNull();
			int cost = type.tokenCost;
			Player finalPlayer = player;
			WarType finalType = type;
			if (WarMetaDataController.getWarTokens(town) < cost)
				throw new TownyException("Not enough tokens to purchase that declaration of war! You require " + cost + " tokens.");
				
			Confirmation.runOnAccept(()-> {
				
				if (WarMetaDataController.getWarTokens(town) < cost) {
					TownyMessaging.sendErrorMsg(finalPlayer, "Not enough tokens to purchase that declaration of war! You require " + cost + " tokens.");
					return;
				}
				
				int remainder = WarMetaDataController.getWarTokens(town) - cost;
				WarMetaDataController.setTokens(town, remainder);
				TownyMessaging.sendPrefixedTownMessage(town, town + " has purchased one Declaration of War of type: " + finalType.getName());
			}).sendTo(player);
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(sender, e.getMessage());
		}
	}
}