package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.conversation.ResidentConversation;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.object.gui.EditGUI;
import com.palmergames.bukkit.towny.object.gui.PermissionGUI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.Style;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
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
	
	public static final Component SAVE = Component.text("Save", Style.style().color(TextColor.color(NamedTextColor.GREEN.asHSV())).decorate(TextDecoration.BOLD).build());
	public static final Component BACK = Component.text("Back", Style.style().color(TextColor.color(NamedTextColor.RED.asHSV())).decorate(TextDecoration.BOLD).build());
	public static final Component DELETE = Component.text("Delete", Style.style().color(TextColor.color(NamedTextColor.RED.asHSV())).decorate(TextDecoration.BOLD).build());

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
			TownyAPI.getInstance().testPlotOwnerOrThrow(resident, townBlock);
		} catch (TownyException e) {
			canEdit = false;
		}
		
		Inventory page = ResidentUtil.getBlankPage(Translatable.of("permission_gui_header").forLocale(resident));
		ArrayList<Inventory> pages = new ArrayList<>();

		for (Entry<Resident, PermissionData> entry : townBlock.getPermissionOverrides().entrySet()) {
			ItemStack skull = new ItemStack(Material.PLAYER_HEAD);
			SkullMeta meta = (SkullMeta) skull.getItemMeta();
			
			if (!entry.getKey().hasUUID())
				meta.setOwningPlayer(BukkitTools.getOfflinePlayer(entry.getKey().getName())); 
			else
				meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey().getUUID()));
			
			meta.displayName(Component.text(entry.getKey().getName(), NamedTextColor.GOLD));

			List<String> lore = new ArrayList<>();
			lore.add(entry.getValue().getPermissionTypes()[ActionType.BUILD.getIndex()].getColor() + "Build" + Colors.Gray + "  | " + entry.getValue().getPermissionTypes()[ActionType.DESTROY.getIndex()].getColor() + "Destroy");
			lore.add(entry.getValue().getPermissionTypes()[ActionType.SWITCH.getIndex()].getColor() + "Switch" + Colors.Gray + " | " + entry.getValue().getPermissionTypes()[ActionType.ITEM_USE.getIndex()].getColor() + "Item");

			if (canEdit) {
				if (entry.getValue().getLastChangedAt() > 0 && !entry.getValue().getLastChangedBy().equals(""))
					lore.add(Translatable.of("msg_last_edited", TownyFormatter.lastOnlineFormat.format(entry.getValue().getLastChangedAt()), entry.getValue().getLastChangedBy()).forLocale(resident));
					
				lore.add(Translatable.of("msg_click_to_edit").forLocale(resident));
			}

			meta.setLore(lore);
			skull.setItemMeta(meta);
			
			if (page.firstEmpty() == 46) {
				pages.add(page);
				page = ResidentUtil.getBlankPage(Translatable.of("permission_gui_header").forLocale(resident));
			}

			page.addItem(skull);
		}
		
		if (canEdit) {
			ItemStack addButton = new ItemStack(Material.NAME_TAG);
			ItemMeta addButtonMeta = addButton.getItemMeta();
			addButtonMeta.displayName(Component.text("Add Player", NamedTextColor.GOLD));
			addButton.setItemMeta(addButtonMeta);

			page.setItem(46, addButton);
		}
		
		page.setItem(52, createTutorialBook());
		
		pages.add(page);
		resident.setGUIPages(pages);
		resident.setGUIPageNum(0);
		new PermissionGUI(resident, pages.get(0), Component.text(Translatable.of("permission_gui_header").forLocale(resident)), townBlock, canEdit);
	}
	
	public static void openPermissionEditorGUI(@NotNull Resident resident, @NotNull TownBlock townBlock, @NotNull ItemStack clickedItem) {
		Inventory inventory = Bukkit.createInventory(null, 54, Component.text(Translatable.of("permission_gui_header").forLocale(resident)));
		
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
		saveButtonMeta.displayName(SAVE);
		saveButton.setItemMeta(saveButtonMeta);
		
		ItemStack backButton = new ItemStack(Material.RED_WOOL);
		ItemMeta backButtonMeta = saveButton.getItemMeta();
		backButtonMeta.displayName(BACK);
		backButton.setItemMeta(backButtonMeta);
		
		ItemStack deleteButton = new ItemStack(Material.RED_WOOL);
		backButtonMeta.displayName(DELETE);
		deleteButton.setItemMeta(backButtonMeta);
		
		inventory.setItem(48, saveButton);
		inventory.setItem(50, backButton);
		inventory.setItem(53, deleteButton);
		
		new EditGUI(resident, inventory, Component.text(Translatable.of("permission_gui_header").forLocale(resident)), townBlock, skullOwner);
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
	
	public static void handleConversation(Player player) {
		TownBlock startingTownBlock = WorldCoord.parseWorldCoord(player).getTownBlockOrNull();
		if (startingTownBlock == null) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_claimed_1"));
			return;
		}
		
		new ResidentConversation(player).runOnResponse((res) -> {
			if (!TownyUniverse.getInstance().getPermissionSource().testPermission(player, PermissionNodes.TOWNY_COMMAND_PLOT_PERM_ADD.getNode())) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_command_disable"));
				return;
			}
			
			Resident resident = (Resident) res;
			if (startingTownBlock.hasPlotObjectGroup()) {
				PlotGroup group = startingTownBlock.getPlotObjectGroup();
					
				if (group.getPermissionOverrides().containsKey(resident)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_overrides_already_set", resident.getName(), Translatable.of("plotgroup_sing")));
					return;
				}

				group.putPermissionOverride(resident, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));
			} else {
				if (startingTownBlock.getPermissionOverrides().containsKey(resident)) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_overrides_already_set", resident.getName(), Translatable.of("townblock")));
					return;
				}

				startingTownBlock.getPermissionOverrides().put(resident, new PermissionData(PermissionGUIUtil.getDefaultTypes(), player.getName()));
				startingTownBlock.save();
			}
			
			TownyMessaging.sendMsg(player, Translatable.of("msg_overrides_added", resident.getName()));
			PermissionGUIUtil.openPermissionGUI(TownyAPI.getInstance().getResident(player), startingTownBlock);
		});
	}
}
