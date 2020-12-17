package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.comparators.ComparatorType;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.chat.hover.content.Text;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class TownySpigotMessaging {
	@SuppressWarnings("deprecation")
	final static class HoverCompatibilityWrapper {
		
		final TextComponent base;
		
		public HoverCompatibilityWrapper(TextComponent base) {
			this.base = base;
		}
		
		public void setHoverText(String hoverText) {
			if (Towny.is116Plus()) {
				try {
					base.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new Text(hoverText)));
					return;
				} catch (Exception ignore) {
					// The above code can throw a ClassNotFoundException if there is an old version of BungeeCord installed.
					// Default to the code below.
				}
			}

			base.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverText).create()));
		}
	}
	
	private static HoverCompatibilityWrapper adaptForHover(TextComponent base) {
		return new HoverCompatibilityWrapper(base);
	}
	
	public static void sendSpigotRequestMessage(CommandSender player, Invite invite) {
		if (invite.getSender() instanceof Town) { // Town invited Resident
			String firstline = Translation.of("invitation_prefix") + Translation.of("you_have_been_invited_to_join2", invite.getSender().getName());
			String secondline = "/" + TownySettings.getAcceptCommand() + " " + invite.getSender().getName();
			String thirdline = "/" + TownySettings.getDenyCommand() + " " + invite.getSender().getName();
			sendSpigotConfirmMessage(player, firstline, secondline, thirdline, "");
		}
		if (invite.getSender() instanceof Nation) {
			if (invite.getReceiver() instanceof Town) { // Nation invited Town
				String firstline = Translation.of("invitation_prefix") + Translation.of("you_have_been_invited_to_join2", invite.getSender().getName());
				String secondline = "/t invite accept " + invite.getSender().getName();
				String thirdline = "/t invite deny " + invite.getSender().getName();
				sendSpigotConfirmMessage(player, firstline, secondline, thirdline, "");
			}
			if (invite.getReceiver() instanceof Nation) { // Nation allied Nation
				String firstline = Translation.of("invitation_prefix") + Translation.of("you_have_been_requested_to_ally2", invite.getSender().getName());
				String secondline = "/n ally accept " + invite.getSender().getName();
				String thirdline = "/n ally deny " + invite.getSender().getName();
				sendSpigotConfirmMessage(player, firstline, secondline, thirdline, "");
			}
		}
	}

	/**
	 * Sends a player click-able confirmation messages if the server is running on Spigot \(or a fork, like Paper.\)
	 * @param player - The player (CommandSender) to send the confirmation
	 * @param firstline - The question regarding the confirmation.
	 * @param confirmline - Line for sending the confirmation.
	 * @param cancelline - Line for sending the cancellation.
	 * @param lastline - If null, announces that the message will expire. Otherwise, ignored.
	 */
	public static void sendSpigotConfirmMessage(CommandSender player, String firstline, String confirmline, String cancelline, String lastline) {

		if (firstline == null) {
			firstline = Translation.of("confirmation_prefix") + Translation.of("are_you_sure_you_want_to_continue");
		}
		if (confirmline == null) {
			confirmline = "/" + TownySettings.getConfirmCommand();
		}
		if (cancelline == null) {
			cancelline = "/" + TownySettings.getCancelCommand();
		}
		if (lastline == null) {
			lastline = Translation.of("this_message_will_expire2");
		} else {
			lastline = "";
		}

		// Create confirm button based on given params.
		TextComponent confirmComponent = new TextComponent(ChatColor.GREEN + confirmline.replace('/', '[').concat("]"));
		adaptForHover(confirmComponent).setHoverText(Translation.of("msg_confirmation_spigot_hover_accept"));
		confirmComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/towny:" + confirmline.replace("/","")));

		// Create cancel button based on given params.
		TextComponent cancelComponent = new TextComponent(ChatColor.GREEN + cancelline.replace('/', '[').concat("]"));
		adaptForHover(cancelComponent).setHoverText(Translation.of("msg_confirmation_spigot_hover_cancel"));
		cancelComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/towny:" + cancelline.replace("/","")));
		
		// Use spigot to send the message.
		player.spigot().sendMessage(new ComponentBuilder(firstline + "\n")
			.append(confirmComponent).append(ChatColor.WHITE + " - " + Translation.of("msg_confirmation_spigot_click_accept", confirmline.replace('/', '[').replace("[",""), confirmline) + "\n")
			.append(cancelComponent).append(ChatColor.WHITE + " - " + Translation.of("msg_confirmation_spigot_click_cancel", cancelline.replace('/', '['), cancelline).replace("[","") + "\n")
			.append(lastline)
			.create());
	}
	
	public static void sendSpigotTownList(CommandSender sender, List<Town> towns, ComparatorType compType, int page, int total) {
		int iMax = Math.min(page * 10, towns.size());

		BaseComponent[] townsformatted;
		
		if ((page * 10) > towns.size()) {
			townsformatted = new BaseComponent[towns.size() % 10];
		} else {
			townsformatted = new BaseComponent[10];
		}
		
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Town town = towns.get(i);
			TextComponent townName = new TextComponent(StringMgmt.remUnderscore(town.getName()));
			townName.setColor(net.md_5.bungee.api.ChatColor.AQUA);
			
			if (!TownySettings.isTownListRandom()) {
				TextComponent nextComponent = new TextComponent(" - ");
				nextComponent.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
				TextComponent count = null;
				switch (compType) {
				case BALANCE:
					count = new TextComponent(town.getAccount().getHoldingFormattedBalance());
					break;
				case TOWNBLOCKS:
					count = new TextComponent(town.getTownBlocks().size() + "");
					break;
				default:
					count = new TextComponent(town.getResidents().size() + "");
					break;
				}
				count.setColor(net.md_5.bungee.api.ChatColor.AQUA);
				nextComponent.addExtra(count);
				townName.addExtra(nextComponent);
			}
			
			if (town.isOpen()) {
				TextComponent nextComponent = new TextComponent(Translation.of("status_title_open"));
				nextComponent.setColor(net.md_5.bungee.api.ChatColor.AQUA);
				townName.addExtra(nextComponent);
			}
			
			String spawnCost = "Free";

			if (TownySettings.isUsingEconomy() && TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));
			
			String hoverText = Translation.of("msg_click_spawn", town) + "\n" + spawnCost;
			
			TextComponent hoverComponent = new TextComponent(hoverText);
			hoverComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);
			
			adaptForHover(townName).setHoverText(hoverText);
			townName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/towny:town spawn " + town + " -ignore"));
			townsformatted[i % 10] = townName;
			
		}
		
		sender.sendMessage(ChatTools.formatTitle(Translation.of("town_plu")));
		sender.sendMessage(Colors.Blue + Translation.of("town_name") + (TownySettings.isTownListRandom() ? "" : Colors.Gray + " - " + Colors.LightBlue + compType.getName()));
		for (BaseComponent baseComponent : townsformatted) {
			sender.spigot().sendMessage(baseComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town", page, total);
		sender.spigot().sendMessage(pageFooter);
	}
	
	public static TextComponent getPageNavigationFooter(String prefix, int page, int total) {
		TextComponent backButton = new TextComponent("<<<");
		backButton.setColor(net.md_5.bungee.api.ChatColor.GOLD);
		backButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + prefix + " list " + (page - 1)));
		adaptForHover(backButton).setHoverText(Translation.of("msg_hover_previous_page"));
		
		TextComponent forwardButton = new TextComponent(">>>");
		forwardButton.setColor(net.md_5.bungee.api.ChatColor.GOLD);
		forwardButton.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/" + prefix + " list " + (page + 1)));
		adaptForHover(forwardButton).setHoverText(Translation.of("msg_hover_next_page"));
		
		TextComponent pageText = new TextComponent("   " + Translation.of("LIST_PAGE", page, total) + "   ");

		TextComponent pageFooter = new TextComponent();
		if (page < total && page > 1) {
			pageFooter.addExtra(backButton);
			pageFooter.addExtra(pageText);
			pageFooter.addExtra(forwardButton);
		} else if (page == 1 && page == total) {
			backButton.setClickEvent(null);
			backButton.setHoverEvent(null);
			backButton.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);

			forwardButton.setClickEvent(null);
			forwardButton.setHoverEvent(null);
			forwardButton.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);

			pageFooter.addExtra(backButton);
			pageFooter.addExtra(pageText);
			pageFooter.addExtra(forwardButton);
		} else if (page == 1) {
			backButton.setClickEvent(null);
			backButton.setHoverEvent(null);
			backButton.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
			pageFooter.addExtra(backButton);
			pageFooter.addExtra(pageText);
			pageFooter.addExtra(forwardButton);
		} else if (page == total) {
			forwardButton.setClickEvent(null);
			forwardButton.setHoverEvent(null);
			forwardButton.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
			pageFooter.addExtra(backButton);
			pageFooter.addExtra(pageText);
			pageFooter.addExtra(forwardButton);
		}
		
		return pageFooter;
	}
	
	public static void sendSpigotNationList(CommandSender sender, List<Nation> nations, ComparatorType compType, int page, int total) {
		int iMax = Math.min(page * 10, nations.size());

		BaseComponent[] nationsformatted;
		if ((page * 10) > nations.size()) {
			nationsformatted = new BaseComponent[nations.size() % 10];
		} else {
			nationsformatted = new BaseComponent[10];
		}
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Nation nation = nations.get(i);
			TextComponent nationName = new TextComponent(StringMgmt.remUnderscore(nation.getName()));
			nationName.setColor(net.md_5.bungee.api.ChatColor.AQUA);

			if (!TownySettings.isTownListRandom()) {
				TextComponent nextComponent = new TextComponent(" - ");
				nextComponent.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);
				
				String slug = null;
				switch (compType) {
				case BALANCE:
					slug = nation.getAccount().getHoldingFormattedBalance();
					break;
				case TOWNBLOCKS:
					slug = nation.getTownBlocks().size() + "";
					break;
				case TOWNS:
					slug = nation.getTowns().size() + "";
					break;
				default:
					slug = nation.getResidents().size() + "";
					break;
				}
				
				TextComponent townCount = new TextComponent(Colors.LightBlue + "(" + slug + ")");
				nextComponent.addExtra(townCount);
				nationName.addExtra(nextComponent);
			}

			if (nation.isOpen()) {
				TextComponent nextComponent = new TextComponent(Translation.of("status_title_open"));
				nextComponent.setColor(net.md_5.bungee.api.ChatColor.AQUA);
				nationName.addExtra(nextComponent);
			}

			String spawnCost = "Free";
			if (TownySettings.isUsingEconomy() && TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(nation.getSpawnCost()));
			
			String hoverText = Translation.of("msg_click_spawn", nation) + "\n" + spawnCost;

			TextComponent hoverComponent = new TextComponent(hoverText);
			hoverComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);


			adaptForHover(nationName).setHoverText(hoverText);
			nationName.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/towny:nation spawn " + nation + " -ignore"));
			nationsformatted[i % 10] = nationName;

		}

		sender.sendMessage(ChatTools.formatTitle(Translation.of("nation_plu")));
		sender.sendMessage(Colors.Blue + Translation.of("nation_name") + Colors.Gray + " - " + Colors.LightBlue + compType.getName());
		for (BaseComponent baseComponent : nationsformatted) {
			sender.spigot().sendMessage(baseComponent);
		}

		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:nation", page, total);
		sender.spigot().sendMessage(pageFooter);
	}
	
	public static void sendSpigotOutpostList(Player player, Town town, int page, int total) {
		int outpostsCount = town.getAllOutpostSpawns().size();
		int iMax = Math.min(page * 10, outpostsCount);
		List<Location> outposts = town.getAllOutpostSpawns();
		
		BaseComponent[] outpostsFormatted;
		
		if ((page * 10) > outpostsCount) {
			outpostsFormatted = new BaseComponent[outpostsCount % 10];
		} else {
			outpostsFormatted = new BaseComponent[10];
		}
		
		for (int i = (page - 1) * 10; i < iMax; i++) {
			Location outpost = outposts.get(i);
			TownBlock tb = TownyAPI.getInstance().getTownBlock(outpost);
			if (tb == null)
				continue;
			String name = !tb.hasPlotObjectGroup() ? tb.getName() : tb.getPlotObjectGroup().getName();
			TextComponent line = new TextComponent(Integer.toString(i + 1));
			line.setColor(net.md_5.bungee.api.ChatColor.GOLD);
			TextComponent dash = new TextComponent(" - ");
			dash.setColor(net.md_5.bungee.api.ChatColor.DARK_GRAY);			
			TextComponent outpostName = new TextComponent(name); 
			outpostName.setColor(net.md_5.bungee.api.ChatColor.GREEN);
			TextComponent worldName = new TextComponent(outpost.getWorld().getName());
			worldName.setColor(net.md_5.bungee.api.ChatColor.BLUE);
			TextComponent coords = new TextComponent("(" + outpost.getBlockX() + "," + outpost.getBlockZ()+ ")");
			coords.setColor(net.md_5.bungee.api.ChatColor.BLUE);

			line.addExtra(dash);
			if (!name.equalsIgnoreCase("")) {
				line.addExtra(outpostName);
				line.addExtra(dash);
			}
			line.addExtra(worldName);
			line.addExtra(dash);
			line.addExtra(coords);
			
			String spawnCost = "Free";

			if (TownySettings.isUsingEconomy() && TownyEconomyHandler.isActive())
				spawnCost = ChatColor.RESET + Translation.of("msg_spawn_cost", TownyEconomyHandler.getFormattedBalance(town.getSpawnCost()));

			String hoverText = Translation.of("msg_click_spawn", name.equalsIgnoreCase("") ? "outpost" : name) + "\n" + spawnCost;
			TextComponent hoverComponent = new TextComponent(hoverText);
			hoverComponent.setColor(net.md_5.bungee.api.ChatColor.GOLD);
			
			adaptForHover(line).setHoverText(hoverText);
			line.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/towny:town outpost " + (i + 1)));
			outpostsFormatted[i % 10] = line;
		}
		
		player.sendMessage(ChatTools.formatTitle(Translation.of("outpost_plu")));
		for (BaseComponent baseComponent : outpostsFormatted) {
			player.spigot().sendMessage(baseComponent);
		}
		
		// Page navigation
		TextComponent pageFooter = getPageNavigationFooter("towny:town outpost", page, total);
		player.spigot().sendMessage(pageFooter);
	}
}
