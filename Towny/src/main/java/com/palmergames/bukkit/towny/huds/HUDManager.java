package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.event.PlayerChangePlotEvent;
import com.palmergames.bukkit.towny.event.TownBlockSettingsChangedEvent;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class HUDManager implements Listener{

	static List<Player> permUsers;
	static List<Player> mapUsers;
	/** Scoreboards cannot show more than 15 lines. **/
	static final int MAX_SCOREBOARD_HEIGHT = 15;

	public HUDManager (Towny plugin) {
		permUsers = new ArrayList<>();
		mapUsers = new ArrayList<>();
	}

	//**TOGGLES**//
	public static void togglePermHUD (Player p) {
		if (Towny.getPlugin().isFolia()) {
			TownyMessaging.sendErrorMsg(p, Translatable.of("msg_folia_scoreboard"));
			return;
		}
		
		if (!permUsers.contains(p)) {
			toggleAllOff(p);
			permUsers.add(p);
			PermHUD.toggleOn(p);
		} else 
			toggleAllOff(p);
	}
	
	public static void toggleMapHud(Player player) {
		if (Towny.getPlugin().isFolia()) {
			TownyMessaging.sendErrorMsg(player, Translatable.of("msg_folia_scoreboard"));
			return;
		}
		
		if (!mapUsers.contains(player)) {
			toggleAllOff(player);
			mapUsers.add(player);
			MapHUD.toggleOn(player);
		} else
			toggleAllOff(player);
	}

	public static void toggleAllOff (Player p) {
		permUsers.remove(p);
		mapUsers.remove(p);
		if (p.isOnline())
			toggleOff(p);
	}
	
	public static void toggleOff(final Player player) {
		if (Towny.getPlugin().isFolia())
			return;
		
		Optional.ofNullable(Bukkit.getScoreboardManager()).ifPresent(manager -> player.setScoreboard(manager.getMainScoreboard()));
	}

	//**EVENTS**//
	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		permUsers.remove(event.getPlayer());
		mapUsers.remove(event.getPlayer());
	}

	@EventHandler
	public void onPlayerMovePlotsEvent(PlayerChangePlotEvent event) {
		Player p = event.getPlayer();
		if (permUsers.contains(p)) {
			if (!isPermHUDActive(p))
				permUsers.remove(p);
			else {
				if (event.getTownyWorldTo().isUsingTowny())
					PermHUD.updatePerms(p, event.getTo());
				else
					toggleAllOff(p);
			}	
		} else if (mapUsers.contains(p)) {
			if (!isMapHudActive(p))
				mapUsers.remove(p);
			else
				if (event.getTownyWorldTo().isUsingTowny())
					MapHUD.updateMap(p, event.getTo());
				else
					toggleAllOff(p);
		}
	}

	//Perm Specific
	@EventHandler
	public void onTownBlockSettingsChanged (TownBlockSettingsChangedEvent e) {

		if (e.getTownyWorld() != null)
			permUsers.forEach(p -> PermHUD.updatePerms(p));
		else if (e.getTown() != null)
			permUsers.stream().filter(p -> WorldCoord.parseWorldCoord(p).hasTown(e.getTown()))
				.forEach(p -> PermHUD.updatePerms(p));
		else if (e.getTownBlock() != null)
			permUsers.stream().filter(p -> e.getTownBlock().getWorldCoord().equals(WorldCoord.parseWorldCoord(p)))
				.forEach(p -> PermHUD.updatePerms(p));
	}

	public static String check(String string) {
		return string.length() > 64 ? string.substring(0, 64) : string;
	}

	public static boolean isUsingTownyHUD(Player player) {
		return permUsers.contains(player) || mapUsers.contains(player);
	}

	public static List<Player> getPermHUDUsers() {
		return permUsers;
	}
	
	public static List<Player> getMapHUDUsers() {
		return mapUsers;
	}

	public static void removePermHUDUser(Player player) {
		if (permUsers.remove(player)) {
			toggleOff(player);
		}
	}
	
	public static void removeMapHUDUser(Player player) {
		if (mapUsers.remove(player)) {
			toggleOff(player);
		}
	}

	public static boolean isPermHUDActive(Player player) {
		return player.getScoreboard().getTeam(PermHUD.permHudTestKey()) != null;
	}

	public static boolean isMapHudActive(Player player) {
		return player.getScoreboard().getTeam(MapHUD.mapHudTestKey()) != null;
	}
}
