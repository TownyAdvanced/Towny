package com.palmergames.bukkit.towny.object.gui;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.PermissionData;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil;
import com.palmergames.bukkit.towny.utils.PermissionGUIUtil.SetPermissionType;

import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.Arrays;
import java.util.Optional;

public class EditGUI extends PermissionGUI {
	private final Resident editor;
	private final Resident selectedResident;

	public EditGUI(Resident res, Inventory inv, Component name, TownBlock townBlock, Resident selectedResident) {
		super(res, inv, name, townBlock, true);
		this.editor = res;
		this.selectedResident = selectedResident;
	}

	/**
	 * Saves updated permissions 
	 */
	public void saveChanges() {
		SetPermissionType[] newTypes = new SetPermissionType[4];
		Arrays.fill(newTypes, SetPermissionType.UNSET);
		
		for (int i = 0; i < 4; i++) {
			final Material type = Optional.ofNullable(getInventory().getItem(PermissionGUIUtil.getWoolSlots()[i])).map(ItemStack::getType).orElse(null);
			if (type == null)
				continue;
			
			if (type == Material.LIME_WOOL)
				newTypes[i] = SetPermissionType.SET;
			else if (type == Material.RED_WOOL)
				newTypes[i] = SetPermissionType.NEGATED;
			else if (type == Material.GRAY_WOOL)
				newTypes[i] = SetPermissionType.UNSET;
		}

		if (getTownBlock().hasPlotObjectGroup())
			getTownBlock().getPlotObjectGroup().putPermissionOverride(selectedResident, new PermissionData(newTypes, editor.getName()));
		else {
			getTownBlock().getPermissionOverrides().put(selectedResident, new PermissionData(newTypes, editor.getName()));
			getTownBlock().save();
		}

		Towny.getPlugin().deleteCache(selectedResident);

		exitScreen();
	}

	public void exitScreen() {
		PermissionGUIUtil.openPermissionGUI(editor, super.getTownBlock());
	}
	
	public void deleteResident() {
		if (getTownBlock().hasPlotObjectGroup())
			getTownBlock().getPlotObjectGroup().removePermissionOverride(selectedResident);
		else {
			getTownBlock().getPermissionOverrides().remove(selectedResident);
			getTownBlock().save();
		}
		
		Towny.getPlugin().deleteCache(selectedResident);

		exitScreen();
	}
}