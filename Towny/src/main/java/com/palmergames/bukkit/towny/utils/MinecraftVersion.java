package com.palmergames.bukkit.towny.utils;

import com.palmergames.bukkit.util.Version;
import org.bukkit.Bukkit;

public class MinecraftVersion {
	private MinecraftVersion() {}

	public static final Version MINECRAFT_1_16 = Version.fromString("1.16");
	public static final Version MINECRAFT_1_17 = Version.fromString("1.17");
	public static final Version MINECRAFT_1_18_2 = Version.fromString("1.18.2");
	public static final Version MINECRAFT_1_19_1 = Version.fromString("1.19.1");
	public static final Version MINECRAFT_1_19_3 = Version.fromString("1.19.3");
	public static final Version MINECRAFT_1_20 = Version.fromString("1.20");
	public static final Version MINECRAFT_1_20_2 = Version.fromString("1.20.2");
	public static final Version MINECRAFT_1_20_3 = Version.fromString("1.20.3");
	public static final Version MINECRAFT_1_20_5 = Version.fromString("1.20.5");
	public static final Version MINECRAFT_1_21 = Version.fromString("1.21");
	public static final Version MINECRAFT_1_21_2 = Version.fromString("1.21.2");
	public static final Version MINECRAFT_1_21_3 = Version.fromString("1.21.3");
	public static final Version MINECRAFT_1_21_5 = Version.fromString("1.21.5");
	
	public static final Version CURRENT_VERSION = Version.fromString(Bukkit.getBukkitVersion());
	public static final Version OLDEST_VERSION_SUPPORTED = MINECRAFT_1_19_1;
}
