package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import net.querz.nbt.io.NBTSerializer;
import net.querz.nbt.io.NamedTag;
import net.querz.nbt.tag.CompoundTag;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.*;

public class FMapOverlayListener implements Listener {
	
	private final Towny plugin;
	private final Set<UUID> fMapOverlayPlayers;
	private static FMapOverlayListener instance; 

	public FMapOverlayListener(Towny instance) {

		plugin = instance;
		FMapOverlayListener.instance = this; 
		fMapOverlayPlayers = new HashSet<>();
	}
	
	public static FMapOverlayListener getInstance() {
		return instance;
	}

	public Set<UUID> getFMapOverlayPlayers() {
		return fMapOverlayPlayers;
	}

	@EventHandler
	public void onPlayerRegisterChannel(PlayerRegisterChannelEvent event) {
		handleChannelEvent(event, Action.REGISTER);
	}

	@EventHandler
	public void onPlayerUnregisterChannel(PlayerUnregisterChannelEvent event) {
		handleChannelEvent(event, Action.UNREGISTER);
	}

	@EventHandler
	public void onPlayerQuit(PlayerQuitEvent event) {
		getFMapOverlayPlayers().remove(event.getPlayer().getUniqueId());
	}
	
	public void sendFMapOverlayTowny(Player player, CompoundTag data) {
		if (getFMapOverlayPlayers().contains(player.getUniqueId())) {
			sendFMapOverlayTownyData(player, data);
		}
	}

	private void handleChannelEvent(PlayerChannelEvent event, Action action) {
		if (!event.getChannel().equals(Towny.FMAPOVERLAY_PLUGIN_CHANNEL)) return;

		Player player = event.getPlayer();
		switch (action) {
			case REGISTER:
				getFMapOverlayPlayers().add(player.getUniqueId());
				plugin.getLogger().info("Player " + player.getDisplayName() + " registered their fmapoverlay channel");
				break;
			case UNREGISTER:
				getFMapOverlayPlayers().remove(player.getUniqueId());
				plugin.getLogger().info("Player " + player.getDisplayName() + " unregistered their fmapoverlay channel");
				break;
		}
	}

	private void sendFMapOverlayTownyData(Player player, CompoundTag data) {
		if (!player.hasPermission("fmapoverlay.register")) return;
		try {
			byte[] townyByteArr = new NBTSerializer(false).toBytes(new NamedTag("townyData",data));
			player.sendPluginMessage(plugin, Towny.FMAPOVERLAY_PLUGIN_CHANNEL, townyByteArr);
		} catch (IOException ignored) {
		}
	}
	
	public enum Action {
		REGISTER,
		UNREGISTER
	}
}
