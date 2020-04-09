package com.palmergames.bukkit.towny.war.siegewar.utils;


import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.towny.war.siegewar.locations.HeldItemsCombination;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;

/**
 * This class contains utility functions related to the dynmap
 *
 * @author Goosius
 */
public class SiegeWarDynmapUtil {

	public static final MetadataValue invisibilityMetaDataValue =
		new MetadataValue() {
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
				return 0;
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
				return false;
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

	/**
	 * Evaluate the visibility of players on the dynmap
	 *
	 * Kings & generals - always visible
	 * Players in banner control sessions - always visible
	 * Others - can become map-invisible via the following methods
	 * 1. Equip shield in off hand
	 * 2. Equip compass in off hand
	 * 3. Take Invisibility potion
	 */
	public static void evaluateTacticalVisibilityOfPlayers() {
		TownyUniverse universe = TownyUniverse.getInstance();
		Towny plugin = Towny.getPlugin();

		for(Player player: BukkitTools.getOnlinePlayers()) {
			try {
				if (universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_ATTACK.getNode())) {
					//King or general - visible on map
					player.removeMetadata("tacticallyInvisible", plugin);
					return;

				} else if (universe.getPlayersInBannerControlSessions().contains(player)) {
					//In banner control session - visible on map
					player.removeMetadata("tacticallyInvisible", plugin);
					return;

				} else {
					//If holding special item(s) - invisible on map
					for(HeldItemsCombination heldItemsCombination: TownySettings.getWarSiegeTacticalVisibilityItems()) {

						//Off Hand
						if(!heldItemsCombination.isIgnoreOffHand() && player.getInventory().getItemInOffHand().getType() != heldItemsCombination.getOffHandItemType())
							continue;  //off hand does not match

						//Main hand
						if(!heldItemsCombination.isIgnoreMainHand() && player.getInventory().getItemInMainHand().getType() != heldItemsCombination.getMainHandItemType())
							continue; //main hand does not match

						//Player invisible on map
						player.setMetadata("tacticallyInvisible", invisibilityMetaDataValue);
						return;
					}
				}

				//Player is visible on map
				player.removeMetadata("tacticallyInvisible", plugin);
			} catch (Exception e) {
				try {
					System.out.println("Problem evaluating tactical visibility for player " + player.getName());
				} catch (Exception e2) {
					System.out.println("Problem evaluating tactical visibility (could not read player name)");
				}
				e.printStackTrace();
			}
		}
	}
}
