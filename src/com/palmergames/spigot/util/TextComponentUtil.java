package com.palmergames.spigot.util;

/*
import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.palmergames.bukkit.towny.object.Town;

import net.md_5.bungee.api.ChatColor;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent; 
*/

public class TextComponentUtil {
	
	
	
	/**
	 * Get a formated List of TextComponents for a Town's Warps.
	 * 
	 * '  &3/town warp &bGoldFarm Geneva &7(75.0, 52.25, -400.0, World_nether)'
	 * 
	 * 
	 * @param town
	 * @param townSpecific
	 * @return Output
	 */
	/* One of these days :(
	public static List<TextComponent> formatWarps(Town town, boolean townSpecific) {
		List<TextComponent> list = new ArrayList<TextComponent>();
		
		for (int i = 0; i < town.getWarpCount(); i++) {
			
			String warp = (String) town.getWarps().keySet().toArray()[i];
			Location loc = town.getWarp(warp);
			
			TextComponent command = new TextComponent("  /town warp");
			command.setColor(ChatColor.DARK_AQUA);
			command.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "/town warp " + warp + " " + (townSpecific ? town.getName():"")));

			TextComponent command2 = new TextComponent(" " + warp + " " + (townSpecific ? town.getName():""));
			command2.setColor(ChatColor.AQUA);
			command2.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, "/town warp " + warp + " " + (townSpecific ? town.getName():"")));
			command2.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT,
					new ComponentBuilder("Click to Teleport to " + warp + "!").create()));
			
			TextComponent info = new TextComponent("(" + loc.getBlockX() + ", "  + loc.getBlockY() + ", " + loc.getBlockZ() + ", " + loc.getWorld().getName() + ")");
			info.setColor(ChatColor.GRAY);
			info.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.SUGGEST_COMMAND, "(" + loc.getBlockX() + ", "  + loc.getBlockY() + ", " + loc.getBlockZ() + ", " + loc.getWorld().getName() + ")"));

			command.addExtra(command2);
			command.addExtra(info);
			
			list.add(command);
		}

		return list;
	}
	
	public TextComponent getTextComponent(String text, ChatColor color, String hoverMessage, String clickCommand) {
		TextComponent message = new TextComponent(text);
		message.setColor(color);
		message.setHoverEvent(new HoverEvent(net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT, new ComponentBuilder(hoverMessage).create()));
		message.setClickEvent(new ClickEvent(net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND, clickCommand));
		return message;
	}

	public static void send(Player player, TextComponent component) {
		player.spigot().sendMessage(component);
	}
	*/
}
