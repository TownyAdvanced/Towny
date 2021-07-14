package com.palmergames.bukkit.towny;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;

public class TownyBungeeMessaging implements PluginMessageListener {

	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] message) {
		if (!channel.equals("BungeeCord")) {
			return;
		}
		ByteArrayDataInput in = ByteStreams.newDataInput(message);
		String subchannel = in.readUTF();
		if (subchannel.equals("TownyBungeeCord")) {
			String object = in.readUTF();
			String name = in.readUTF();
			
			switch (object) {
				case "TOWN":
					if (!TownyUniverse.getInstance().hasTown(name)) {
						Town town = new Town(name);
						try {
							TownyUniverse.getInstance().registerTown(town);
						} catch (AlreadyRegisteredException ignored) {}
					}
					TownyUniverse.getInstance().getDataSource().loadTown(name);
					break;

				case "NATION":
					if (!TownyUniverse.getInstance().hasNation(name)) {
						Nation nation  = new Nation(name);
						try {
							TownyUniverse.getInstance().registerNation(nation);
						} catch (AlreadyRegisteredException ignored) {}
					}
					TownyUniverse.getInstance().getDataSource().loadNation(name);
					break;

				case "RESIDENT":
					if (!TownyUniverse.getInstance().hasResident(name)) {
						Resident res = new Resident(name);
						try {
							TownyUniverse.getInstance().registerResident(res);
						} catch (AlreadyRegisteredException ignored) {}
					}
					TownyUniverse.getInstance().getDataSource().loadResident(name);
					break;
			}
		}
	}
}
