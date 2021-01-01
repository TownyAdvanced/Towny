package com.palmergames.bukkit.towny.war.siegewar;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.war.siegewar.objects.HeldItemsCombination;

public class SiegeWarSettings {
	
	private static final List<HeldItemsCombination> tacticalVisibilityItems = new ArrayList<>();
	private static List<Material> battleSessionsForbiddenBlockMaterials = null;
	private static List<Material> battleSessionsForbiddenBucketMaterials = null;
	
	public static boolean getWarSiegeEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_ENABLED);
	}

	public static String getWarSiegeWorlds() {
		return TownySettings.getString(ConfigNodes.WAR_SIEGE_WORLDS);
	}

	public static String getWarWorldsWithUndergroundBannerControl() {
		return TownySettings.getString(ConfigNodes.WAR_SIEGE_WORLDS_WITH_UNDERGROUND_BANNER_CONTROL);
	}

	public static boolean getWarSiegeAttackEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_ATTACK_ENABLED);
	}

	public static boolean getWarSiegeAbandonEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_ABANDON_ENABLED);
	}

	public static boolean getWarSiegeSurrenderEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_TOWN_SURRENDER_ENABLED);
	}

	public static boolean getWarSiegeInvadeEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_INVADE_ENABLED);
	}

	public static boolean getWarSiegePlunderEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_PLUNDER_ENABLED);
	}

	public static boolean getWarSiegeRevoltEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_REVOLT_ENABLED);
	}

	public static boolean getWarSiegeTownLeaveDisabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_TOWN_LEAVE_DISABLED);
	}

	public static boolean getWarSiegePvpAlwaysOnInBesiegedTowns() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_PVP_ALWAYS_ON_IN_BESIEGED_TOWNS);
	}

	public static boolean getWarSiegeExplosionsAlwaysOnInBesiegedTowns() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_EXPLOSIONS_ALWAYS_ON_IN_BESIEGED_TOWNS);
	}

	public static boolean getWarSiegeClaimingDisabledNearSiegeZones() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_CLAIMING_DISABLED_NEAR_SIEGE_ZONES);
	}

	public static int getWarSiegeMaxAllowedBannerToTownDownwardElevationDifference() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_MAX_ALLOWED_BANNER_TO_TOWN_DOWNWARD_ELEVATION_DIFFERENCE);
	}

	public static double getWarSiegeAttackerCostUpFrontPerPlot() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_ATTACKER_COST_UPFRONT_PER_PLOT);
	}

	public static double getWarSiegeSiegeImmunityTimeNewTownsHours() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_SIEGE_IMMUNITY_TIME_NEW_TOWN_HOURS);
	}

	public static double getWarSiegeSiegeImmunityTimeModifier() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_SIEGE_IMMUNITY_TIME_MODIFIER);
	}

	public static double getWarSiegeRevoltImmunityTimeHours() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_REVOLT_IMMUNITY_TIME_HOURS);
	}

	public static double getWarSiegeAttackerPlunderAmountPerPlot() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_ATTACKER_PLUNDER_AMOUNT_PER_PLOT);
	}

	public static double getWarSiegeMaxHoldoutTimeHours() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_MAX_HOLDOUT_TIME_HOURS);
	}
	
	public static double getWarSiegeMinSiegeDurationBeforeSurrenderHours() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_MIN_SIEGE_DURATION_BEFORE_SURRENDER_HOURS);
	}

	public static double getWarSiegeMinSiegeDurationBeforeAbandonHours() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_MIN_SIEGE_DURATION_BEFORE_ABANDON_HOURS);
	}

	public static int getWarSiegePointsForAttackerOccupation() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_ATTACKER_OCCUPATION);
	}

	public static int getWarSiegePointsForDefenderOccupation() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_DEFENDER_OCCUPATION);
	}

	public static int getWarSiegePointsForAttackerDeath() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_ATTACKER_DEATH);
	}

	public static int getWarSiegePointsForDefenderDeath() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_POINTS_FOR_DEFENDER_DEATH);
	}
	
	public static int getWarSiegeZoneRadiusBlocks() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_ZONE_RADIUS_BLOCKS);
	}

	public static boolean getWarSiegeNonResidentSpawnIntoSiegeZonesOrBesiegedTownsDisabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_NON_RESIDENT_SPAWN_INTO_SIEGE_ZONES_OR_BESIEGED_TOWNS_DISABLED);
	}

	public static double getWarSiegeNationCostRefundPercentageOnDelete() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_NATION_COST_REFUND_PERCENTAGE_ON_DELETE);
	}

	public static int getWarSiegeMaxActiveSiegeAttacksPerNation() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_MAX_ACTIVE_SIEGE_ATTACKS_PER_NATION);
	}

	public static boolean getWarSiegeRefundInitialNationCostOnDelete() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_REFUND_INITIAL_NATION_COST_ON_DELETE);
	}

	public static boolean getWarCommonPeacefulTownsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_COMMON_PEACEFUL_TOWNS_ENABLED);
	}

	public static int getWarCommonPeacefulTownsConfirmationRequirementDays() {
		return TownySettings.getInt(ConfigNodes.WAR_COMMON_PEACEFUL_TOWNS_CONFIRMATION_REQUIREMENT_DAYS);
	}

	public static boolean getWarSiegeBesiegedTownRecruitmentDisabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_BESIEGED_TOWN_RECRUITMENT_DISABLED);
	}

	public static boolean getWarSiegeBesiegedTownClaimingDisabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_BESIEGED_TOWN_CLAIMING_DISABLED);
	}

	public static boolean getWarSiegeBesiegedTownUnClaimingDisabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_BESIEGED_TOWN_UNCLAIMING_DISABLED);
	}

	public static boolean getWarSiegeDeathPenaltyKeepInventoryEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_DEATH_PENALTY_KEEP_INVENTORY_ENABLED);
	}

	public static boolean getWarSiegeDeathPenaltyDegradeInventoryEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_DEATH_PENALTY_DEGRADE_INVENTORY_ENABLED);
	}

	public static double getWarSiegeDeathPenaltyDegradeInventoryPercentage() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_DEATH_PENALTY_DEGRADE_INVENTORY_PERCENTAGE);
	}

	public static int getWarSiegeExtraMoneyPercentagePerTownLevel() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_EXTRA_MONEY_PERCENTAGE_PER_TOWN_LEVEL);
	}

	public static double getWarSiegePointsPercentageAdjustmentForLeaderProximity() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_POINTS_PERCENTAGE_ADJUSTMENT_FOR_LEADER_PROXIMITY);
	}

	public static double getWarSiegePointsPercentageAdjustmentForLeaderDeath() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_POINTS_PERCENTAGE_ADJUSTMENT_FOR_LEADER_DEATH);
	}

	public static int getWarSiegeLeadershipAuraRadiusBlocks() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_LEADERSHIP_AURA_RADIUS_BLOCKS);
	}

	public static boolean getWarSiegeTacticalVisibilityEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_TACTICAL_VISIBILITY_ENABLED);
	}

	public static List<HeldItemsCombination> getWarSiegeTacticalVisibilityItems() {
		try {
			if (tacticalVisibilityItems.isEmpty()) {
				String itemsListAsString = TownySettings.getString(ConfigNodes.WAR_SIEGE_TACTICAL_VISIBILITY_ITEMS);
				String[] itemsListAsArray = itemsListAsString.split(",");
				String[] itemPair;
				boolean ignoreOffHand;
				boolean ignoreMainHand;
				Material offHandItem;
				Material mainHandItem;

				for (String itemAsString : itemsListAsArray) {
					itemPair = itemAsString.trim().split("\\|");

					if(itemPair[0].equalsIgnoreCase("any")) {
						ignoreOffHand = true;
						offHandItem = null;
					} else if (itemPair[0].equalsIgnoreCase("empty")){
						ignoreOffHand = false;
						offHandItem = Material.AIR;
					} else{
						ignoreOffHand = false;
						offHandItem = Material.matchMaterial(itemPair[0]);
					}

					if(itemPair[1].equalsIgnoreCase("any")) {
						ignoreMainHand = true;
						mainHandItem = null;
					} else if (itemPair[1].equalsIgnoreCase("empty")){
						ignoreMainHand = false;
						mainHandItem = Material.AIR;
					} else{
						ignoreMainHand = false;
						mainHandItem = Material.matchMaterial(itemPair[1]);
					}

					tacticalVisibilityItems.add(
						new HeldItemsCombination(offHandItem,mainHandItem,ignoreOffHand,ignoreMainHand));
				}
			}
		} catch (Exception e) {
			System.out.println("Problem reading tactical visibility items list. The list is config.yml may be misconfigured.");
			e.printStackTrace();
		}
		return tacticalVisibilityItems;
	}

	public static int getWarSiegeBannerControlSessionDurationMinutes() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_BANNER_CONTROL_SESSION_DURATION_MINUTES);
	}

	public static boolean getWarSiegePopulationBasedPointBoostsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_POPULATION_BASED_POINT_BOOSTS_ENABLED);
	}

	public static double getWarSiegePopulationQuotientForMaxPointsBoost() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_POPULATION_QUOTIENT_FOR_MAX_POINTS_BOOST);
	}

	public static double getWarSiegeMaxPopulationBasedPointBoost() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_MAX_POPULATION_BASED_POINTS_BOOST);
	}

	public static boolean getWarCommonOccupiedTownUnClaimingDisabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_COMMON_OCCUPIED_TOWN_UNCLAIMING_DISABLED);
	}

	public static boolean isWarSiegeCounterattackBoosterEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_COUNTERATTACK_BOOSTER_ENABLED);
	}

	public static double getWarSiegeCounterattackBoosterExtraDeathPointsPerPlayerPercent() {
		return TownySettings.getDouble(ConfigNodes.WAR_SIEGE_COUNTERATTACK_BOOSTER_EXTRA_DEATH_POINTS_PER_PLAYER_PERCENT);
	}

	public static boolean isWarSiegeBattleSessionsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_BATTLE_SESSIONS_ENABLED);
	}

	public static int getWarSiegeBattleSessionsActivePhaseDurationMinutes() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_BATTLE_SESSIONS_ACTIVE_PHASE_DURATION_MINUTES);
	}

	public static int getWarSiegeBattleSessionsFirstWarningMinutesToExpiry() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_BATTLE_SESSIONS_FIRST_WARNING_MINUTES_TO_EXPIRY);
	}

	public static int getWarSiegeBattleSessionsSecondWarningMinutesToExpiry() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_BATTLE_SESSIONS_SECOND_WARNING_MINUTES_TO_EXPIRY);
	}

	public static int getWarSiegeBattleSessionsExpiredPhaseDurationMinutes() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_BATTLE_SESSIONS_EXPIRED_PHASE_DURATION_MINUTES);
	}

	public static boolean isWarSiegeZoneBlockPlacementRestrictionsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_ZONE_BLOCK_PLACEMENT_RESTRICTIONS_ENABLED);
	}

	public static List<Material> getWarSiegeZoneBlockPlacementRestrictionsMaterials() {
		if(battleSessionsForbiddenBlockMaterials == null) {
			battleSessionsForbiddenBlockMaterials = new ArrayList<>();
			String listAsString = TownySettings.getString(ConfigNodes.WAR_SIEGE_ZONE_BLOCK_PLACEMENT_RESTRICTIONS_MATERIALS);
			String[] listAsStringArray = listAsString.split(",");
			for (String blockTypeAsString : listAsStringArray) {
				Material material = Material.matchMaterial(blockTypeAsString.trim());
				battleSessionsForbiddenBlockMaterials.add(material);
			}
		}
		return battleSessionsForbiddenBlockMaterials;
	}

	public static boolean isWarSiegeZoneBucketEmptyingRestrictionsEnabled() {
		return TownySettings.getBoolean(ConfigNodes.WAR_SIEGE_ZONE_BUCKET_EMPTYING_RESTRICTIONS_ENABLED);
	}

	public static List<Material> getWarSiegeZoneBucketEmptyingRestrictionsMaterials() {
		if(battleSessionsForbiddenBucketMaterials == null) {
			battleSessionsForbiddenBucketMaterials = new ArrayList<>();
			String listAsString = TownySettings.getString(ConfigNodes.WAR_SIEGE_ZONE_BUCKET_EMPTYING_RESTRICTIONS_MATERIALS);
			String[] listAsStringArray = listAsString.split(",");
			for (String blockTypeAsString : listAsStringArray) {
				Material material = Material.matchMaterial(blockTypeAsString.trim());
				battleSessionsForbiddenBucketMaterials.add(material);
			}
		}
		return battleSessionsForbiddenBucketMaterials;
	}

	public static int getWarSiegePeacefulTownsGuardianTownPlotsRequirement() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_PEACEFUL_TOWNS_GUARDIAN_TOWN_PLOTS_REQUIREMENT);
	}

	public static int getWarSiegePeacefulTownsGuardianTownMinDistanceRequirement() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_PEACEFUL_TOWNS_GUARDIAN_TOWN_MIN_DISTANCE_REQUIREMENT_TOWNBLOCKS);
	}

	public static boolean getWarCommonPeacefulTownsAllowedToMakeNation() {
		return TownySettings.getBoolean(ConfigNodes.WAR_COMMON_PEACEFUL_TOWNS_ALLOWED_TO_MAKE_NATION);
	}

	public static int getWarCommonPeacefulTownsNewTownConfirmationRequirementDays() {
		return TownySettings.getInt(ConfigNodes.WAR_COMMON_PEACEFUL_TOWNS_NEW_TOWN_CONFIRMATION_REQUIREMENT_DAYS);
	}

	public static int getBannerControlHorizontalDistanceBlocks() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_BANNER_CONTROL_HORIZONTAL_DISTANCE_BLOCKS);
	}

	public static int getBannerControlVerticalDistanceBlocks() {
		return TownySettings.getInt(ConfigNodes.WAR_SIEGE_BANNER_CONTROL_VERTICAL_DISTANCE_BLOCKS);
	}


}
