package com.palmergames.bukkit.towny.command;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownyUniverse;


public class BaseCommand implements TabCompleter{

	
	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {

		LinkedList<String> output = new LinkedList<String>();
		String lastArg = "";

		// Get the last argument
		if (args.length > 0) {
			lastArg = args[args.length - 1].toLowerCase();
		}

		if (!lastArg.equalsIgnoreCase("")) {

			// Match nations
			for (Nation nation : TownyUniverse.getDataSource().getNations()) {
				if (nation.getName().toLowerCase().startsWith(lastArg)) {
					output.add(nation.getName());
				}

			}

			// Match towns
			for (Town town : TownyUniverse.getDataSource().getTowns()) {
				if (town.getName().toLowerCase().startsWith(lastArg)) {
					output.add(town.getName());
				}

			}

			// Match residents
			for (Resident resident : TownyUniverse.getDataSource().getResidents()) {
				if (resident.getName().toLowerCase().startsWith(lastArg)) {
					output.add(resident.getName());
				}
			
			}

		}

		return output;
	}
}
