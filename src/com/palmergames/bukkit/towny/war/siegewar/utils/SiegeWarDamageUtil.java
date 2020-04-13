package com.palmergames.bukkit.towny.war.siegewar.utils;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.util.TimeMgmt;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

public class SiegeWarDamageUtil {

	public static String TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID = "towny.post.spawn.immunity";

	/**
	 * Grant post spawn immunity to a player
	 *
	 * @param player
	 */
	public static void grantPostSpawnImmunity(Player player) {
		try {
			if (!player.hasMetadata(TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID)) {

				MetadataValue postSpawnDamageImmunity =
					new MetadataValue() {
						private long immunityEndTime =
							System.currentTimeMillis() +
								(int) (TownySettings.getWarSiegePostSpawnDamageImmunityMinimumDurationSeconds() * TimeMgmt.ONE_SECOND_IN_MILLIS);

						@Override
						public Object value() {
							return null;
						}

						@Override
						public int asInt() {
							return 0;
						}

						@Override
						public float asFloat() {
							return 0;
						}

						@Override
						public double asDouble() {
							return 0;
						}

						@Override
						public long asLong() {
							return immunityEndTime;
						}

						@Override
						public short asShort() {
							return 0;
						}

						@Override
						public byte asByte() {
							return 0;
						}

						@Override
						public boolean asBoolean() {
							return true;
						}

						@Override
						public String asString() {
							return null;
						}

						@Override
						public Plugin getOwningPlugin() {
							return Towny.getPlugin();
						}

						@Override
						public void invalidate() {
						}
					};
				player.setMetadata(TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID, postSpawnDamageImmunity);
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
	 * Checks if the given player can damage another player
	 *
	 * @param attackingPlayer
	 * @return true if damage is prevented
	 */
	public static boolean isPlayerPreventedFromDamagingOtherPlayers(Player attackingPlayer) {
		if(
			(TownySettings.getWarSiegePostSpawnDamageImmunityEnabled() && attackingPlayer.hasMetadata(TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID))
				|| 
			(TownySettings.getWarSiegeTownNeutralityEnabled() && isPlayerFromANeutralOrDesiredNeutralTown(attackingPlayer))
		) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Checks if the given entity can be damaged
	 *
	 * @param entity the involved entity
	 * @return true if damage is prevented
	 */
	public static boolean canEntityBeDamaged(Entity entity) {
		if(TownySettings.getWarSiegePostSpawnDamageImmunityEnabled() 
			&& entity instanceof Player
			&& entity.hasMetadata(TOWNY_POST_SPAWN_IMMUNITY_METADATA_ID)) {
			return false;
		} else {
			return true;
		}
	}

	private static boolean isPlayerFromANeutralOrDesiredNeutralTown(Player player) {
		try {
			Resident resident = TownyUniverse.getInstance().getDataSource().getResident(player.getName());
			if(resident.hasTown()) {
				return resident.getTown().isNeutral() || resident.getTown().getDesiredNeutralityValue();
			} else {
				return false;
			}
		} catch (NotRegisteredException e) { return false; }
	}
}
