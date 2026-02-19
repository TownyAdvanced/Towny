package com.palmergames.bukkit.towny.huds.providers;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.huds.HUDImplementer;

import fr.mrmicky.fastboard.adventure.FastBoard;
import net.kyori.adventure.text.Component;

public class FoliaHUD implements ServerHUD {

	final String objectiveName;
	final String displayName;
	final Consumer<Player> playerConsumer;
	final BiConsumer<Player, Object> playerWithObjectConsumer;

	Map<UUID, FastBoard> boardMap = new HashMap<>();
	Set<Player> players = new HashSet<>();


	public FoliaHUD(HUDImplementer implementer) {
		HUD hud = implementer.getHUD();
		this.displayName = hud.displayName;
		this.objectiveName = hud.objectiveName;
		this.playerConsumer = hud.playerConsumer;
		this.playerWithObjectConsumer = hud.playerWithObjectConsumer;
	}

	public boolean hasPlayer(Player player) {
		return getPlayers().contains(player);
	}

	public boolean addPlayer(Player player) {
		return players.add(player);
	}

	public boolean removePlayer(Player player) {
		return players.remove(player);
	}

	public Set<Player> getPlayers() {
		return players;
	}

	@Override
	public boolean toggleOff(Player player) {
		FastBoard board = boardMap.remove(player.getUniqueId());
		if (board != null)
			board.delete();

		return true;
	}

	@Override
	public boolean toggleOn(Player player) {
		FastBoard board = new FastBoard(player);
		boardMap.put(player.getUniqueId(), board);
		updateHUD(player);
		return true;
	}

	@Override
	public boolean isActive(Player player) {
		return boardMap.containsKey(player.getUniqueId());
	}

	@Override
	public void setTitle(UUID uuuid, Component title) {
		FastBoard board = getBoard(uuuid);
		if (board == null)
			return;

		board.updateTitle(title);
	}

	@Override
	public void setLines(UUID uuuid, List<Component> lines) {
		FastBoard board = getBoard(uuuid);
		if (board == null)
			return;

		while (lines.size() < 15)
			lines.add(Component.empty()); // Pad the List until is is the max height for a scoreboard.

		board.updateLines(lines);
	}

	@Override
	public void updateHUD(Player player, Object object) {
		playerWithObjectConsumer.accept(player, object);
	}

	@Override
	public void updateHUD(Player player) {
		playerConsumer.accept(player);
	}

	@Nullable
	private FastBoard getBoard(UUID uuid) {
		return boardMap.get(uuid);
	}
}
