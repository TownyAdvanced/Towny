package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.object.economy.Account;
import org.bukkit.Location;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * @since 0.99.0.10
 */
public class TeleportRequest {
	private final long requestTime;
	private final long teleportTime;
	private final Location destination;
	private final int cooldown;
	private final double teleportCost;
	private final Account account;
	
	private TeleportRequest(long requestTime, long teleportTime, @NotNull Location destination, int cooldown, double teleportCost, @Nullable Account account) {
		this.requestTime = requestTime;
		this.teleportTime = teleportTime;
		this.destination = destination;
		this.cooldown = cooldown;
		this.teleportCost = teleportCost;
		this.account = account;
	}
	
	public static TeleportRequest teleportRequest(long requestTime, long teleportTime, @NotNull Location destination, int cooldown) {
		return teleportRequest(requestTime, teleportTime, destination, cooldown, 0, null);
	}
	
	public static TeleportRequest teleportRequest(long requestTime, long teleportTime, @NotNull Location destination, int cooldown, double teleportCost, @Nullable Account account) {
		return new TeleportRequest(requestTime, teleportTime, destination, cooldown, teleportCost, account);
	}
	
	public long requestTime() {
		return this.requestTime;
	}
	
	public long teleportTime() {
		return this.teleportTime;
	}
	
	@NotNull
	public Location destinationLocation() {
		return this.destination;
	}
	
	public int cooldown() {
		return this.cooldown;
	}
	
	public double teleportCost() {
		return this.teleportCost;
	}
	
	@Nullable
	public final Account teleportAccount() {
		return this.account;
	}
}
