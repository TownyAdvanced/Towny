package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.util.Version;
import org.bukkit.Bukkit;

public class MinecraftVersion {
	private MinecraftVersion() {}
	
	public static final Version MINECRAFT_1_16 = Version.fromString("1.16");
	public static final Version MINECRAFT_1_20 = Version.fromString("1.20");
	
	public static final Version CURRENT_VERSION = Version.fromString(Bukkit.getBukkitVersion());
	public static final Version OLDEST_VERSION_SUPPORTED = MINECRAFT_1_16;
}
