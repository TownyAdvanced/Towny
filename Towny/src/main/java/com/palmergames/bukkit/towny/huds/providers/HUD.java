package com.palmergames.bukkit.towny.huds.providers;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.entity.Player;

public class HUD {
	final String displayName;
	final String objectiveName;
	final Consumer<Player> playerConsumer;
	final BiConsumer<Player, Object> playerWithObjectConsumer;

	public HUD(String name, String objectiveName, Consumer<Player> playerConsumer, BiConsumer<Player, Object> playerWithObjectConsumer) {
		this.displayName = name;
		this.objectiveName = objectiveName;
		this.playerConsumer = playerConsumer;
		this.playerWithObjectConsumer = playerWithObjectConsumer;
	}
}
