package com.palmergames.bukkit.towny.war.eventwar.listeners;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.TownyLoadedDatabaseEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyBurnEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyExplodingBlocksEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;
import com.palmergames.bukkit.towny.event.nation.toggle.NationToggleNeutralEvent;
import com.palmergames.bukkit.towny.event.statusscreen.ResidentStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownBlockStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.event.damage.TownBlockPVPTestEvent;
import com.palmergames.bukkit.towny.event.damage.TownyExplosionDamagesEntityEvent;
import com.palmergames.bukkit.towny.event.damage.TownyPlayerDamagePlayerEvent;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.PlayerCache.TownBlockStatus;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.regen.TownyRegenAPI;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarDataBase;
import com.palmergames.bukkit.towny.war.eventwar.WarMetaDataController;
import com.palmergames.bukkit.towny.war.eventwar.WarUtil;
import com.palmergames.bukkit.towny.war.eventwar.WarZoneConfig;
import com.palmergames.bukkit.towny.war.eventwar.instance.War;
import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;

public class WarZoneListener implements Listener {
	
	private final Towny plugin;
	
	public WarZoneListener(Towny instance) {

		plugin = instance;
	}
	
    @EventHandler
    public void onTownyDatabaseLoad(TownyLoadedDatabaseEvent event) {
    	WarDataBase.loadAll();
    }
    
    @EventHandler
    public void onTownStatus(TownStatusScreenEvent event) {
    	Town town = event.getTown();
    	if (!town.hasActiveWar())
	    	return;
    	String meta = WarMetaDataController.getWarUUID(town);
    	if (meta == null)
    		return;
    	UUID warUUID = UUID.fromString(meta);
    	try {
			War war = TownyUniverse.getInstance().getWarEvent(warUUID);
			event.getStatusScreen().addComponentOf("eventwar", Colors.Green + "War: " + Colors.LightGreen + war.getWarName(),
					HoverEvent.showText(Component.text(war.getWarType().name()).append(Component.newline())
							.append(Component.text("Spoils: " + TownyEconomyHandler.getFormattedBalance(war.getWarSpoils())))
							.append(Component.newline())
							.append(Component.text("Delinquents: " + war.getWarParticipants().getTowns()))
							.append(Component.newline())
							.append(Component.text("ID: " + war.getWarUUID()))
							));
		} catch (Exception e) {
			return;
		}
    }
    
    @EventHandler
    public void onTBStatus(TownBlockStatusScreenEvent event) {
    	String meta = WarMetaDataController.getWarUUID(event.getTownBlock());
    	if (meta == null)
    		return;
    	UUID warUUID = UUID.fromString(meta);
    	try {
			War war = TownyUniverse.getInstance().getWarEvent(warUUID);
			event.getStatusScreen().addComponentOf("eventwar", Colors.Green + "War: " + Colors.LightGreen + war.getWarName(),
					HoverEvent.showText(Component.text(war.getWarType().name()).append(Component.newline())
							.append(Component.text("Spoils: " + TownyEconomyHandler.getFormattedBalance(war.getWarSpoils())))
							.append(Component.newline())
							.append(Component.text("Delinquents: " + war.getWarParticipants().getTowns()))
							.append(Component.newline())
							.append(Component.text("ID: " + war.getWarUUID()))
							));
		} catch (Exception e) {
			return;
		}
    }
    
    @EventHandler
    public void onResidentStatus(ResidentStatusScreenEvent event) {
    	String meta = WarMetaDataController.getWarUUID(event.getResident());
    	if (meta == null)
    		return;
    	UUID warUUID = UUID.fromString(meta);
    	try {
			War war = TownyUniverse.getInstance().getWarEvent(warUUID);
			event.getStatusScreen().addComponentOf("eventwar", Colors.Green + "War: " + Colors.LightGreen + war.getWarName(),
					HoverEvent.showText(Component.text(war.getWarType().name()).append(Component.newline())
							.append(Component.text("Spoils: " + TownyEconomyHandler.getFormattedBalance(war.getWarSpoils())))
							.append(Component.newline())
							.append(Component.text("Delinquents: " + war.getWarParticipants().getTowns()))
							.append(Component.newline())
							.append(Component.text("ID: " + war.getWarUUID()))
							));
		} catch (Exception e) {
			return;
		}
    }

	@EventHandler
	public void onDestroy(TownyDestroyEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow build for Event War if material is an EditableMaterial
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			Material mat = event.getMaterial();
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_edit_material", "destroy", mat.toString().toLowerCase()).forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onBuild(TownyBuildEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow destroy for Event War if material is an EditableMaterial
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			Material mat = event.getMaterial();
			if (!WarZoneConfig.isEditableMaterialInWarZone(mat)) {
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_edit_material", "build", mat.toString().toLowerCase()).forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onItemUse(TownyItemuseEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow ItemUse for Event War if configured.
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			if (!WarZoneConfig.isAllowingItemUseInWarZone()) {				
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_use_item").forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onSwitchUse(TownySwitchEvent event) {
		if (!TownyAPI.getInstance().isWarTime() || event.isInWilderness())
			return;
		Player player = event.getPlayer();
		if (!plugin.hasCache(player))
			plugin.newCache(player);
		TownBlockStatus status = plugin.getCache(player).getStatus();

		// Allow Switch for Event War if configured.
		if (status == TownBlockStatus.WARZONE && !WarUtil.isPlayerNeutral(player)) {
			if (!WarZoneConfig.isAllowingSwitchesInWarZone()) {
				event.setCancelled(true);
				event.setMessage(Translatable.of("msg_err_warzone_cannot_use_switches").forLocale(player));
				return;
			}
			event.setCancelled(false);
		}
	}
	
	@EventHandler
	public void onExplosionDamagingBlocks(TownyExplodingBlocksEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;

		List<Block> alreadyAllowed = event.getTownyFilteredBlockList();
		List<Block> toAllow = new ArrayList<Block>();
		
		int count = 0;
		for (Block block : event.getVanillaBlockList()) {
			// Wilderness, skip it.
			if (TownyAPI.getInstance().isWilderness(block))
				continue;
			
			// Non-warzone, skip it.
			if (!TownyUniverse.getInstance().hasWarEvent(TownyAPI.getInstance().getTownBlock(block.getLocation())))
				continue;
			
			// A war that doesn't allow any kind of explosions.
			if (!WarZoneConfig.isAllowingExplosionsInWarZone()) {
				// Remove from the alreadyAllowed list if it exists there.
				if (alreadyAllowed.contains(block))
					alreadyAllowed.remove(block);
				continue;
			}

			// A war that does allow explosions and explosions regenerate.
			if (WarZoneConfig.regenBlocksAfterExplosionInWarZone()) {
				// Skip this block if it is in the ignore list. TODO: with the blockdata nowadays this might not even be necessary.
				if (WarZoneConfig.getExplosionsIgnoreList().contains(block.getType().name()) || WarZoneConfig.getExplosionsIgnoreList().contains(block.getRelative(BlockFace.UP).getType().name())) {
					// Remove from the alreadyAllowed list if it exists there.
					if (alreadyAllowed.contains(block))
						alreadyAllowed.remove(block);
					continue;
				}
				count++;
				TownyRegenAPI.beginProtectionRegenTask(block, count, TownyAPI.getInstance().getTownyWorld(block.getLocation().getWorld().getName()), event);
			}
			// This is an allowed explosion, so add it to our War-allowed list.
			toAllow.add(block);
		}
		// Add all TownyFilteredBlocks to our list, since our list will be used.
		toAllow.addAll(alreadyAllowed);
		
		// Return the list of allowed blocks for this block explosion event.
		event.setBlockList(toAllow);
	}

	@EventHandler
	public void onExplosionDamagingEntity(TownyExplosionDamagesEntityEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		/*
		 * Handle occasions in the wilderness first.
		 */
		if (event.isInWilderness())
			return;

		/*
		 * Must be inside of a town.
		 */
		
		// Not in a war zone, do not modify the outcome of the event.
		if (!TownyUniverse.getInstance().hasWarEvent(TownyAPI.getInstance().getTownBlock(event.getLocation())))
			return;
			
		/*
		 * Stops any type of exploding damage if wars are not allowing explosions.
		 */
		if (!WarZoneConfig.isAllowingExplosionsInWarZone()) {
			event.setCancelled(true);
			return;
		}
		
		/*
		 * Stops explosions from damaging entities protected from explosions during war.
		 */
		if (WarZoneConfig.getExplosionsIgnoreList().contains(event.getEntity().getType().name())) {
			event.setCancelled(true);
			return;
		}

		/*
		 * Explosions must be allowed, so un-cancel the event.
		 */
		event.setCancelled(false);
	}
	
	@EventHandler
	public void onBurn(TownyBurnEvent event) {
		/*
		 * Return early if this is in the wild.
		 */
		if (event.isInWilderness())
			return;

		/*
		 * Event War fire control settings.
		 */
		if (TownyAPI.getInstance().isWarTime() && TownyUniverse.getInstance().hasWarEvent(event.getTownBlock())) {
			if (WarZoneConfig.isAllowingFireInWarZone()) {           // Allow ignition using normal fire-during-war rule.
				event.setCancelled(false);
			} else if (TownySettings.isAllowWarBlockGriefing()) {    // Allow ignition using exceptionally-griefy-war rule for Event War.
				event.setCancelled(false);
			} else {
				event.setCancelled(true);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.LOW, ignoreCancelled = true)
	public void onNationToggleNeutral(NationToggleNeutralEvent event) {
		if (!TownySettings.isDeclaringNeutral() && event.getFutureState()) {
			event.setCancelled(true);
			event.setCancelMessage(Translatable.of("msg_err_fight_like_king").forLocale(event.getPlayer()));
		}

	}
	
	@EventHandler
	public void onPlayerDamagePlayer(TownyPlayerDamagePlayerEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
			
		Town attackerTown = event.getAttackerTown();
		Town defenderTown = event.getVictimTown();
		
		//Cancel because one of two players has no town and should not be interfering during war.
		if (TownySettings.isWarTimeTownsNeutral() && (event.getAttackerTown() == null || event.getVictimTown() == null)){
			event.setMessage(Translatable.of("msg_war_a_player_has_no_town").forLocale(event.getAttackingPlayer()));
			event.setCancelled(true);
			return;
		}

		//Cancel because one of the two players' town has no nation and should not be interfering during war.  AND towns_are_neutral is true in the config.
		if (TownySettings.isWarTimeTownsNeutral() && (!attackerTown.hasNation() || !defenderTown.hasNation())) {
			event.setMessage(Translatable.of("msg_war_a_player_has_no_nation").forLocale(event.getAttackingPlayer()));
			event.setCancelled(true);
			return;
		}
		
		//Cancel because one of the two player's nations is neutral.
		if ((attackerTown.hasNation() && attackerTown.getNationOrNull().isNeutral()) || (defenderTown.hasNation() && defenderTown.getNationOrNull().isNeutral())) {
			event.setMessage(Translatable.of("msg_war_a_player_has_a_neutral_nation").forLocale(event.getAttackingPlayer()));
			event.setCancelled(true);
			return;
		}
		
		//Cancel because one of the two players are no longer involved in the war.
		if (!TownyUniverse.getInstance().hasWarEvent(defenderTown) || !TownyUniverse.getInstance().hasWarEvent(attackerTown)) {
			event.setMessage(Translatable.of("msg_war_a_player_has_been_removed_from_war").forLocale(event.getAttackingPlayer()));
			event.setCancelled(true);
			return;
		}
		
		//Cancel because one of the two players considers the other an ally.
		if (CombatUtil.isAlly(attackerTown, defenderTown)){
			event.setMessage(Translatable.of("msg_war_a_player_is_an_ally").forLocale(event.getAttackingPlayer()));
			event.setCancelled(true);
			return;
		}
	}
	
	@EventHandler
	public void onTownBlockPVPTest(TownBlockPVPTestEvent event) {
		if (!TownyAPI.getInstance().isWarTime())
			return;
		
		if (TownyUniverse.getInstance().hasWarEvent(event.getTownBlock()))
			event.setPvp(true);
	}
}
