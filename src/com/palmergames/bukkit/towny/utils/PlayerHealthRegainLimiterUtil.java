package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;
import java.util.ArrayList;

public class PlayerHealthRegainLimiterUtil {

	public final static String METADATA_KEY_NAME =  "towny.recent.player.health.regain.amount";

	/**
	 * Remove recent health regain records from all online players
	 */
	public static void clearAllRecentHealthGainAmounts() {
		for (Player player : new ArrayList<>(BukkitTools.getOnlinePlayers())) {
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			try {
				if (player.isOnline()) {
					if(player.hasMetadata(METADATA_KEY_NAME)) {
						player.removeMetadata(METADATA_KEY_NAME, Towny.getPlugin());
					}
				}
			} catch (Exception e) {
				try {
					TownyMessaging.sendErrorMsg("Problem removing recent healing potions for player " + player.getName());
				} catch (Exception e2) {
					TownyMessaging.sendErrorMsg("Problem removing recent healing potions (could not read player name");
				}
				e.printStackTrace();
			}
		}
	}
}
