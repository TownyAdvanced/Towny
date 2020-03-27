package com.palmergames.bukkit.towny.war.siegewar.utils;


import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffectType;

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
	 * Pillaging player in siegezone - always visible
	 * Others - can become map-invisible via the following methods
	 * 1. Equip shield in off hand
	 * 2. Equip compass in off hand
	 * 3. Take Invisibility potion
	 */
	public static void evaluateTacticalVisibilityOfPlayers() {
		try {
			TownyUniverse universe = TownyUniverse.getInstance();
			Towny plugin = Towny.getPlugin();

			for(Player player: BukkitTools.getOnlinePlayers()) {

				if (universe.getPermissionSource().testPermission(player, PermissionNodes.TOWNY_NATION_SIEGE_ATTACK.getNode())) {
					//Visible on map
					player.removeMetadata("tacticallyInvisible", plugin);

				} else if (universe.getPillagingPlayers().contains(player)) {
					//Visible on map
					player.removeMetadata("tacticallyInvisible", plugin);

				} else if (player.getInventory().getItemInOffHand().getType() == Material.SHIELD
						|| player.getInventory().getItemInOffHand().getType() == Material.COMPASS) {
					//Invisible on map
					player.setMetadata("tacticallyInvisible", invisibilityMetaDataValue);

				} else if (player.getPotionEffect(PotionEffectType.INVISIBILITY) != null){
					//Invisible on map
					player.setMetadata("tacticallyInvisible", invisibilityMetaDataValue);

				} else {
					//Visible on map
					player.removeMetadata("tacticallyInvisible", plugin);
				}
			}
		} catch (Exception e) {
			System.out.println("Problem evaluating tactical visibility");
			e.printStackTrace();
		}
	}
}
