package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.gui.EditGUI;
import com.palmergames.bukkit.towny.object.gui.PermissionGUI;
import com.palmergames.bukkit.towny.object.gui.SelectionGUI;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import io.papermc.lib.PaperLib;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyInventory;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Set;

public class TownyInventoryListener implements Listener {
	
	private final Towny plugin;
	private final Sound clickSound = Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.stone_button.click_on"), Sound.Source.PLAYER, 1.0f, 1.0f);

	public TownyInventoryListener(final Towny plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if (!(PaperLib.getHolder(event.getInventory(), false).getHolder() instanceof TownyInventory townyInventory) || event.getCurrentItem() == null)
			return;

		event.setCancelled(true);

		Player player = (Player) event.getWhoClicked();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

		if (resident == null || (event.getClickedInventory() != null && !(PaperLib.getHolder(event.getClickedInventory(), false).getHolder() instanceof TownyInventory)))
			return;

		if (event.getInventory().getHolder() instanceof EditGUI editGUI) {
			
			ItemMeta meta = event.getCurrentItem().getItemMeta();
			if (meta == null)
				return;
			
			Material type = event.getCurrentItem().getType();
			if (type == Material.LIME_WOOL) {
				if (meta.getDisplayName().equals(Colors.LightGreen + ChatColor.BOLD + "Save")) {
					editGUI.saveChanges();
				} else {
					meta.setDisplayName(Colors.Red + ChatColor.BOLD + Colors.strip(meta.getDisplayName()));
					event.getCurrentItem().setType(Material.RED_WOOL);
				}
			} else if (type == Material.RED_WOOL) {
				if (meta.getDisplayName().equals(Colors.Red + ChatColor.BOLD + "Back")) {
					editGUI.exitScreen();
				} else if (meta.getDisplayName().equals(Colors.Red + ChatColor.BOLD + "Delete")) {
					editGUI.deleteResident();
				} else {
					meta.setDisplayName(Colors.Gray + ChatColor.BOLD + Colors.strip(meta.getDisplayName()));
					event.getCurrentItem().setType(Material.GRAY_WOOL);
				}
			} else if (type == Material.GRAY_WOOL) {
				meta.setDisplayName(Colors.LightGreen + ChatColor.BOLD + Colors.strip(meta.getDisplayName()));
				event.getCurrentItem().setType(Material.LIME_WOOL);
			} else 
				return;
			
			event.getCurrentItem().setItemMeta(meta);			
			editGUI.playClickSound(player);

		} else if (event.getInventory().getHolder() instanceof PermissionGUI permissionGUI) {
			if (event.getCurrentItem().getType() == Material.PLAYER_HEAD && permissionGUI.canEdit()) {
				PermissionGUIUtil.openPermissionEditorGUI(resident, permissionGUI.getTownBlock(), event.getCurrentItem());
				Towny.getAdventure().player(player).playSound(clickSound);
			} else if (event.getCurrentItem().getType() == Material.WRITTEN_BOOK) {
				player.openBook(PermissionGUIUtil.createTutorialBook());
			} else if (event.getCurrentItem().getType() == Material.NAME_TAG) {
				if (plugin.isFolia()) {
					TownyMessaging.sendErrorMsg(player, Translatable.of("msg_perm_gui_folia"));
					return;
				}
				
				PermissionGUIUtil.handleConversation(player);
				event.getWhoClicked().closeInventory();
			} else {
				permissionGUI.tryPaginate(event.getCurrentItem(), player, resident, event.getView());
			}
		} else if (event.getInventory().getHolder() instanceof SelectionGUI selectionGUI) {
			TownBlockType type = TownBlockTypeHandler.getType(Colors.strip(event.getCurrentItem().getItemMeta().getDisplayName()));
			if (type == null) {
				// The player has clicked the back/next button or an empty spot..
				selectionGUI.playClickSound(player);
				return;
			}

			Set<Material> materialSet = switch (selectionGUI.getType()) {
				case ITEMUSE -> type.getData().getItemUseIds();
				case ALLOWEDBLOCKS -> type.getData().getAllowedBlocks();
				case SWITCHES -> type.getData().getSwitchIds();
			};
			
			String title = materialSet.isEmpty()
				? Translatable.of("gui_title_no_restrictions").forLocale(resident)
				: switch (selectionGUI.getType()) {
				case ALLOWEDBLOCKS -> Translatable.of("gui_title_towny_allowedblocks", type.getName()).forLocale(resident);
				case SWITCHES -> Translatable.of("gui_title_towny_switch").forLocale(resident);
				case ITEMUSE -> Translatable.of("gui_title_towny_itemuse").forLocale(resident);
			};

			resident.setGUISelectionType(selectionGUI.getType());
			selectionGUI.playClickSound(player);
			ResidentUtil.openGUIInventory(resident, materialSet, title);
		} else {
			/*
			 * Not a PermissionGUI, EditGUI or SelectionGUI. Use normal pagination.
			 */
			townyInventory.tryPaginate(event.getCurrentItem(), player, resident, event.getView());
		}
	}
}