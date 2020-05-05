package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.db.TownyDataSource;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.TimeTools;
import org.bukkit.entity.Player;
import java.util.ArrayList;

public class PostRespawnPeacefulnessUtil {
	/**
	 * Grant post spawn immunity to a player
	 *
	 * @param player
	 */
	public static void grantPostRespawnPeacefulness(Player player) {
		Resident resident;
		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();

		try {
			try {
				resident = dataSource.getResident(player.getName());
			} catch (NotRegisteredException e) {
				return;
			}

			if(!resident.isPostRespawnPeacefulEnabled()) {
				resident.setPostRespawnPeacefulEnabled(true);
				int ticks = TimeTools.convertToShortTicks(TownySettings.getWarCommonPostRespawnPeacefulnessDurationSeconds());
				resident.setPostRespawnPeacefulShortTicksRemaining(ticks);
			}

		} catch (Exception e) {
			try {
				TownyMessaging.sendErrorMsg("Problem granting post spawn damage immunity for player " + player.getName());
			} catch (Exception e2) {
				TownyMessaging.sendErrorMsg("Problem granting post spawn damage immunity (could not read player name");
			}
			e.printStackTrace();
		}
	}

	/**
	 * This method cycles through all online players
	 * It determines which players currently have post-respawn peacefulness.
	 * In each such player, the counter is reduced.
	 * If the counter hits 0, post-respawn-peacefulness is removed
	 */
	public static void evaluatePostRespawnPeacefulnessRemovals() {
		Resident resident;
		TownyDataSource dataSource = TownyUniverse.getInstance().getDataSource();
		
		for(Player player: new ArrayList<>(BukkitTools.getOnlinePlayers())) {
			/*
			 * We are running in an Async thread so MUST verify all objects.
			 */
			try {
				if (player.isOnline()) {
					try {
						resident = dataSource.getResident(player.getName());
					} catch (NotRegisteredException e) {
						continue; //Next player pls
					}

					if (resident.isPostRespawnPeacefulEnabled()) {
						resident.decrementPostRespawnPeacefulShortTicksRemaining();
					}

					if (resident.getPostRespawnPeacefulShortTicksRemaining() < 1) {
						resident.setPostRespawnPeacefulEnabled(false);
					}
				}
			} catch (Exception e) {
				try {
					TownyMessaging.sendErrorMsg("Problem removing post-respawn-peacefulness for player " + player.getName());
				} catch (Exception e2) {
					TownyMessaging.sendErrorMsg("Problem removing post-respawn-peacefulness (could not read player name");
				}
				e.printStackTrace();
			}
		}
	}

	public static boolean doesPlayerHavePostRespawnPeacefulness(Player player) {
		try {
			return TownyUniverse.getInstance().getDataSource().getResident(player.getName()).isPostRespawnPeacefulEnabled();
		} catch (NotRegisteredException e) {
			return false;
		}
	}
}
