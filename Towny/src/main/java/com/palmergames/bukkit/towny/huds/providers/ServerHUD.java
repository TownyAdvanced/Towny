package com.palmergames.bukkit.towny.huds.providers;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.bukkit.entity.Player;

import com.palmergames.bukkit.util.BukkitTools;

import net.kyori.adventure.text.Component;

public abstract interface ServerHUD {

	boolean toggleOff(Player player);

	boolean toggleOn(Player player);
	
	public default boolean hasPlayer(Player player) {
		return getPlayerUUIDs().contains(player.getUniqueId());
	}

	public default boolean addPlayer(Player player) {
		return getPlayerUUIDs().add(player.getUniqueId());
	}

	public default boolean removePlayer(Player player) {
		return getPlayerUUIDs().remove(player.getUniqueId());
	}

	public Set<UUID> getPlayerUUIDs();

	public default Set<Player> getPlayers() {
		return getPlayerUUIDs().stream().map(uuid -> BukkitTools.getPlayer(uuid)).collect(Collectors.toSet());
	}

	boolean isActive(Player player);

	void setTitle(UUID uuuid, Component title);

	void setLines(UUID uuuid, List<Component> lines);

	void updateHUD(Player player, Object object);

	void updateHUD(Player player);
}
