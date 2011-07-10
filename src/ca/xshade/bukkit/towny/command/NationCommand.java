package ca.xshade.bukkit.towny.command;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import com.iConomy.iConomy;

import ca.xshade.bukkit.questioner.Questioner;
import ca.xshade.bukkit.towny.AlreadyRegisteredException;
import ca.xshade.bukkit.towny.EmptyNationException;
import ca.xshade.bukkit.towny.IConomyException;
import ca.xshade.bukkit.towny.NotRegisteredException;
import ca.xshade.bukkit.towny.Towny;
import ca.xshade.bukkit.towny.TownyException;
import ca.xshade.bukkit.towny.TownySettings;
import ca.xshade.bukkit.towny.object.Nation;
import ca.xshade.bukkit.towny.object.Resident;
import ca.xshade.bukkit.towny.object.Town;
import ca.xshade.bukkit.towny.object.TownyIConomyObject;
import ca.xshade.bukkit.towny.object.TownyUniverse;
import ca.xshade.bukkit.towny.questioner.JoinNationTask;
import ca.xshade.bukkit.towny.questioner.ResidentNationQuestionTask;
import ca.xshade.bukkit.util.ChatTools;
import ca.xshade.bukkit.util.Colors;
import ca.xshade.questionmanager.Option;
import ca.xshade.questionmanager.Question;
import ca.xshade.util.StringMgmt;

/**
 * Send a list of all nation commands to player Command: /nation ?
 * 
 * @param player
 * 
 * handles all nation based commands
 */

public class NationCommand implements CommandExecutor  {
	
	private static Towny plugin;
	private static final List<String> nation_help = new ArrayList<String>();
	private static final List<String> king_help = new ArrayList<String>();
	
	static {
		
		nation_help.add(ChatTools.formatTitle("/nation"));
		nation_help.add(ChatTools.formatCommand("", "/nation", "", TownySettings.getLangString("nation_help_1")));
		nation_help.add(ChatTools.formatCommand("", "/nation", TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("nation_help_3")));
		nation_help.add(ChatTools.formatCommand("", "/nation", "list", TownySettings.getLangString("nation_help_4")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("res_sing"), "/nation", "deposit [$]", ""));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/nation", "leave", TownySettings.getLangString("nation_help_5")));
		if (!TownySettings.isNationCreationAdminOnly())
			nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("mayor_sing"), "/nation", "new " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("nation_help_6")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "king ?", TownySettings.getLangString("nation_help_7")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/nation", "new " + TownySettings.getLangString("nation_help_2") + " [capital]", TownySettings.getLangString("nation_help_8")));
		nation_help.add(ChatTools.formatCommand(TownySettings.getLangString("admin_sing"), "/nation", "delete " + TownySettings.getLangString("nation_help_2"), ""));
	
		king_help.add(ChatTools.formatTitle(TownySettings.getLangString("king_help_1")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "withdraw [$]", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "[add/kick] [town] .. [town]", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "assistant [add/remove] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_6")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "assistant [add+/remove+] " + TownySettings.getLangString("res_2"), TownySettings.getLangString("res_7")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "set [] .. []", ""));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "ally [add/remove] " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("king_help_2")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "enemy [add/remove] " + TownySettings.getLangString("nation_help_2"), TownySettings.getLangString("king_help_3")));
		king_help.add(ChatTools.formatCommand(TownySettings.getLangString("king_sing"), "/nation", "delete", ""));
	
	}
	
	public NationCommand(Towny instance) {
		plugin = instance;
	}		

	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			
			if (args == null){
				for (String line : nation_help)
					player.sendMessage(line);
				parseNationCommand(player, args);
			} else{
				parseNationCommand(player, args);
			}

		} else
			// Console
			for (String line : nation_help)
				sender.sendMessage(Colors.strip(line));
		return true;
	}
	
	public void parseNationCommand(Player player, String[] split) {
		String nationCom = TownySettings.getFirstCommand(TownySettings.getNationCommands());
		
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				Nation nation = town.getNation();
				plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
			} catch (NotRegisteredException x) {
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_err_dont_belong_nation"));
			}
		else if (split[0].equalsIgnoreCase("?"))
			for (String line : nation_help)
				player.sendMessage(line);
		else if (split[0].equalsIgnoreCase("list"))
			listNations(player);
		else if (split[0].equalsIgnoreCase("new")) {
			// TODO: Make an overloaded function
			// newNation(Player,String,Town)
			if (split.length == 1)
				plugin.sendErrorMsg(player, TownySettings.getLangString("msg_specify_nation_name"));
			else if (split.length == 2)
				try { // TODO: Make sure of the error catching
					Resident resident = plugin.getTownyUniverse().getResident(player.getName());
					if (!resident.isMayor() && !resident.getTown().hasAssistant(resident))
						throw new TownyException(TownySettings.getLangString("msg_peasant_right"));
					newNation(player, split[1], resident.getTown().getName());
				} catch (TownyException x) {
					plugin.sendErrorMsg(player, x.getError());
				}
			else
				// TODO: Check if player is an admin
				newNation(player, split[1], split[2]);
		} else if (split[0].equalsIgnoreCase("leave"))
			nationLeave(player);
		else if (split[0].equalsIgnoreCase("withdraw")) {
			if (split.length == 2)
				try {
					nationWithdraw(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
				}
			else
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), nationCom));
		} else if (split[0].equalsIgnoreCase("deposit")) {
			if (split.length == 2)
				try {
					nationDeposit(player, Integer.parseInt(split[1]));
				} catch (NumberFormatException e) {
					plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
				}
			else
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_must_specify_amnt"), nationCom));
		} else {
			String[] newSplit = StringMgmt.remFirstArg(split);
			
			if (split[0].equalsIgnoreCase("king"))
				nationKing(player, newSplit);
			else if (split[0].equalsIgnoreCase("add"))
				nationAdd(player, newSplit);
			else if (split[0].equalsIgnoreCase("kick"))
				nationKick(player, newSplit);
			else if (split[0].equalsIgnoreCase("assistant"))
				nationAssistant(player, newSplit);
			else if (split[0].equalsIgnoreCase("set"))
				nationSet(player, newSplit);
			else if (split[0].equalsIgnoreCase("ally"))
				nationAlly(player, newSplit);
			else if (split[0].equalsIgnoreCase("enemy"))
				nationEnemy(player, newSplit);
			else if (split[0].equalsIgnoreCase("delete"))
				nationDelete(player, newSplit);
			else 
				try {
					Nation nation = plugin.getTownyUniverse().getNation(split[0]);
					plugin.getTownyUniverse().sendMessage(player, plugin.getTownyUniverse().getStatus(nation));
				} catch (NotRegisteredException x) {
					plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_not_registered_1"), split[0]));
				}
		}
	}
		
	private void nationWithdraw(Player player, int amount) {
		Resident resident;
		Nation nation;
		try {
			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money")); //TODO
			
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			
			nation.withdrawFromBank(resident, amount);
			plugin.getTownyUniverse().sendNationMessage(nation, String.format(TownySettings.getLangString("msg_xx_withdrew_xx"), resident.getName(), amount, "nation"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	private void nationDeposit(Player player, int amount) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			
			if (amount < 0)
				throw new TownyException(TownySettings.getLangString("msg_err_negative_money"));
			
			if (!resident.pay(amount, nation))
				throw new TownyException(TownySettings.getLangString("msg_insuf_funds"));
			
			plugin.getTownyUniverse().sendNationMessage(nation, String.format(TownySettings.getLangString("msg_xx_deposited_xx"), resident.getName(), amount, "nation"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}

	/**
	 * Send a list of all nations in the universe to player Command: /nation list
	 * 
	 * @param player
	 */

	public void listNations(Player player) {
		player.sendMessage(ChatTools.formatTitle(TownySettings.getLangString("nation_plu")));
		ArrayList<String> formatedList = new ArrayList<String>();
		for (Nation nation : plugin.getTownyUniverse().getNations())
			formatedList.add(Colors.LightBlue + nation.getName() + Colors.Blue + " [" + nation.getNumTowns() + "]" + Colors.White);
		for (String line : ChatTools.list(formatedList))
			player.sendMessage(line);
	}

	/**
	 * Create a new nation. Command: /nation new [nation] *[capital]
	 * 
	 * @param player
	 */

	public void newNation(Player player, String name, String capitalName) {
		TownyUniverse universe = plugin.getTownyUniverse();
		try {
			if (TownySettings.isNationCreationAdminOnly() && !plugin.isTownyAdmin(player) && !plugin.hasPermission(player, "towny.nation.new"))
				throw new TownyException(TownySettings.getNotPermToNewNationLine());
			
			Town town = universe.getTown(capitalName);
			if (town.hasNation())
				throw new TownyException(TownySettings.getLangString("msg_err_already_nation"));
			
			if (!TownySettings.isValidName(name))
				throw new TownyException(String.format(TownySettings.getLangString("msg_err_invalid_name"), name));
			
			if (TownySettings.isUsingIConomy() && !town.pay(TownySettings.getNewNationPrice()))
				throw new TownyException(TownySettings.getLangString("msg_no_funds_new_nation"));

			newNation(universe, name, town);
			/*universe.newNation(name);
			Nation nation = universe.getNation(name);
			nation.addTown(town);
			nation.setCapital(town);

			universe.getDataSource().saveTown(town);
			universe.getDataSource().saveNation(nation);
			universe.getDataSource().saveNationList();*/

			universe.sendGlobalMessage(TownySettings.getNewNationMsg(player.getName(), name));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			// TODO: delete town data that might have been done
		} catch (IConomyException x) {
			plugin.sendErrorMsg(player, x.getError());
		}
	}
	
	public Nation newNation(TownyUniverse universe, String name, Town town) throws AlreadyRegisteredException, NotRegisteredException {
		universe.newNation(name);
		Nation nation = universe.getNation(name);
		nation.addTown(town);
		nation.setCapital(town);
		if(TownySettings.isUsingIConomy())
		{
			iConomy.getAccount("nation-"+name);
			iConomy.getAccount("nation-"+name).getHoldings().set(0);
		}
		universe.getDataSource().saveTown(town);
		universe.getDataSource().saveNation(nation);
		universe.getDataSource().saveNationList();
		
		return nation;
	}
	
	public void nationLeave(Player player) {
		try {
			Resident resident = plugin.getTownyUniverse().getResident(player.getName());
			Town town = resident.getTown();
			Nation nation = town.getNation();
			if (!resident.isMayor())
				if (!town.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_mayor_ass"));
			nation.removeTown(town);
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(String.format(TownySettings.getLangString("msg_nation_town_left"), town.getName())));
			plugin.getTownyUniverse().sendTownMessage(town, ChatTools.color(String.format(TownySettings.getLangString("msg_town_left_nation"), nation.getName())));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		} catch (EmptyNationException en) {
			plugin.getTownyUniverse().removeNation(en.getNation());
			plugin.getTownyUniverse().sendGlobalMessage(ChatTools.color(String.format(TownySettings.getLangString("msg_del_nation"), en.getNation().getName())));
		}
	}
	
	public void nationDelete(Player player, String[] split) {
		if (split.length == 0)
			try {
				Resident resident = plugin.getTownyUniverse().getResident(player.getName());
				Town town = resident.getTown();
				Nation nation = town.getNation();
				if (!resident.isKing())
					throw new TownyException(TownySettings.getLangString("msg_not_king"));
				plugin.getTownyUniverse().removeNation(nation);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelNationMsg(nation));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
		else
			try {
				if (!plugin.isTownyAdmin(player))
					throw new TownyException(TownySettings.getLangString("msg_err_admin_only_delete_nation"));
				Nation nation = plugin.getTownyUniverse().getNation(split[0]);
				plugin.getTownyUniverse().removeNation(nation);
				plugin.getTownyUniverse().sendGlobalMessage(TownySettings.getDelNationMsg(nation));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}
	}
	
	public void nationKing(Player player, String[] split) {
		if (split.length == 0 || split[0].equalsIgnoreCase("?"))
			for (String line : king_help)
				player.sendMessage(line);
	}
	
	public void nationAdd(Player player, String[] names) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				if (!nation.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_king_ass"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		nationAdd(player, nation, plugin.getTownyUniverse().getTowns(names));
		
	}

	public void nationAdd(Player player, Nation nation, List<Town> invited) {
		ArrayList<Town> remove = new ArrayList<Town>();
		for (Town town : invited)
			try {
				//nation.addTown(town);
				
				nationInviteTown(player, nation, town);
			} catch (AlreadyRegisteredException e) {
				remove.add(town);
			}
		for (Town town : remove)
			invited.remove(town);

		if (invited.size() > 0) {
			String msg = "";

			for (Town town : invited)
				msg += town.getName() + ", ";
					
			msg = msg.substring(0, msg.length()-2);
			msg = String.format(TownySettings.getLangString("msg_invited_join_nation"), player.getName(), msg);
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}
	
	private static void nationInviteTown(Player player, Nation nation, Town town) throws AlreadyRegisteredException {
		Plugin test = plugin.getServer().getPluginManager().getPlugin("Questioner");
		
		Resident townMayor = town.getMayor();
		
		if (TownySettings.isUsingQuestioner() && test != null && test instanceof Questioner && test.isEnabled()) {
			Questioner questioner = (Questioner)test;
			questioner.loadClasses();
			
			List<Option> options = new ArrayList<Option>();
			options.add(new Option("accept", new JoinNationTask(townMayor, nation)));
			options.add(new Option("deny", new ResidentNationQuestionTask(townMayor, nation) {
				@Override
				public void run() {
					getUniverse().sendNationMessage(nation, String.format(TownySettings.getLangString("msg_deny_invite"), getResident().getName()));
				}
			}));
			Question question = new Question(townMayor.getName(), String.format(TownySettings.getLangString("msg_invited"), nation.getName()), options);
			try {
				plugin.appendQuestion(questioner, question);
			} catch (Exception e) {
				System.out.println(e.getMessage());
			}
		} else
			
				nation.addTown(town);
				plugin.updateCache();
				plugin.getTownyUniverse().getDataSource().saveTown(town);
	}
	
	public void nationKick(Player player, String[] names) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				if (!nation.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_king_ass"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		nationKick(player, resident, nation, plugin.getTownyUniverse().getTowns(names));
	}

	public void nationKick(Player player, Resident resident, Nation nation, List<Town> kicking) {
		ArrayList<Town> remove = new ArrayList<Town>();
		for (Town town : kicking)
			if (town.isCapital())
				remove.add(town);
			else
				try {
					nation.removeTown(town);
					plugin.updateCache();
					plugin.getTownyUniverse().getDataSource().saveTown(town);
				} catch (NotRegisteredException e) {
					remove.add(town);
				} catch (EmptyNationException e) {
					// You can't kick yourself and only the mayor can kick assistants
					// so there will always be at least one resident.
				}
		for (Town town : remove)
			kicking.remove(town);

		if (kicking.size() > 0) {
			String msg = "";
			
			for (Town town : kicking) {
				msg += town.getName() + ", ";
				
				msg = msg.substring(0, msg.length()-2);
				plugin.getTownyUniverse().sendTownMessage(town, String.format(TownySettings.getLangString("msg_nation_kicked_by"), player.getName()));
			}
			msg = msg.substring(0, msg.length()-2);
			msg = String.format(TownySettings.getLangString("msg_nation_kicked"), player.getName(), msg);
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}
	
	public void nationAssistant(Player player, String[] split) {
		if (split.length == 0) {
			//TODO: assistant help
		} else if (split[0].equalsIgnoreCase("add")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsAdd(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("remove")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsRemove(player, newSplit, true);
		} else if (split[0].equalsIgnoreCase("add+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsAdd(player, newSplit, false);
		} else if (split[0].equalsIgnoreCase("remove+")) {
			String[] newSplit = StringMgmt.remFirstArg(split);
			nationAssistantsRemove(player, newSplit, false);
		}
	}
	
	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and invite them to town. Command: /town add
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void nationAssistantsAdd(Player player, String[] names, boolean matchOnline) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				throw new TownyException(TownySettings.getLangString("msg_not_king"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		nationAssistantsAdd(player, nation, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
	}

	public void nationAssistantsAdd(Player player, Nation nation, List<Resident> invited) {
		//TODO: change variable names from townAdd copypasta
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident newMember : invited)
			try {
				nation.addAssistant(newMember);
				plugin.deleteCache(newMember.getName());
				plugin.getTownyUniverse().getDataSource().saveResident(newMember);
			} catch (AlreadyRegisteredException e) {
				remove.add(newMember);
			}
		for (Resident newMember : remove)
			invited.remove(newMember);

		if (invited.size() > 0) {
			String msg = "";
			
			for (Resident newMember : invited)
				msg += newMember.getName() + ", ";
					
			msg = msg.substring(0, msg.length()-2);
			msg = String.format(TownySettings.getLangString("msg_raised_ass"), player.getName(), msg, "nation");
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}

	/**
	 * Confirm player is a mayor or assistant, then get list of filter names
	 * with online players and kick them from town. Command: /town kick
	 * [resident] .. [resident]
	 * 
	 * @param player
	 * @param names
	 */

	public void nationAssistantsRemove(Player player, String[] names, boolean matchOnline) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				throw new TownyException(TownySettings.getLangString("msg_not_king"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}

		nationAssistantsRemove(player, resident, nation, (matchOnline ? plugin.getTownyUniverse().getOnlineResidents(player, names) : getResidents(player, names)));
	}

	public void nationAssistantsRemove(Player player, Resident resident, Nation nation, List<Resident> kicking) {
		ArrayList<Resident> remove = new ArrayList<Resident>();
		for (Resident member : kicking)
			try {
				nation.removeAssistant(member);
				plugin.deleteCache(member.getName());
				plugin.getTownyUniverse().getDataSource().saveResident(member);
			} catch (NotRegisteredException e) {
				remove.add(member);
			}
		for (Resident member : remove)
			kicking.remove(member);

		if (kicking.size() > 0) {
			String msg = "";
			
			for (Resident member : kicking) {
				msg += member.getName() + ", ";
				/* removed to prevent multiple message spam.
				 * 
				Player p = plugin.getServer().getPlayer(member.getName());
				if (p != null)
					p.sendMessage(String.format(TownySettings.getLangString("msg_lowered_to_res_by"), player.getName()));
				*/
			}
			msg = msg.substring(0, msg.length()-2);
			msg = String.format(TownySettings.getLangString("msg_lowered_to_res"), player.getName(), msg);
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
	}
	
	public void nationAlly(Player player, String[] split) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				if (!nation.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_king_ass"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		ArrayList<Nation> list = new ArrayList<Nation>();
		Nation ally;
		//test add or remove
		String test = split[0];
		String[] newSplit = StringMgmt.remFirstArg(split);
		
		if ((test.equalsIgnoreCase("remove") || test.equalsIgnoreCase("add")) && newSplit.length > 0) {
			for (String name : newSplit) {
				try {
					ally = plugin.getTownyUniverse().getNation(name);
					list.add(ally);
				} catch (NotRegisteredException e) {
					// Do nothing here as the name doesn't match a Nation
				}						
			}
			nationAlly(player, nation, list, test.equalsIgnoreCase("add"));

		} else {
			plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "[add/remove]"));
		}
		
	}
	
	public void nationAlly(Player player, Nation nation, List<Nation> allies, boolean add) {
		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation targetNation : allies)
			try {
				if (add && !nation.getAllies().contains(targetNation)) {
					nation.addAlly(targetNation);
					plugin.getTownyUniverse().sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_ally"), nation.getName()));
				}
				else if (nation.getAllies().contains(targetNation)) {
					nation.removeAlly(targetNation);
					plugin.getTownyUniverse().sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_removed_ally"), nation.getName()));
				}
				
				plugin.updateCache();
			} catch (AlreadyRegisteredException e) {
				remove.add(targetNation);
			} catch (NotRegisteredException e) {
				remove.add(targetNation);
			}
			
		for (Nation newAlly : remove)
			allies.remove(newAlly);

		if (allies.size() > 0) {
			String msg = "";
			
			for (Nation newAlly : allies)
				msg += newAlly.getName() + ", ";
			
			msg = msg.substring(0, msg.length()-2);
			if (add)
				msg = String.format(TownySettings.getLangString("msg_allied_nations"), player.getName(), msg);
			else
				msg = String.format(TownySettings.getLangString("msg_broke_alliance"), player.getName(), msg);
			
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNations();
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));
		
	}
	
	public void nationEnemy(Player player, String[] split) {
		Resident resident;
		Nation nation;
		try {
			resident = plugin.getTownyUniverse().getResident(player.getName());
			nation = resident.getTown().getNation();
			if (!resident.isKing())
				if (!nation.hasAssistant(resident))
					throw new TownyException(TownySettings.getLangString("msg_not_king_ass"));
		} catch (TownyException x) {
			plugin.sendErrorMsg(player, x.getError());
			return;
		}
		
		ArrayList<Nation> list = new ArrayList<Nation>();
		Nation enemy;
		//test add or remove
		String test = split[0];
		String[] newSplit = StringMgmt.remFirstArg(split);
		
		if ((test.equalsIgnoreCase("remove") || test.equalsIgnoreCase("add")) && newSplit.length > 0) {
			for (String name : newSplit) {
				try {
					enemy = plugin.getTownyUniverse().getNation(name);
					list.add(enemy);
				} catch (NotRegisteredException e) {
					// Do nothing here as the name doesn't match a Nation
				}						
			}
			nationEnemy(player, nation, list, test.equalsIgnoreCase("add"));

		} else {
			plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "[add/remove]"));
		}
		
	}
	
	public void nationEnemy(Player player, Nation nation, List<Nation> enemies, boolean add) {
		ArrayList<Nation> remove = new ArrayList<Nation>();
		for (Nation targetNation : enemies)
			try {
				if (add && !nation.getEnemies().contains(targetNation)) {
					nation.addEnemy(targetNation);
					plugin.getTownyUniverse().sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_added_enemy"), nation.getName()));
				}
				else if (nation.getEnemies().contains(targetNation)) {
					nation.removeEnemy(targetNation);
					plugin.getTownyUniverse().sendNationMessage(targetNation, String.format(TownySettings.getLangString("msg_removed_enemy"), nation.getName()));
				}
				
				plugin.updateCache();
			} catch (AlreadyRegisteredException e) {
				remove.add(targetNation);
			} catch (NotRegisteredException e) {
				remove.add(targetNation);
			}
			
		for (Nation newEnemy : remove)
			enemies.remove(newEnemy);

		if (enemies.size() > 0) {
			String msg = "";
			
			for (Nation newEnemy : enemies)
				msg += newEnemy.getName() + ", ";
			
			msg = msg.substring(0, msg.length()-2);
			if (add)
				msg = String.format(TownySettings.getLangString("msg_enemy_nations"), player.getName(), msg);
			else
				msg = String.format(TownySettings.getLangString("msg_enemy_to_neutral"), player.getName(), msg);
			
			plugin.getTownyUniverse().sendNationMessage(nation, ChatTools.color(msg));
			plugin.getTownyUniverse().getDataSource().saveNations();
		} else
			plugin.sendErrorMsg(player, TownySettings.getLangString("msg_invalid_name"));

	}
	
	public void nationSet(Player player, String[] split) {
		
		if (split.length == 0) {			
			player.sendMessage(ChatTools.formatTitle("/nation set"));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "king " + TownySettings.getLangString("res_2"), ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "capital [town]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "taxes [$]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "name [name]", ""));
			player.sendMessage(ChatTools.formatCommand("", "/nation set", "neutral [on/off]", ""));
		} else {
			Resident resident;
			Nation nation;
			try {
				resident = plugin.getTownyUniverse().getResident(player.getName());
				nation = resident.getTown().getNation();
				if (!resident.isKing())
					if (!nation.hasAssistant(resident))
						throw new TownyException(TownySettings.getLangString("msg_not_king_ass"));
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
				return;
			}

			// TODO: Let admin's call a subfunction of this.
			if (split[0].equalsIgnoreCase("king")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set king Dumbo");
				else
					try {
						Resident newKing = plugin.getTownyUniverse().getResident(split[1]);
						String oldKingsName = nation.getCapital().getMayor().getName();
						nation.setKing(newKing);
						plugin.deleteCache(oldKingsName);
						plugin.deleteCache(newKing.getName());
						plugin.getTownyUniverse().sendNationMessage(nation, TownySettings.getNewKingMsg(newKing.getName()));
					} catch (TownyException e) {
						plugin.sendErrorMsg(player, e.getError());
					}
			} else if (split[0].equalsIgnoreCase("capital")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set capital 70");
				else
					try {
						Town newCapital = plugin.getTownyUniverse().getTown(split[1]);
						nation.setCapital(newCapital);
						plugin.updateCache();
						plugin.getTownyUniverse().sendNationMessage(nation, TownySettings.getNewKingMsg(newCapital.getMayor().getName()));
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					} catch (TownyException e) {
						plugin.sendErrorMsg(player, e.getError());
					}
			} else if (split[0].equalsIgnoreCase("taxes")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set taxes 70");
				else
					try {
						nation.setTaxes(Integer.parseInt(split[1]));
						plugin.getTownyUniverse().sendNationMessage(nation, String.format(TownySettings.getLangString("msg_town_set_nation_tax"), player.getName(), split[1]));
					} catch (NumberFormatException e) {
						plugin.sendErrorMsg(player, TownySettings.getLangString("msg_error_must_be_int"));
					}
			} else if (split[0].equalsIgnoreCase("name")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set name Plutoria");
				else
					nationRename(player, nation, split[1]);
			} else if (split[0].equalsIgnoreCase("neutral")) {
				if (split.length < 2)
					plugin.sendErrorMsg(player, "Eg: /nation set neutral [on/off]");
				else
					try {
						if (!TownySettings.isDeclaringNeutral())
							throw new TownyException(TownySettings.getLangString("msg_neutral_disabled"));
							

						boolean choice = parseOnOff(split[1]);
						Double cost = TownySettings.getNationNeutralityCost();
						
						if (choice && TownySettings.isUsingIConomy() && !nation.pay(cost))
							throw new TownyException(TownySettings.getLangString("msg_nation_cant_neutral"));
							
						nation.setNeutral(choice);
						plugin.updateCache();

						// send message depending on if using IConomy and charging for neutral
						if (TownySettings.isUsingIConomy() && cost > 0)
							plugin.sendMsg(player, String.format(TownySettings.getLangString("msg_you_paid"), cost + TownyIConomyObject.getIConomyCurrency()));
						else							
							plugin.sendMsg(player, TownySettings.getLangString("msg_nation_set_neutral"));
						
						plugin.getTownyUniverse().sendNationMessage(nation, TownySettings.getLangString("msg_nation_neutral") + (nation.isNeutral() ? Colors.Green : Colors.Red + " not") + " neutral.");
					} catch (IConomyException e) {
						plugin.sendErrorMsg(player, e.getError());
					} catch (TownyException e) {
						try {
							nation.setNeutral(false);
						} catch (TownyException e1) {
							// TODO Auto-generated catch block
							e1.printStackTrace();
						}
						plugin.sendErrorMsg(player, e.getError());
					} catch (Exception e) {
						plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
					}
			} else {
				plugin.sendErrorMsg(player, String.format(TownySettings.getLangString("msg_err_invalid_property"), "nation"));
				return;
			}

			plugin.getTownyUniverse().getDataSource().saveNation(nation);
		}
	}
	
	public void nationRename(Player player, Nation nation, String newName) {
		try {
			plugin.getTownyUniverse().renameNation(nation, newName);
			plugin.getTownyUniverse().sendNationMessage(nation, String.format(TownySettings.getLangString("msg_nation_set_name"), player.getName(), newName));
		} catch (TownyException e) {
			plugin.sendErrorMsg(player, e.getError());
		}
	}
	
	private List<Resident> getResidents(Player player, String[] names) {
		List<Resident> invited = new ArrayList<Resident>();
		for (String name : names)
			try {
				Resident target = plugin.getTownyUniverse().getResident(name);
				invited.add(target);
			} catch (TownyException x) {
				plugin.sendErrorMsg(player, x.getError());
			}
		return invited;
	}
	
	private boolean parseOnOff(String s) throws Exception {
		if (s.equalsIgnoreCase("on"))
			return true;
		else if (s.equalsIgnoreCase("off"))
			return false;
		else
			throw new Exception(String.format(TownySettings.getLangString("msg_err_invalid_input"), " on/off."));
	}
	

}
