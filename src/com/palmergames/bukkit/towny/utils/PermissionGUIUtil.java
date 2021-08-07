package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.command.PlotCommand;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.gui.EditGUI;
import com.palmergames.bukkit.towny.object.gui.PermissionGUI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

public class PermissionGUIUtil {
	private static final SetPermissionType[] defaultTypes = new SetPermissionType[]{SetPermissionType.UNSET, SetPermissionType.UNSET, SetPermissionType.UNSET, SetPermissionType.UNSET};
	private static final int[] woolSlots = new int[]{21, 23, 30, 32};
	
	public enum SetPermissionType {
		UNSET(Colors.Gray, Material.GRAY_WOOL),
		SET(Colors.Green, Material.LIME_WOOL),
		NEGATED(Colors.Red, Material.RED_WOOL);
		
		private String color;
		private Material woolColour;
		
		SetPermissionType(String color, Material woolColour) {
			this.color = color;
			this.woolColour = woolColour;
		}

		public String getColor() {
			return color;
		}

		public Material getWoolColour() {
			return woolColour;
		}
	}
	
	public static void openPermissionGUI(@NotNull Resident resident, @NotNull TownBlock townBlock) {
		boolean canEdit = true;
		try {
			PlotCommand.plotTestOwner(resident, townBlock);
		} catch (TownyException e) {
			canEdit = false;
		}
		
		Inventory page = ResidentUtil.getBlankPage(Translation.of("permission_gui_header", resident));
		ArrayList<Inventory> pages = new ArrayList<>();

		for (Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet()) {
			ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) skull.getItemMeta();
			
			if (!entry.getKey().hasUUID())
				meta.setOwningPlayer(BukkitTools.getOfflinePlayer(entry.getKey().getName())); 
			else
				meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey().getUUID()));
			
			meta.setDisplayName(Colors.Gold + entry.getKey().getName());

			List<String> lore = new ArrayList<>();
			lore.add(entry.getValue().getPermissionTypes()[ActionType.BUILD.getIndex()].getColor() + "Build" + Colors.Gray + "  | " + entry.getValue().getPermissionTypes()[ActionType.DESTROY.getIndex()].getColor() + "Destroy");
			lore.add(entry.getValue().getPermissionTypes()[ActionType.SWITCH.getIndex()].getColor() + "Switch" + Colors.Gray + " | " + entry.getValue().getPermissionTypes()[ActionType.ITEM_USE.getIndex()].getColor() + "Item");

			if (canEdit) {
				if (entry.getValue().getLastChangedAt() > 0 && !entry.getValue().getLastChangedBy().equals(""))
					lore.add(Translation.of("msg_last_edited", resident, TownyFormatter.lastOnlineFormat.format(entry.getValue().getLastChangedAt()), entry.getValue().getLastChangedBy()));
					
				lore.add(Translation.of("msg_click_to_edit", resident));
			}

			meta.setLore(lore);
			skull.setItemMeta(meta);
			
			if (page.firstEmpty() == 46) {
				pages.add(page);
				page = ResidentUtil.getBlankPage(Translation.of("permission_gui_header", resident));
			}

			page.addItem(skull);
		}
		
		page.setItem(52, createTutorialBook());
		
		pages.add(page);
		resident.setGUIPages(pages);
		resident.setGUIPageNum(0);
		new PermissionGUI(resident, pages.get(0), Translation.of("permission_gui_header", resident), townBlock, canEdit);
	}
	
	public static void openPermissionEditorGUI(@NotNull Resident resident, @NotNull TownBlock townBlock, @NotNull ItemStack clickedItem) {
		Inventory inventory = Bukkit.createInventory(null, 54, Translation.of("permission_gui_header", resident));
		
		SkullMeta meta = (SkullMeta) clickedItem.getItemMeta();
		Resident skullOwner = TownyAPI.getInstance().getResident(Colors.strip(meta.getDisplayName()));
		
		clickedItem.setItemMeta(meta);
		inventory.setItem(4, clickedItem);
		
		SetPermissionType[] setPermissionTypes = townBlock.getPermissionOverrides().get(skullOwner).getPermissionTypes();
		for (ActionType actionType : ActionType.values()) {
			ItemStack wool = new ItemStack(setPermissionTypes[actionType.getIndex()].getWoolColour());
			ItemMeta woolMeta = wool.getItemMeta();
			woolMeta.setDisplayName(setPermissionTypes[actionType.getIndex()].getColor() + ChatColor.BOLD + actionType.getCommonName());
			
			wool.setItemMeta(woolMeta);
			inventory.setItem(woolSlots[actionType.getIndex()], wool);
		}
		
		ItemStack saveButton = new ItemStack(Material.LIME_WOOL);
		ItemMeta saveButtonMeta = saveButton.getItemMeta();
		saveButtonMeta.setDisplayName(Colors.LightGreen + ChatColor.BOLD + "Save");
		saveButton.setItemMeta(saveButtonMeta);
		
		ItemStack backButton = new ItemStack(Material.RED_WOOL);
		ItemMeta backButtonMeta = saveButton.getItemMeta();
		backButtonMeta.setDisplayName(Colors.Red + ChatColor.BOLD + "Back");
		backButton.setItemMeta(backButtonMeta);
		
		ItemStack deleteButton = new ItemStack(Material.RED_WOOL);
		backButtonMeta.setDisplayName(Colors.Red + ChatColor.BOLD + "Delete");
		deleteButton.setItemMeta(backButtonMeta);
		
		inventory.setItem(48, saveButton);
		inventory.setItem(50, backButton);
		inventory.setItem(53, deleteButton);
		
		new EditGUI(resident, inventory, Translation.of("permission_gui_header", resident), townBlock, skullOwner);
	}

	public static SetPermissionType[] getDefaultTypes() {
		return defaultTypes;
	}

	public static int[] getWoolSlots() {
		return woolSlots;
	}
	
	public static ItemStack createTutorialBook() {
		ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
		BookMeta meta = (BookMeta) book.getItemMeta();

		List<String> pages = new ArrayList<>();
		pages.add("    §lPlot Perm GUI§r\n\nUsing the GUI, you can give or remove permissions from individual players in your plots.\n\n§l  Getting Started§r\n\nTo start, you will need to add players to the GUI. You can do this using /plot perm add.");
		pages.add("After a player has been added, you can now start editing their permissions.\n\n§l    Permissions§r\n\nAfter you've clicked on a player head, you will be able to edit their permissions.§a Green§0 means that this player has this permission.");
		pages.add("§cRed§0 means that this player does not have this permission.\n\n§7Gray§0 means that normal plot permissions apply.\n\nWhen starting out, all permissions will be gray. Note that denying permissions will not work for plot owners or mayors.");

		meta.setTitle("GUI Tutorial");
		meta.setGeneration(BookMeta.Generation.ORIGINAL);
		meta.setPages(pages);
		meta.setAuthor("Warriorrr");

		book.setItemMeta(meta);
		return book;
	}
}
