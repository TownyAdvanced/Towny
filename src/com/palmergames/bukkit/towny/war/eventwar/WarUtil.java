package com.palmergames.bukkit.towny.war.eventwar;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.entity.Firework;
import org.bukkit.entity.Player;
import org.bukkit.inventory.meta.FireworkMeta;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.confirmations.Confirmation;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.bukkit.towny.war.eventwar.settings.EventWarSettings;
import com.palmergames.bukkit.util.BukkitTools;

public class WarUtil {

	/**
	 * Allows War Event to handle editable materials, while accounting for neutral
	 * or jailed players.
	 * 
	 * @param player - Player who is being tested for neutrality.
	 * @return Whether a player is considered neutral.
	 */
	public static boolean isPlayerNeutral(Player player) {
		if (TownyAPI.getInstance().isWarTime()) {
			Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
			if (resident != null) {
				if (resident.isJailed())
					return true;
				if (TownyUniverse.getInstance().hasWarEvent(resident))
					return false;
			}
		}
		return true;
	}

	/**
	 * Launch a {@link Firework} at a given plot
	 * @param townblock - The {@link TownBlock} to fire in
	 * @param atPlayer - The {@link Player} in which the location is grabbed
	 * @param type - The {@link FireworkEffect} type
	 * @param c - The Firework {@link Color}
	 */
	public static void launchFireworkAtPlot(final TownBlock townblock, final Player atPlayer, final FireworkEffect.Type type, final Color c) {
		// Check the config. If false, do not launch a firework.
		if (!EventWarSettings.getPlotsFireworkOnAttacked()) {
			return;
		}
		
		BukkitTools.scheduleSyncDelayedTask(() -> {
			double x = (double)townblock.getX() * Coord.getCellSize() + Coord.getCellSize()/2.0;
			double z = (double)townblock.getZ() * Coord.getCellSize() + Coord.getCellSize()/2.0;
			double y = atPlayer.getLocation().getY() + 20;
			Firework firework = atPlayer.getWorld().spawn(new Location(atPlayer.getWorld(), x, y, z), Firework.class);
			FireworkMeta data = firework.getFireworkMeta();
			data.addEffects(FireworkEffect.builder().withColor(c).with(type).trail(false).build());
			firework.setFireworkMeta(data);
			firework.detonate();
		}, 0);
	}

	public static boolean sameWar(War war1, War war2) {
		if (war1 == null || war2 == null)
			return false;
		return war1.getWarUUID().equals(war2.getWarUUID());
	}
	public static boolean hasSameWar(Town town1, Town town2) {
		War war1 = TownyUniverse.getInstance().getWarEvent(town1);
		War war2 = TownyUniverse.getInstance().getWarEvent(town2);
		return sameWar(war1, war2);
	}
	
	public static boolean hasSameWar(Resident res1, Resident res2) {
		War war1 = TownyUniverse.getInstance().getWarEvent(res1);
		War war2 = TownyUniverse.getInstance().getWarEvent(res2);
		return sameWar(war1, war2);
	}
	
	public static boolean hasSameWar(Resident res, Town town) {
		War war1 = TownyUniverse.getInstance().getWarEvent(res);
		War war2 = TownyUniverse.getInstance().getWarEvent(town);
		return sameWar(war1, war2);
	}
	
	public static boolean hasSameWar(Resident res, TownBlock tb) {
		War war1 = TownyUniverse.getInstance().getWarEvent(res);
		War war2 = TownyUniverse.getInstance().getWarEvent(tb);
		return sameWar(war1, war2);
	}
	
	public static boolean hasWorldWar(TownyWorld world) {
		if (!world.isWarAllowed())
			return false;
		return TownyUniverse.getInstance().getWars().stream().anyMatch(war -> war.getWarType().equals(WarType.WORLDWAR));
	}
	
	public static void confirmPlayerSide(War war, Player player) {
		Confirmation.runOnAccept(() -> {
			switch (war.getWarType()) {
				case RIOT: 
					war.getWarParticipants().addGovSide(TownyAPI.getInstance().getResident(player.getUniqueId()));
					break;
				case CIVILWAR:
					war.getWarParticipants().addGovSide(TownyAPI.getInstance().getResident(player.getUniqueId()).getTownOrNull());
					break;
				case NATIONWAR:
				case TOWNWAR:
				case WORLDWAR:
				default:
					break;
			}
			TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_sided_with_the_government"));
			
		}).runOnCancel(() -> {
			switch (war.getWarType()) {
				case RIOT: 
					war.getWarParticipants().addRebSide(TownyAPI.getInstance().getResident(player.getUniqueId()));
					break;
				case CIVILWAR:
					war.getWarParticipants().addRebSide(TownyAPI.getInstance().getResident(player.getUniqueId()).getTownOrNull());
					break;
				case NATIONWAR:
				case TOWNWAR:
				case WORLDWAR:
				default:
					break;
			}
			TownyMessaging.sendMsg(player, Translatable.of("msg_you_have_sided_with_the_rebels"));
		})
		.setTitle(Translatable.of("msg_which_side_will_you_choose"))
		.setDuration(EventWarSettings.teamSelectionSeconds())
		.setConfirmText("/state")
		.setCancelText("/rebel")
		.sendTo(player);
	}
	
	public static boolean eligibleForWar(WarType type, TownyObject obj) {
		return !WarMetaDataController.hasLastWarTime(obj) || !tooSoonForWar(type, obj);
	}

	private static boolean tooSoonForWar(WarType type, TownyObject obj) {
		return (WarMetaDataController.getLastWarTime(obj) + type.cooldown) > System.currentTimeMillis();
	}
}
