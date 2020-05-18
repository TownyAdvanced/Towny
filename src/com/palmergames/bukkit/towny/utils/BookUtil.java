package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.economy.AccountAuditor;
import com.palmergames.bukkit.towny.object.economy.Audit;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;

import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Material;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;


import java.util.ArrayList;
import java.util.List;

public class BookUtil {
	public static ItemStack createAuditBook(EconomyHandler handler) {
		AccountAuditor auditor = handler.getAccount().getAuditor();
		
		ItemStack auditBook = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta bookMeta = (BookMeta) auditBook.getItemMeta();
		bookMeta.setTitle("Audit Book");
		bookMeta.setAuthor("Me");
		
		StringBuilder text = new StringBuilder();
		List<BaseComponent[]> components = new ArrayList<>();
		ComponentBuilder builder = new ComponentBuilder();
		
		for (int i = 0; i < auditor.getAuditHistory().size(); i++) {
			Audit audit = auditor.getAuditHistory().get(i);

			TextComponent textComponent = new TextComponent("\n" + audit.toString());
			textComponent.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, new ComponentBuilder("Reason: " + audit.getReason()).create()));
			builder.append(textComponent);

			if (i % 25 == 0 && i != 0) {
				components.add(builder.create());
				builder = new ComponentBuilder();
				continue;
			}
			
			if (i == auditor.getAuditHistory().size() - 1) {
				components.add(builder.create());
			}
		}

		System.out.println("Component Length = " + components.size());

		
		bookMeta.spigot().setPages(components);
		auditBook.setItemMeta(bookMeta);
		return auditBook;
	}
}
