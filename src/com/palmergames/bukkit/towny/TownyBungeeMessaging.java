package com.palmergames.bukkit.towny;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

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
			short len = in.readShort();
			byte[] msgbytes = new byte[len];
			in.readFully(msgbytes);			
			DataInputStream msgin = new DataInputStream(new ByteArrayInputStream(msgbytes));
			String object = null;
			String name = null;
			try {
				object = msgin.readUTF();
				name = msgin.readUTF();
			} catch (IOException e) {
			}
			final String townyObject = object;
			final String objectName = name;
			System.out.println("object: " + object + " : name " + name);
//			Bukkit.getScheduler().runTaskLater(Towny.getPlugin(), ()-> {
				switch (townyObject) {
				case "TOWN":
					if (!TownyUniverse.getInstance().hasTown(objectName)) {
						Town town = new Town(objectName);
						try {
							TownyUniverse.getInstance().registerTown(town);
						} catch (AlreadyRegisteredException ignored) {}
					}
					TownyUniverse.getInstance().getDataSource().loadTown(objectName);
					break;

				case "NATION":
					if (!TownyUniverse.getInstance().hasNation(objectName)) {
						Nation nation  = new Nation(objectName);
						try {
							TownyUniverse.getInstance().registerNation(nation);
						} catch (AlreadyRegisteredException ignored) {}
					}
					TownyUniverse.getInstance().getDataSource().loadNation(objectName);
					break;

				case "RESIDENT":
					if (!TownyUniverse.getInstance().hasResident(objectName)) {
						Resident res = new Resident(objectName);
						try {
							TownyUniverse.getInstance().registerResident(res);
						} catch (AlreadyRegisteredException ignored) {}
					}
					TownyUniverse.getInstance().getDataSource().loadResident(objectName);
					break;
			}	
//			}, 5l);
		}
	}
}
