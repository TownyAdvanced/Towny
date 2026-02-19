package com.palmergames.bukkit.towny.huds.providers;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Scoreboard;
import org.jetbrains.annotations.Nullable;

import com.palmergames.bukkit.towny.huds.HUDImplementer;
import com.palmergames.bukkit.util.BukkitTools;

import io.papermc.paper.scoreboard.numbers.NumberFormat;
import net.kyori.adventure.text.Component;

public class PaperHUD implements ServerHUD {

	final String displayName;
	final String objectiveName;
	final Consumer<Player> playerConsumer;
	final BiConsumer<Player, Object> playerWithObjectConsumer;

	Map<UUID, Scoreboard> boardMap = new HashMap<>();
	Set<Player> players = new HashSet<>();

	public PaperHUD(HUDImplementer implementer) {
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
		boardMap.remove(player.getUniqueId());
		return players.remove(player);
	}

	public Set<Player> getPlayers() {
		return players;
	}

	@Nullable
	private Objective getObjective(UUID uuuid) {
		Scoreboard board = boardMap.get(uuuid);
		if (board == null)
			return null;
		Objective objective = board.getObjective(objectiveName);
		return objective;
	}

	@Override
	public boolean toggleOff(Player player) {
		Optional.ofNullable(Bukkit.getScoreboardManager()).ifPresent(manager -> player.setScoreboard(manager.getMainScoreboard()));
		removePlayer(player);
		return true; 
	}

	@Override
	public boolean toggleOn(Player player) {
		Scoreboard board = Bukkit.getScoreboardManager().getNewScoreboard();
		Objective objective = BukkitTools.objective(board, objectiveName, displayName);
		objective.setDisplaySlot(DisplaySlot.SIDEBAR);
		objective.setAutoUpdateDisplay(false);
		try {
			objective.numberFormat(NumberFormat.blank());
		} catch (NoSuchMethodError | NoClassDefFoundError ignored) {}

		board.registerNewTeam(objectiveName);
		boardMap.put(player.getUniqueId(), board);

		addPlayer(player);
		updateHUD(player);
		player.setScoreboard(board);
		return true;
	}

	@Override
	public boolean isActive(Player player) {
		return player.getScoreboard().getObjective(objectiveName) != null;
	}

	@Override
	public void setTitle(UUID uuuid, Component title) {
		Objective objective = getObjective(uuuid);
		if (objective == null)
			return;
		objective.displayName(title);
	}

	@Override
	public void setLines(UUID uuuid, List<Component> lines) {
		Objective objective = getObjective(uuuid);
		if (objective == null)
			return;

		while (lines.size() < 15)
			lines.add(Component.empty()); // Pad the List until is is the max height for a scoreboard.

		Collections.reverse(lines);
		for (int i = 14; i >= 0 ; i--) {
			String teamName = String.valueOf(i);
			objective.getScore(teamName).setScore(i);
			objective.getScore(teamName).customName(lines.get(i));
		}
	}

	@Override
	public void updateHUD(Player player, Object object) {
		playerWithObjectConsumer.accept(player, object);
	}

	@Override
	public void updateHUD(Player player) {
		playerConsumer.accept(player);
	}
}
