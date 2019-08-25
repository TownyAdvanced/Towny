package com.palmergames.bukkit.towny.command;

import java.util.LinkedList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.command.defaults.BukkitCommand;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;


public abstract class BaseCommand extends BukkitCommand implements TabCompleter {

	protected BaseCommand(String label, String description, String usage, List<String> aliases) {
		super(label);
		this.description = description;
		this.usageMessage = usage;
		this.setAliases(aliases);
	}

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		LinkedList<String> output = new LinkedList<>();
		String lastArg = "";
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		// Get the last argument
		if (args.length > 0) {
			lastArg = args[args.length - 1].toLowerCase();
		}

		if (!lastArg.equalsIgnoreCase("")) {
			// Match nations
			for (Nation nation : townyUniverse.getDataSource().getNations()) {
				if (nation.getName().toLowerCase().startsWith(lastArg)) {
					output.add(nation.getName());
				}

			}
			// Match towns
			for (Town town : townyUniverse.getDataSource().getTowns()) {
				if (town.getName().toLowerCase().startsWith(lastArg)) {
					output.add(town.getName());
				}

			}
			// Match residents
			for (Resident resident : townyUniverse.getDataSource().getResidents()) {
				if (resident.getName().toLowerCase().startsWith(lastArg)) {
					output.add(resident.getName());
				}
			
			}

		}
		return output;
	}

}
