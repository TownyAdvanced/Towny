package com.palmergames.bukkit.towny.event;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.HandlerList;

import com.palmergames.bukkit.towny.object.notification.TitleNotification;

public class TitleNotificationEvent extends Event {

	private static final HandlerList handlers = new HandlerList();
	private final TitleNotification titleNotification;
	private final Player player;

	public TitleNotificationEvent(TitleNotification titleNotification, Player player) {
		super(!Bukkit.getServer().isPrimaryThread());
		this.titleNotification = titleNotification;
		this.player = player;
	}

	@Override
	public HandlerList getHandlers() {
		return handlers;
	}

	public static HandlerList getHandlerList() {

		return handlers;
	}

	public TitleNotification getTitleNotification() {
		return titleNotification;
	}

	public Player getPlayer() {
		return player;
	}
}
