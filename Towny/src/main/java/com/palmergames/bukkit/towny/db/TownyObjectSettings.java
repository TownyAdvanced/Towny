package com.palmergames.bukkit.towny.db;

import org.bukkit.command.CommandSender;

import com.palmergames.bukkit.towny.TownyLogger;
import com.palmergames.bukkit.towny.object.Town;

public class TownyObjectSettings {

	public void updateTown(Town town, CommandSender sender, String setting, Object value) {
		TownyLogger.getInstance().logSettingChange(town, sender, setting, value);

		if (value instanceof Boolean b)
			updateTownBooleanSetting(town, setting, b);

		town.save();
	}

	private void updateTownBooleanSetting(Town town, String setting, boolean b) {
		switch (setting) {
		case "neutral" -> town.setNeutral(b);
		}
	}
}
