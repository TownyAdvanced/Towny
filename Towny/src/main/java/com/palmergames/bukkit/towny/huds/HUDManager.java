package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.huds.providers.FoliaHUD;
import com.palmergames.bukkit.towny.huds.providers.HUD;
import com.palmergames.bukkit.towny.huds.providers.PaperHUD;
import com.palmergames.bukkit.towny.huds.providers.ServerHUD;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.Nullable;

public class HUDManager implements Listener {

	private static final String MAP_HUD_OBJECTIVE_NAME = "MAP_HUD_OBJ";
	private static final String PERM_HUD_OBJECTIVE_NAME = "PLOT_PERM_OBJ";
	static Map<String, ServerHUD> huds = new HashMap<>();
	/** Scoreboards cannot show more than 15 lines. **/
	static final int MAX_SCOREBOARD_HEIGHT = 15;

	public HUDManager(Towny plugin) {
		boolean isFolia = Towny.getPlugin().isFolia();

		HUD permHUD = new HUD("permHUD", PERM_HUD_OBJECTIVE_NAME, (p) -> PermHUD.updatePerms(p), (p, wc) -> PermHUD.updatePerms(p, (WorldCoord) wc));
		PermHUD permHud = new PermHUD(permHUD);
		huds.put("permHUD", isFolia ? new FoliaHUD(permHud) : new PaperHUD(permHud));

		HUD mapHUD = new HUD("mapHUD", MAP_HUD_OBJECTIVE_NAME, (p) -> MapHUD.updateMap(p), (p, wc) -> MapHUD.updateMap(p, (WorldCoord) wc));
		MapHUD mapHud = new MapHUD(mapHUD);
		huds.put("mapHUD", isFolia ? new FoliaHUD(mapHud) : new PaperHUD(mapHud));
	}

	@Nullable
	public static ServerHUD getHUD(String name) {
		return huds.get(name);
	}

	public static void addHUD(String name, ServerHUD hud) {
		huds.put(name, null);
	}

	public static void removeHUD(String name) {
		huds.remove(name);
	}

	public static void toggleHUD(Player player, String hudName) {
		if (!huds.containsKey(hudName)) {
			Towny.getPlugin().getLogger().warning("Unabled to toggle " + hudName + " hud for player " + player.getName() + ", it does not exist.");
			return;
		}

		ServerHUD hud = huds.get(hudName);
		if (hud.hasPlayer(player)) {
			hud.toggleOff(player);
			return;
		}

		toggleAllOff(player);
		hud.addPlayer(player);
		hud.toggleOn(player);
	}

	/**
	 * Called from the /plot perm hud command.
	 * @param p Player who is toggling their perm hud.
	 */
	public static void togglePermHUD (Player p) {
		toggleHUD(p, "permHUD");
	}

	/**
	 * Called from the /towny map hud command.
	 * 
	 * @param player Player who is toggling their map hud.
	 */
	public static void toggleMapHud(Player player) {
		toggleHUD(player, "mapHUD");
	}

	public static void toggleAllOff (Player p) {
		for (ServerHUD hud : new ArrayList<>(huds.values())) {
			if (hud.hasPlayer(p)) {
				hud.removePlayer(p);
				if (hud.isActive(p))
					hud.toggleOff(p);
			}
		}
	}

	//**EVENTS**//
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		Player p = event.getPlayer();
		for (ServerHUD huds : new ArrayList<>(huds.values())) {
			if (huds.hasPlayer(p)) {
				huds.removePlayer(p);
			}
		}
	}

	@EventHandler
	public void onPlayerMovePlotsEvent(PlayerChangePlotEvent event) {
		Player p = event.getPlayer();
		if (!event.getTownyWorldTo().isUsingTowny()) {
			toggleAllOff(p);
			return;
		}

		ServerHUD serverHUD = huds.get("permHUD");
		if (serverHUD != null && serverHUD.hasPlayer(p)) {
			if (!serverHUD.isActive(p)) // Player is seeing another HUD.
				serverHUD.removePlayer(p);
			else
				serverHUD.updateHUD(p, event.getTo());
		}

		serverHUD = huds.get("mapHUD");
		if (serverHUD != null && serverHUD.hasPlayer(p)) {
			if (!serverHUD.isActive(p)) // Player is seeing another HUD.
				serverHUD.removePlayer(p);
			else
				serverHUD.updateHUD(p, event.getTo());
		}
	}

	//Perm Specific
	@EventHandler
	public void onTownBlockSettingsChanged (TownBlockSettingsChangedEvent e) {

		ServerHUD hud = huds.get("permHUD");
		if (e.getTownyWorld() != null) 
			hud.getPlayers().stream()
				.filter(p -> TownyAPI.getInstance().isWilderness(p.getLocation()))
				.forEach(p -> hud.updateHUD(p));

		else if (e.getTown() != null)
			hud.getPlayers().stream()
				.filter(p -> WorldCoord.parseWorldCoord(p).hasTown(e.getTown()))
				.forEach(p -> hud.updateHUD(p));

		else if (e.getTownBlock() != null)
			hud.getPlayers().stream()
				.filter(p -> e.getTownBlock().getWorldCoord().equals(WorldCoord.parseWorldCoord(p)))
				.forEach(p -> hud.updateHUD(p));
	}

	public static String check(String string) {
		return string.length() > 64 ? string.substring(0, 64) : string;
	}

	public static boolean isUsingTownyHUD(Player player) {
		return huds.values().stream().anyMatch(hud -> hud.hasPlayer(player) && hud.isActive(player));
	}

	/**
	 * @deprecated since 0.102.0.8
	 * @return the list of perm hud users
	 */
	@Deprecated 
	public static List<Player> getPermHUDUsers() {
		return new ArrayList<>(huds.get("permHUD").getPlayers());
	}

	/**
	 * @deprecated since 0.102.0.8
	 * @return the list of map hud users
	 */
	@Deprecated 
	public static List<Player> getMapHUDUsers() {
		return new ArrayList<>(huds.get("mapHUD").getPlayers());
	}

	/**
	 * @deprecated since 0.102.0.8
	 */
	@Deprecated 
	public static void removePermHUDUser(Player player) {
		huds.get("permHUD").toggleOff(player);
	}

	/**
	 * @deprecated since 0.102.0.8
	 */
	@Deprecated	
	public static void removeMapHUDUser(Player player) {
		huds.get("mapHUD").toggleOff(player);
	}

	/**
	 * @deprecated since 0.102.0.8
	 */
	public static boolean isPermHUDActive(Player player) {
		return huds.get("permHUD").isActive(player);
	}

	/**
	 * @deprecated since 0.102.0.8
	 */
	public static boolean isMapHudActive(Player player) {
		return huds.get("mapHUD").isActive(player);
	}
}
