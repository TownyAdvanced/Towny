package com.palmergames.bukkit.towny;

import be.seeseemelk.mockbukkit.MockBukkit;
import be.seeseemelk.mockbukkit.ServerMock;
import be.seeseemelk.mockbukkit.WorldMock;
import be.seeseemelk.mockbukkit.entity.PlayerMock;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.tasks.OnPlayerLogin;
import net.milkbowl.vault.economy.Economy;
import net.milkbowl.vault.Vault;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.plugin.ServicePriority;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class TownTests {

	static ServerMock server;
	static Object vault;
	static Towny towny;
	static WorldMock world;


	@BeforeAll
	public static void beforeAll() {
		server = MockBukkit.mock();
		vault = MockBukkit.load(Vault.class);
		Bukkit.getServicesManager().register(Economy.class, new MockEconomy(), towny, ServicePriority.Highest);
		towny = MockBukkit.load(Towny.class);
		world = server.addSimpleWorld("test");
		TownyUniverse.getInstance().newWorld(world);
	}
	
	@Test
	public void createTown() {
		PlayerMock testPlayer = server.addPlayer();
		testPlayer.setLocation(new Location(world, 1, 1, 1));
		testPlayer.setOp(true);
		new OnPlayerLogin(towny, testPlayer).run();

		TownyUniverse.getInstance().getResident(testPlayer.getUniqueId()).getAccount().setBalance(1000, "test");
		Bukkit.dispatchCommand(testPlayer, "towny:town new test");
		Bukkit.dispatchCommand(testPlayer, "towny:confirm");
		server.getScheduler().performTicks(1);
		Assertions.assertTrue(TownyUniverse.getInstance().getResident(testPlayer.getUniqueId()).hasTown());
	}
}
