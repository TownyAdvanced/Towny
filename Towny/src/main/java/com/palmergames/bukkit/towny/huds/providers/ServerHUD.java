package com.palmergames.bukkit.towny.huds.providers;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.bukkit.entity.Player;

import net.kyori.adventure.text.Component;

public abstract interface ServerHUD {

	boolean toggleOff(Player player);

	boolean toggleOn(Player player);
	
	boolean hasPlayer(Player player);

	boolean addPlayer(Player player);

	boolean removePlayer(Player player);

	Set<Player> getPlayers();

	boolean isActive(Player player);

	void setTitle(UUID uuuid, Component title);

	void setLines(UUID uuuid, List<Component> lines);

	void updateHUD(Player player, Object object);

	void updateHUD(Player player);
}
