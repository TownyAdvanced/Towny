package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.invites.Invite;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

public class TownySpigotMessaging {
	public static void sendSpigotRequestMessage(CommandSender player, Invite invite) {
		if (invite.getSender() instanceof Town) { // Town invited Resident
			String firstline = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Invitation" + ChatColor.DARK_GRAY + "] " + ChatColor.BLUE + String.format(TownySettings.getLangString("you_have_been_invited_to_join2"), invite.getSender().getName());
			String secondline = "/" + TownySettings.getAcceptCommand() + " " + invite.getSender().getName();
			String thirdline = "/" + TownySettings.getDenyCommand() + " " + invite.getSender().getName();
			sendSpigotConfirmMessage(player, firstline, secondline, thirdline, "");
		}
		if (invite.getSender() instanceof Nation) {
			if (invite.getReceiver() instanceof Town) { // Nation invited Town
				String firstline = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Invitation" + ChatColor.DARK_GRAY + "] " + ChatColor.BLUE + String.format(TownySettings.getLangString("you_have_been_invited_to_join2"), invite.getSender().getName());
				String secondline = ChatColor.GREEN + "/t invite accept " + invite.getSender().getName();
				String thirdline = ChatColor.GREEN +  "/t invite deny " + invite.getSender().getName();
				sendSpigotConfirmMessage(player, firstline, secondline, thirdline, "");
			}
			if (invite.getReceiver() instanceof Nation) { // Nation allied Nation
				String firstline = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Invitation" + ChatColor.DARK_GRAY + "] " + ChatColor.BLUE + String.format(TownySettings.getLangString("you_have_been_requested_to_ally2"), invite.getSender().getName());
				String secondline = ChatColor.GREEN + "/n ally accept " + invite.getSender().getName();
				String thirdline = ChatColor.GREEN +  "/n ally deny " + invite.getSender().getName();
				sendSpigotConfirmMessage(player, firstline, secondline, thirdline, "");
			}
		}
	}

	/**
	 * Sends player confirm message that is clickable
	 */
	public static void sendSpigotConfirmMessage(CommandSender player, String firstline, String confirmline, String cancelline, String lastline) {

		if (firstline == null) {
			firstline = ChatColor.DARK_GRAY + "[" + ChatColor.GRAY + "Confirmation" + ChatColor.DARK_GRAY + "] " + ChatColor.BLUE + TownySettings.getLangString("are_you_sure_you_want_to_continue");
		}
		if (confirmline == null) {
			confirmline = "/" + TownySettings.getConfirmCommand();
		}
		if (cancelline == null) {
			cancelline = "/" + TownySettings.getCancelCommand();
		}
		if (lastline == null) {
			lastline = ChatColor.BLUE + TownySettings.getLangString("this_message_will_expire");
		} else {
			lastline = "";
		}

		// Create confirm button based on given params.
		TextComponent confirmComponent = new TextComponent(ChatColor.GREEN + confirmline.replace('/', '[').concat("]"));
		confirmComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(TownySettings.getLangString("msg_confirmation_spigot_hover_accept")).create()));
		confirmComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, confirmline));

		// Create cancel button based on given params.
		TextComponent cancelComponent = new TextComponent(ChatColor.GREEN + cancelline.replace('/', '[').concat("]"));
		cancelComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(TownySettings.getLangString("msg_confirmation_spigot_hover_cancel")).create()));
		cancelComponent.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, cancelline));
		
		// Use spigot to send the message.
		player.spigot().sendMessage(new ComponentBuilder(firstline + "\n")
			.append(confirmComponent).append(ChatColor.WHITE + " - " + String.format(TownySettings.getLangString("msg_confirmation_spigot_click_accept"), confirmline.replace('/', '[').replace("[",""), confirmline) + "\n")
			.append(cancelComponent).append(ChatColor.WHITE + " - " + String.format(TownySettings.getLangString("msg_confirmation_spigot_click_cancel"), cancelline.replace('/', '['), cancelline).replace("[","") + "\n")
			.append(lastline)
			.create());
	}
}
