package com.palmergames.bukkit.towny.war.siegewar;

import com.palmergames.bukkit.towny.TownyFormatter;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.war.siegewar.enums.SiegeSide;
import com.palmergames.bukkit.towny.war.siegewar.metadata.TownMetaDataController;
import com.palmergames.bukkit.towny.war.siegewar.objects.Siege;
import com.palmergames.bukkit.towny.war.siegewar.siege.SiegeController;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.util.TimeMgmt;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class SiegeWarFormatter {
    @NotNull
    public static List<String> getStatus(@NotNull Town town) {
        List<String> out = new ArrayList<>();

        //Revolt Immunity Timer: 71.8 hours
        if (SiegeWarSettings.getWarSiegeRevoltEnabled() && System.currentTimeMillis() < TownMetaDataController.getRevoltImmunityEndTime(town)) {        	
        	String time = TimeMgmt.getFormattedTimeValue(TownMetaDataController.getRevoltImmunityEndTime(town)- System.currentTimeMillis());        	
            out.add(Translation.of("status_town_revolt_immunity_timer", time));
        }

        if (SiegeController.hasSiege(town)) {
            Siege siege = SiegeController.getSiege(town);
            String time = TimeMgmt.getFormattedTimeValue(TownMetaDataController.getRevoltImmunityEndTime(town)- System.currentTimeMillis());
            switch (siege.getStatus()) {
                case IN_PROGRESS:
                    //Siege:
                    String siegeStatus = Translation.of("status_town_siege_status", getStatusTownSiegeSummary(siege));
                    out.add(siegeStatus);

                    // > Banner XYZ: {2223,82,9877}
                    out.add(
                        String.format(
                            Translation.of("status_town_siege_status_banner_xyz"),
                            siege.getFlagLocation().getBlockX(),
                            siege.getFlagLocation().getBlockY(),
                            siege.getFlagLocation().getBlockZ())
                    );

                    // > Attacker: Land of Empire (Nation) {+30}
                    int pointsInt = siege.getSiegePoints();
                    String pointsString = pointsInt > 0 ? "+" + pointsInt : "" + pointsInt;
                    out.add(Translation.of("status_town_siege_status_besieger", siege.getAttackingNation().getFormattedName(), pointsString));

                    // >  Victory Timer: 5.3 hours
                    String victoryTimer = Translation.of("status_town_siege_victory_timer", siege.getFormattedHoursUntilScheduledCompletion());
                    out.add(victoryTimer);

                    // > Banner Control: Attackers [4] Killbot401x, NerfeyMcNerferson, WarCriminal80372
                    if (siege.getBannerControllingSide() == SiegeSide.NOBODY) {
                        out.add(Translation.of("status_town_banner_control_nobody", siege.getBannerControllingSide().getFormattedName()));
                    } else {
                        String[] bannerControllingResidents = TownyFormatter.getFormattedNames(siege.getBannerControllingResidents());
                        if (bannerControllingResidents.length > 34) {
                            String[] entire = bannerControllingResidents;
                            bannerControllingResidents = new String[36];
                            System.arraycopy(entire, 0, bannerControllingResidents, 0, 35);
                            bannerControllingResidents[35] = Translation.of("status_town_reslist_overlength");
                        }
                        out.addAll(ChatTools.listArr(bannerControllingResidents, Translation.of("status_town_banner_control", siege.getBannerControllingSide().getFormattedName(), siege.getBannerControllingResidents().size())));
                    }
                    break;

                    
                case ATTACKER_WIN:
                case DEFENDER_SURRENDER:
                    siegeStatus = Translation.of("status_town_siege_status", getStatusTownSiegeSummary(siege));
                    String invadedYesNo = siege.isTownInvaded() ? Translation.of("status_yes") : Translation.of("status_no_green");
                    String plunderedYesNo = siege.isTownPlundered() ? Translation.of("status_yes") : Translation.of("status_no_green");
                    String invadedPlunderedStatus = Translation.of("status_town_siege_invaded_plundered_status", invadedYesNo, plunderedYesNo);
                    String siegeImmunityTimer = Translation.of("status_town_siege_immunity_timer", time);
                    out.add(siegeStatus);
                    out.add(invadedPlunderedStatus);
                    out.add(siegeImmunityTimer);
                    break;

                case DEFENDER_WIN:
                case ATTACKER_ABANDON:
                    siegeStatus = Translation.of("status_town_siege_status", getStatusTownSiegeSummary(siege));
                    siegeImmunityTimer = Translation.of("status_town_siege_immunity_timer", time);
                    out.add(siegeStatus);
                    out.add(siegeImmunityTimer);
                    break;

                case PENDING_DEFENDER_SURRENDER:
                case PENDING_ATTACKER_ABANDON:
                    siegeStatus = Translation.of("status_town_siege_status", getStatusTownSiegeSummary(siege));
                    out.add(siegeStatus);
                    break;
            }
        } else {
            if (SiegeWarSettings.getWarSiegeAttackEnabled() 
            	&& !(SiegeController.hasActiveSiege(town))
            	&& System.currentTimeMillis() < TownMetaDataController.getSiegeImmunityEndTime(town)) {
                //Siege:
                // > Immunity Timer: 40.8 hours
                out.add(Translation.of("status_town_siege_status", ""));
                String time = TimeMgmt.getFormattedTimeValue(TownMetaDataController.getRevoltImmunityEndTime(town)- System.currentTimeMillis()); 
                out.add(Translation.of("status_town_siege_immunity_timer", time));
            }
        }
        return out;
    }


    /**
     * Gets the status screen of a Nation
     *
     * @param nation the nation to check against
     * @return a string list containing the results.
     */
    @NotNull
    public static List<String> getStatus(@NotNull Nation nation) {

        // Siege Attacks [3]: TownA, TownB, TownC
        List<Town> siegeAttacks = nation.getTownsUnderSiegeAttack();
        String[] formattedSiegeAttacks = TownyFormatter.getFormattedNames(siegeAttacks.toArray(new Town[0]));
        List<String> out = new ArrayList<>(ChatTools.listArr(formattedSiegeAttacks, Translation.of("status_nation_siege_attacks", siegeAttacks.size())));

        // Siege Defences [3]: TownX, TownY, TownZ
        List<Town> siegeDefences = nation.getTownsUnderSiegeDefence();
        String[] formattedSiegeDefences = TownyFormatter.getFormattedNames(siegeDefences.toArray(new Town[0]));
        out.addAll(ChatTools.listArr(formattedSiegeDefences, Translation.of("status_nation_siege_defences", siegeDefences.size())));

        return out;
    }

    private static String getStatusTownSiegeSummary(@NotNull Siege siege) {
        switch (siege.getStatus()) {
            case IN_PROGRESS:
                return Translation.of("status_town_siege_status_in_progress");
            case ATTACKER_WIN:
                return Translation.of("status_town_siege_status_attacker_win", siege.getAttackingNation().getFormattedName());
            case DEFENDER_SURRENDER:
                return Translation.of("status_town_siege_status_defender_surrender", siege.getAttackingNation().getFormattedName());
            case DEFENDER_WIN:
                return Translation.of("status_town_siege_status_defender_win");
            case ATTACKER_ABANDON:
                return Translation.of("status_town_siege_status_attacker_abandon");
            case PENDING_DEFENDER_SURRENDER:
                return Translation.of("status_town_siege_status_pending_defender_surrender", siege.getFormattedTimeUntilDefenderSurrender());
            case PENDING_ATTACKER_ABANDON:
                return Translation.of("status_town_siege_status_pending_attacker_abandon", siege.getFormattedTimeUntilAttackerAbandon());
            default:
                return "???";
        }
    }
}
