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
import com.palmergames.bukkit.util.Colors;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.persistence.PersistentDataType;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.UUID;

public class PermissionGUIUtil {
	public static final @NotNull NamespacedKey EDIT_GUI_SAVE_KEY = Objects.requireNonNull(NamespacedKey.fromString("towny:permission_edit_gui_save"));
	public static final @NotNull NamespacedKey EDIT_GUI_BACK_KEY = Objects.requireNonNull(NamespacedKey.fromString("towny:permission_edit_gui_back"));
	public static final @NotNull NamespacedKey EDIT_GUI_DELETE_KEY = Objects.requireNonNull(NamespacedKey.fromString("towny:permission_edit_gui_delete"));

	public static final @NotNull NamespacedKey GUI_ADD_KEY = Objects.requireNonNull(NamespacedKey.fromString("towny:permission_gui_add"));
	private static final @NotNull NamespacedKey GUI_PLAYER_UUID_KEY = Objects.requireNonNull(NamespacedKey.fromString("towny:permission_gui_player_uuid"));

	private static final SetPermissionType[] defaultTypes = new SetPermissionType[]{SetPermissionType.UNSET, SetPermissionType.UNSET, SetPermissionType.UNSET, SetPermissionType.UNSET};
	private static final int[] woolSlots = new int[]{21, 23, 30, 32};
	
	public enum SetPermissionType {
		UNSET(NamedTextColor.DARK_GRAY, Material.GRAY_WOOL),
		SET(NamedTextColor.DARK_GREEN, Material.LIME_WOOL),
		NEGATED(NamedTextColor.DARK_RED, Material.RED_WOOL);
		
		private final NamedTextColor color;
		private final Material woolColour;
		
		SetPermissionType(NamedTextColor color, Material woolColour) {
			this.color = color;
			this.woolColour = woolColour;
		}

		@Deprecated(since = "0.101.2.5")
		public String getColor() {
			return Colors.getLegacyFromNamedTextColor(this.color);
		}

		public TextColor color() {
			return this.color;
		}

		public Material getWoolColour() {
			return woolColour;
		}
	}
	
	public static void openPermissionGUI(@NotNull Resident resident, @NotNull TownBlock townBlock) {
		final Player player = resident.getPlayer();
		if (player == null) {
			return;
		}

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
			
			meta.setOwningPlayer(Bukkit.getOfflinePlayer(entry.getKey().getUUID()));
			meta.getPersistentDataContainer().set(GUI_PLAYER_UUID_KEY, PersistentDataType.STRING, entry.getKey().getUUID().toString());
			meta.displayName(Component.text(entry.getKey().getName(), NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));

			List<Component> lore = new ArrayList<>();

			final List<Component> actionTypeComponents = new ArrayList<>();
			for (final ActionType actionType : ActionType.values()) {
				actionTypeComponents.add(Component.text(actionType.getCommonName(), entry.getValue().getPermissionTypes()[actionType.getIndex()].color()).decoration(TextDecoration.ITALIC, false));
			}

			final Component splitter = Component.text(" | ", NamedTextColor.DARK_GRAY);
			lore.add(actionTypeComponents.get(0).append(splitter).append(actionTypeComponents.get(1)));
			lore.add(actionTypeComponents.get(2).append(splitter).append(actionTypeComponents.get(3)));

			if (canEdit) {
				if (entry.getValue().getLastChangedAt() > 0 && !entry.getValue().getLastChangedBy().isEmpty())
					lore.add(Translatable.of("msg_last_edited", TownyFormatter.lastOnlineFormat.format(entry.getValue().getLastChangedAt()), entry.getValue().getLastChangedBy()).component(player.locale()));
					
				lore.add(Translatable.of("msg_click_to_edit").component(player.locale()));
			}

			meta.lore(lore);
			skull.setItemMeta(meta);
			
			if (page.firstEmpty() == 46) {
				pages.add(page);
				page = ResidentUtil.getBlankPage(Translatable.of("permission_gui_header").forLocale(resident));
			}

			page.addItem(skull);
		}
		
		if (canEdit) {
			ItemStack addButton = new ItemStack(Material.NAME_TAG);
			addButton.editMeta(meta -> {
				meta.displayName(Component.text("Add Player", NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
				meta.getPersistentDataContainer().set(GUI_ADD_KEY, PersistentDataType.BOOLEAN, true);
			});

			page.setItem(46, addButton);
		}
		
		page.setItem(52, createTutorialBook());
		
		pages.add(page);
		resident.setGUIPages(pages);
		resident.setGUIPageNum(0);
		new PermissionGUI(resident, pages.get(0), Translatable.of("permission_gui_header").component(player.locale()), townBlock, canEdit);
	}
	
	public static void openPermissionEditorGUI(@NotNull Resident resident, @NotNull TownBlock townBlock, @NotNull ItemStack clickedItem) {
		final Player player = resident.getPlayer();
		if (player == null || !clickedItem.hasItemMeta()) {
			return;
		}
		
		Inventory inventory = Bukkit.createInventory(null, 54, Translatable.of("permission_gui_header").component(player.locale()));

		final String uuidString = clickedItem.getItemMeta().getPersistentDataContainer().get(GUI_PLAYER_UUID_KEY, PersistentDataType.STRING);
		if (uuidString == null) {
			return;
		}

		Resident skullOwner = null;
		try {
			skullOwner = TownyAPI.getInstance().getResident(UUID.fromString(uuidString));
		} catch (IllegalArgumentException ignored) {}

		if (skullOwner == null) {
			return;
		}

		inventory.setItem(4, clickedItem);
		
		SetPermissionType[] setPermissionTypes = townBlock.getPermissionOverrides().get(skullOwner).getPermissionTypes();
		for (ActionType actionType : ActionType.values()) {
			ItemStack wool = new ItemStack(setPermissionTypes[actionType.getIndex()].getWoolColour());
			ItemMeta woolMeta = wool.getItemMeta();
			woolMeta.displayName(Component.text(actionType.getCommonName(), setPermissionTypes[actionType.getIndex()].color()).decoration(TextDecoration.ITALIC, false));
			
			wool.setItemMeta(woolMeta);
			inventory.setItem(woolSlots[actionType.getIndex()], wool);
		}
		
		ItemStack saveButton = new ItemStack(Material.LIME_WOOL);
		saveButton.editMeta(meta -> {
			meta.displayName(Component.text("Save", NamedTextColor.GREEN, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.getPersistentDataContainer().set(EDIT_GUI_SAVE_KEY, PersistentDataType.BOOLEAN, true);
		});
		
		ItemStack backButton = new ItemStack(Material.RED_WOOL);
		backButton.editMeta(meta -> {
			meta.displayName(Component.text("Back", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.getPersistentDataContainer().set(EDIT_GUI_BACK_KEY, PersistentDataType.BOOLEAN, true);
		});
		
		ItemStack deleteButton = new ItemStack(Material.RED_WOOL);
		deleteButton.editMeta(meta -> {
			meta.displayName(Component.text("Delete", NamedTextColor.DARK_RED, TextDecoration.BOLD).decoration(TextDecoration.ITALIC, false));
			meta.getPersistentDataContainer().set(EDIT_GUI_DELETE_KEY, PersistentDataType.BOOLEAN, true);
		});
		
		inventory.setItem(48, saveButton);
		inventory.setItem(50, backButton);
		inventory.setItem(53, deleteButton);
		
		new EditGUI(resident, inventory, Translatable.of("permission_gui_header").component(player.locale()), townBlock, skullOwner);
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
		pages.add("    <bold>Plot Perm GUI</bold>\n\nUsing the GUI, you can give or remove permissions from individual players in your plots.\n\n<bold>  Getting Started</bold>\n\nTo start, you will need to add players to the GUI. You can do this using /plot perm add.");
		pages.add("After a player has been added, you can now start editing their permissions.\n\n<bold>    Permissions</bold>\n\nAfter you've clicked on a player head, you will be able to edit their permissions. <green>Green</green> means that this player has this permission.");
		pages.add("<red>Red</red> means that this player does not have this permission.\n\n<gray>Gray</gray> means that normal plot permissions apply.\n\nWhen starting out, all permissions will be gray. Note that denying permissions will not work for plot owners or mayors.");

		meta.setTitle("GUI Tutorial");
		meta.setGeneration(BookMeta.Generation.ORIGINAL);

		for (final String page : pages) {
			meta.addPages(MiniMessage.miniMessage().deserialize(page));
		}

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
