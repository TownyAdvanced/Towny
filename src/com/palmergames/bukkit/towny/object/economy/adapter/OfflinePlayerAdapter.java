package com.palmergames.bukkit.towny.object.economy.adapter;

import com.palmergames.bukkit.towny.Towny;
import org.bukkit.BanList;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.Statistic;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.profile.PlayerProfile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class OfflinePlayerAdapter implements OfflinePlayer {
	private final String name;
	private final UUID uuid;
	private @Nullable PlayerProfile profile;
	
	public OfflinePlayerAdapter(@NotNull String name, @NotNull UUID uuid) {
		this.name = name;
		this.uuid = uuid;
	}

	@Override
	public boolean isOnline() {
		return Bukkit.getPlayer(this.uuid) != null;
	}

	@Nullable
	@Override
	public String getName() {
		return name;
	}

	@Override
	public @NotNull UUID getUniqueId() {
		return uuid;
	}

	@NotNull
	@Override
	public PlayerProfile getPlayerProfile() {
		if (this.profile == null)
			this.profile = Bukkit.createPlayerProfile(this.uuid, this.name);
		
		return this.profile;
	}

	@Override
	public boolean isBanned() {
		return Bukkit.getBanList(BanList.Type.NAME).isBanned(this.name);
	}

	@Override
	public boolean isWhitelisted() {
		return false;
	}

	@Override
	public void setWhitelisted(boolean value) {

	}

	@Nullable
	@Override
	public Player getPlayer() {
		return Bukkit.getPlayer(this.uuid);
	}

	@Override
	public long getFirstPlayed() {
		return 0;
	}

	@Override
	public long getLastPlayed() {
		return 0;
	}

	@Override
	public boolean hasPlayedBefore() {
		return false;
	}

	@Nullable
	@Override
	public Location getBedSpawnLocation() {
		return null;
	}

	@Override
	public void incrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {

	}

	@Override
	public void incrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(@NotNull Statistic statistic, int amount) throws IllegalArgumentException {

	}

	@Override
	public void setStatistic(@NotNull Statistic statistic, int newValue) throws IllegalArgumentException {

	}

	@Override
	public int getStatistic(@NotNull Statistic statistic) throws IllegalArgumentException {
		return 0;
	}

	@Override
	public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {

	}

	@Override
	public int getStatistic(@NotNull Statistic statistic, @NotNull Material material) throws IllegalArgumentException {
		return 0;
	}

	@Override
	public void incrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(@NotNull Statistic statistic, @NotNull Material material, int amount) throws IllegalArgumentException {

	}

	@Override
	public void setStatistic(@NotNull Statistic statistic, @NotNull Material material, int newValue) throws IllegalArgumentException {

	}

	@Override
	public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {

	}

	@Override
	public int getStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType) throws IllegalArgumentException {
		return 0;
	}

	@Override
	public void incrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) throws IllegalArgumentException {

	}

	@Override
	public void decrementStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int amount) {

	}

	@Override
	public void setStatistic(@NotNull Statistic statistic, @NotNull EntityType entityType, int newValue) {

	}

	@NotNull
	@Override
	public Map<String, Object> serialize() {
		Map<String, Object> map = new LinkedHashMap<>();
		map.put("UUID", this.uuid.toString());
		return map;
	}

	@Override
	public boolean isOp() {
		return false;
	}

	@Override
	public void setOp(boolean value) {

	}
}
