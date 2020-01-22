package com.palmergames.bukkit.towny.object;

import org.bukkit.World;

public interface Economical {

	/**
	 * The name of the economy account to be used with this object.
	 * 
	 * @return A string representing the account name.
	 */
	String getEconomyName();
	
	World getBukkitWorld();
}
