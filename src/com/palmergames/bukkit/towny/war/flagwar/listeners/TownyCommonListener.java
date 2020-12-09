package com.palmergames.bukkit.towny.war.flagwar.listeners;

import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.town.TownPreUnclaimCmdEvent;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.flagwar.FlagWar;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

/**
 * Listener for processing events from Towny's Common War Events.
 */
public class TownyCommonListener implements Listener {

  @EventHandler (priority= EventPriority.HIGH)
  private void onWarPreUnclaim(TownPreUnclaimCmdEvent event) {
    if (FlagWar.isUnderAttack(event.getTown()) && TownySettings.isFlaggedInteractionTown()) {
      event.setCancelMessage(Translation.of("msg_war_flag_deny_town_under_attack"));
      event.setCancelled(true);
      return; // Return early, no reason to try sequential checks if a town is under attack.
    }

    if (System.currentTimeMillis() - FlagWar.lastFlagged(event.getTown()) < TownySettings.timeToWaitAfterFlag()) {
      event.setCancelMessage(Translation.of("msg_war_flag_deny_recently_attacked"));
      event.setCancelled(true);
    }
  }

}
