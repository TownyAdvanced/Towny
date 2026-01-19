package com.palmergames.bukkit.util;

import org.bukkit.Particle;

import com.palmergames.bukkit.towny.utils.MinecraftVersion;

public class BukkitParticle {

	public static Particle getBorderParticle() {
		if (MinecraftVersion.CURRENT_VERSION.isNewerThanOrEquals(MinecraftVersion.MINECRAFT_1_20_5))
			return Particle.DUST;
		else
			return Particle.valueOf("REDSTONE");
	}

	public static Particle getSpawnPointParticle() {
		if (MinecraftVersion.CURRENT_VERSION.isNewerThanOrEquals(MinecraftVersion.MINECRAFT_1_20_5))
			return Particle.ENCHANTED_HIT;
		else
			return Particle.valueOf("CRIT_MAGIC");
	}
}
