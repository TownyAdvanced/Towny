package com.palmergames.bukkit.towny.test;

import com.palmergames.bukkit.towny.TownySettings;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * An extension of the base bukkit mock extension that also loads the Towny config
 */
public class TownyConfigExtension extends BukkitMockExtension {
	@Override
	public void beforeAll(ExtensionContext context) {
		super.beforeAll(context);
		TownySettings.loadDefaultConfig();
	}
}
