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
import com.palmergames.bukkit.towny.utils.TownyComponents;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.sound.Sound;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownyInventory;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.Colors;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.Set;

public class TownyInventoryListener implements Listener {
	
	private final Towny plugin;
	private final Sound clickSound = Sound.sound(Key.key(Key.MINECRAFT_NAMESPACE, "block.stone_button.click_on"), Sound.Source.PLAYER, 1.0f, 1.0f);

	public TownyInventoryListener(final Towny plugin) {
		this.plugin = plugin;
	}

	@EventHandler(ignoreCancelled = true)
	public void onClick(InventoryClickEvent event) {
		if (!(event.getInventory().getHolder(false) instanceof TownyInventory townyInventory) || (event.getCurrentItem() == null && event.getHotbarButton() == -1))
			return;

		event.setCancelled(true);

		// Someone is using their hotbar buttons to place items into the TownyInventory, an action they cannot reverse causing item loss.
		if (event.getHotbarButton() > -1)
			return;

		Player player = (Player) event.getWhoClicked();
		Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());

		if (resident == null || (event.getClickedInventory() != null && !(event.getClickedInventory().getHolder(false) instanceof TownyInventory)))
			return;

		if (event.getInventory().getHolder() instanceof EditGUI editGUI) {
			
			ItemMeta meta = event.getCurrentItem().getItemMeta();
			if (meta == null)
				return;
			
			final Component customName = meta.displayName(); // TODO: after 1.21.4 becomes the minimum version, replace with customName
			final String plainCustomName = customName != null ? TownyComponents.plain(customName) : event.getCurrentItem().getType().getKey().asMinimalString();
			
			Material type = event.getCurrentItem().getType();
			if (type == Material.LIME_WOOL) {
				if (meta.getPersistentDataContainer().has(PermissionGUIUtil.EDIT_GUI_SAVE_KEY)) {
					editGUI.saveChanges();
				} else {
					final ItemStack newItem = new ItemStack(Material.RED_WOOL);
					newItem.editMeta(newMeta -> newMeta.displayName(Component.text(plainCustomName, NamedTextColor.RED, TextDecoration.BOLD)));

					event.setCurrentItem(newItem);
				}
			} else if (type == Material.RED_WOOL) {
				if (meta.getPersistentDataContainer().has(PermissionGUIUtil.EDIT_GUI_BACK_KEY)) {
					editGUI.exitScreen();
				} else if (meta.getPersistentDataContainer().has(PermissionGUIUtil.EDIT_GUI_DELETE_KEY)) {
					editGUI.deleteResident();
				} else {
					final ItemStack newItem = new ItemStack(Material.GRAY_WOOL);
					newItem.editMeta(newMeta -> newMeta.displayName(Component.text(plainCustomName, NamedTextColor.GRAY, TextDecoration.BOLD)));

					event.setCurrentItem(newItem);
				}
			} else if (type == Material.GRAY_WOOL) {
				final ItemStack newItem = new ItemStack(Material.LIME_WOOL);
				newItem.editMeta(newMeta -> newMeta.displayName(Component.text(plainCustomName, NamedTextColor.GREEN, TextDecoration.BOLD)));

				event.setCurrentItem(newItem);
			} else 
				return;
			
			editGUI.playClickSound(player);

		} else if (event.getInventory().getHolder() instanceof PermissionGUI permissionGUI) {
			if (event.getCurrentItem().getType() == Material.PLAYER_HEAD && permissionGUI.canEdit()) {
				PermissionGUIUtil.openPermissionEditorGUI(resident, permissionGUI.getTownBlock(), event.getCurrentItem());
				player.playSound(clickSound);
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