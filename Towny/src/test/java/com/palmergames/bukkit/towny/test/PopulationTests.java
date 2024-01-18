package com.palmergames.bukkit.towny.test;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

import be.seeseemelk.mockbukkit.MockBukkit;

public class PopulationTests {

	static Town town;
	static Nation nation;

	@BeforeAll
	static void init() {
		MockBukkit.getOrCreateMock();
		TownySettings.loadDefaultConfig();
		nation = new Nation("testNation");
		town = new Town("testTown");
		Resident res1 = new Resident("leonardo");
		Resident res2 = new Resident("donatello");
		Resident res3 = new Resident("raphael");
		Resident res4 = new Resident("michelangelo");
		Resident res5 = new Resident("splinter");
		town.addResident(res1);
		town.addResident(res2);
		town.addResident(res3);
		town.addResident(res4);
		town.addResident(res5);
	}

	@BeforeEach
	void reset() {
		town.removeNation();
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 0);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_CAPITAL_OVERRIDE.getRoot(), 0);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_MAX_NUMBER_RESIDENTS_WITHOUT_NATION.getRoot(), 0);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_CREATE_NATION.getRoot(), 0);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_JOIN_NATION.getRoot(), 0);
		TownySettings.getConfig().set(ConfigNodes.GNATION_SETTINGS_MAX_RESIDENTS_PER_NATION.getRoot(), 0);
	}

	/*
	 * Basic population test where no restrictions apply.
	 */

	@Test
	void testSuccessAddResidentNoRestrictionsWithoutCapital() {
		assertEquals(town.isAllowedThisAmountOfResidents(10, false), true);
	}

	@Test
	void testSuccessAddResidentNoRestrictionsWithCapital() {
		assertEquals(town.isAllowedThisAmountOfResidents(10, true), true);
	}

	/*
	 * Test restricted town population without capital.
	 */

	@Test
	void testSuccessAllowTownPopulationWithRestrictionsWithoutCapital() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 5);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_CAPITAL_OVERRIDE.getRoot(), 10);
		assertEquals(town.isAllowedThisAmountOfResidents(5, false), true);
	}

	@Test
	void testFailAllowTownPopulationWithRestrictionsWithoutCapital() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 5);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_CAPITAL_OVERRIDE.getRoot(), 10);
		assertEquals(town.isAllowedThisAmountOfResidents(10, false), false);
	}

	/*
	 * Test restricted town population with capital.
	 */

	@Test
	void testSuccessAllowTownPopulationWithRestrictionsWithCapital() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 5);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_CAPITAL_OVERRIDE.getRoot(), 10);
		assertEquals(town.isAllowedThisAmountOfResidents(10, true), true);
	}

	@Test
	void testFailureAllowTownPopulationWithRestrictionsWithCapital() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 5);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_CAPITAL_OVERRIDE.getRoot(), 10);
		assertEquals(town.isAllowedThisAmountOfResidents(11, true), false);
	}

	/*
	 * Test nationless towns' population restriction.
	 */

	@Test
	void testSuccessAllowNationlessTownPopulationWithFeatureDisabled() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 10);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_MAX_NUMBER_RESIDENTS_WITHOUT_NATION.getRoot(), 0);
		assertEquals(town.isAllowedThisAmountOfResidents(5, false), true);
	}

	@Test
	void testSuccessAllowNationlessTownPopulation() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 10);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_MAX_NUMBER_RESIDENTS_WITHOUT_NATION.getRoot(), 5);
		assertEquals(town.isAllowedThisAmountOfResidents(5, false), true);
	}

	@Test
	void testFailureAllowNationlessTownPopulation() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_MAX_RESIDENTS_PER_TOWN.getRoot(), 10);
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_MAX_NUMBER_RESIDENTS_WITHOUT_NATION.getRoot(), 5);
		assertEquals(town.isAllowedThisAmountOfResidents(6, false), false);
	}

	/*
	 * Test capital population requirements.
	 */

	@Test
	void testSuccessTownMakingNationWithFeatureDisabled() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_CREATE_NATION.getRoot(), 0);
		assertEquals(town.hasEnoughResidentsToBeANationCapital(), true);
	}

	@Test
	void testSucceedTownPopAllowsBeingCapital() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_CREATE_NATION.getRoot(), 5);
		assertEquals(town.hasEnoughResidentsToBeANationCapital(), true);
	}

	@Test
	void testFailTownPopAllowsBeingCapital() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_CREATE_NATION.getRoot(), 6);
		assertEquals(town.hasEnoughResidentsToBeANationCapital(), false);
	}

	/*
	 * Test nation-joining population requirements.
	 */

	@Test
	void testSuccessTownJoiningNationWithFeatureDisabled() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_JOIN_NATION.getRoot(), 0);
		assertEquals(town.hasEnoughResidentsToBeANationCapital(), true);
	}

	@Test
	void testSucceedTownPopAllowsJoiningNation() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_JOIN_NATION.getRoot(), 5);
		assertEquals(town.hasEnoughResidentsToJoinANation(), true);
	}

	@Test
	void testFailTownPopAllowsJoiningNation() {
		TownySettings.getConfig().set(ConfigNodes.GTOWN_SETTINGS_REQUIRED_NUMBER_RESIDENTS_JOIN_NATION.getRoot(), 6);
		assertEquals(town.hasEnoughResidentsToJoinANation(), false);
	}

	/*
	 * Test max residents per nation.
	 */

	@Test
	void testSuccessNationAddingResidentsWithFeatureDisabled() {
		TownySettings.getConfig().set(ConfigNodes.GNATION_SETTINGS_MAX_RESIDENTS_PER_NATION.getRoot(), 0);
		nation.addTown(town);
		assertEquals(nation.canAddResidents(1), true);
	}

	@Test
	void testSuccessNationAddingResidents() {
		TownySettings.getConfig().set(ConfigNodes.GNATION_SETTINGS_MAX_RESIDENTS_PER_NATION.getRoot(), 7);
		nation.addTown(town);
		assertEquals(nation.canAddResidents(1), true);
	}

	@Test
	void testFailureNationAddingResidents() {
		TownySettings.getConfig().set(ConfigNodes.GNATION_SETTINGS_MAX_RESIDENTS_PER_NATION.getRoot(), 5);
		nation.addTown(town);
		assertEquals(nation.canAddResidents(1), false);
	}
}

