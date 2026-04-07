package com.palmergames.bukkit.towny.test;

import io.papermc.paper.registry.RegistryAccess;
import org.bukkit.Bukkit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(TownyConfigExtension.class)
public class ExtensionMetaTests {
	@Test
	void testRegistryAccessMock_isAMock() {
		assertTrue(Mockito.mockingDetails(RegistryAccess.registryAccess()).isMock());
	}

	@Test
	void testBukkitSingleton_isNonNull() {
		assertNotNull(Bukkit.getServer());
	}
}
