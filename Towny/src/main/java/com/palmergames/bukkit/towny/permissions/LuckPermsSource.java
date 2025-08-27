package com.palmergames.bukkit.towny.permissions;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.Colors;
import net.kyori.adventure.util.TriState;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.luckperms.api.cacheddata.CachedMetaData;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.model.user.User;
import net.luckperms.api.platform.PlayerAdapter;
import net.luckperms.api.query.QueryOptions;
import net.luckperms.api.util.Tristate;
import org.bukkit.entity.Player;
import org.bukkit.permissions.Permissible;
import org.jetbrains.annotations.Nullable;

import java.util.Locale;
import java.util.UUID;

public class LuckPermsSource extends TownyPermissionSource {
	private final Towny plugin;
	private final LuckPerms luckPerms;
	private final PlayerAdapter<Player> adapter;

	public LuckPermsSource(final Towny plugin) {
		this.plugin = plugin;
		this.luckPerms = LuckPermsProvider.get();
		this.adapter = luckPerms.getPlayerAdapter(Player.class);
	}

	@Override
	public String getPrefixSuffix(Resident resident, String node) {
		final User user = this.lookupUser(resident.getName(), resident.getUUID());
		if (user == null) {
			return "";
		}

		QueryOptions queryOptions = luckPerms.getContextManager().getStaticQueryOptions();

		final Player player = resident.getPlayer();
		if (player != null) {
			queryOptions = luckPerms.getContextManager().getQueryOptions(player);
		}

		// Fetch primary group
		final Group primaryGroup = luckPerms.getGroupManager().getGroup(getPlayerGroup(player));

		final CachedMetaData groupMetadata = primaryGroup == null ? null : primaryGroup.getCachedData().getMetaData();
		final CachedMetaData playerMetadata = user.getCachedData().getMetaData(queryOptions);

		String groupPrefixSuffix = "";
		String playerPrefixSuffix = "";

		// Pull prefix/suffix for both primary group and player
		switch (node.toLowerCase(Locale.ROOT)) {
			case "prefix" -> {
				if (groupMetadata != null) {
					groupPrefixSuffix = groupMetadata.getSuffix();
				}
				playerPrefixSuffix = playerMetadata.getPrefix();
			}
			case "suffix" -> {
				if (primaryGroup != null) {
					groupPrefixSuffix = groupMetadata.getSuffix();
				}
				playerPrefixSuffix = playerMetadata.getSuffix();
			}
			case "userprefix" -> playerPrefixSuffix = playerMetadata.getPrefix();
			case "usersuffix" -> playerPrefixSuffix = playerMetadata.getSuffix();
			case "groupprefix" -> groupPrefixSuffix = groupMetadata != null ? groupMetadata.getPrefix() : "";
			case "groupsuffix" -> groupPrefixSuffix = groupMetadata != null ? groupMetadata.getSuffix() : "";
		}

		// Normalize
		if (groupPrefixSuffix == null)
			groupPrefixSuffix = "";
		if (playerPrefixSuffix == null)
			playerPrefixSuffix = "";

		// Combine, if different
		String prefixSuffix = playerPrefixSuffix;
		if (!playerPrefixSuffix.equals(groupPrefixSuffix))
			prefixSuffix = groupPrefixSuffix + playerPrefixSuffix;

		return Colors.translateColorCodes(prefixSuffix);
	}

	@Override
	public int getGroupPermissionIntNode(String playerName, String node) {
		final User user = lookupUser(playerName, null);
		if (user == null) {
			return -1;
		}

		int value = -1;
		final Group group = luckPerms.getGroupManager().getGroup(user.getPrimaryGroup());
		if (group != null) {
			final String metadata = group.getCachedData().getMetaData().getMetaValue(node);
			value = parseInt(metadata);
		}

		if (value == -1) {
			value = getPlayerPermissionIntNode(playerName, node);
		}

		return value;
	}

	@Override
	public String getPlayerGroup(Player player) {
		return this.adapter.getUser(player).getPrimaryGroup();
	}

	@Override
	public int getPlayerPermissionIntNode(String playerName, String node) {
		return parseInt(getPlayerPermissionStringNode(playerName, node));
	}

	@Override
	public String getPlayerPermissionStringNode(String playerName, String node) {
		final User user;
		QueryOptions queryOptions = luckPerms.getContextManager().getStaticQueryOptions();

		final Player player = plugin.getServer().getPlayerExact(playerName);
		if (player != null) {
			user = adapter.getUser(player);
			queryOptions = luckPerms.getContextManager().getQueryOptions(player);
		} else {
			user = lookupUser(playerName, null);
		}

		if (user == null) {
			return "";
		}

		final String result = user.getCachedData().getMetaData(queryOptions).getMetaValue(node);
		return result != null ? result : "";
	}

	@Override
	public TriState strictHas(Permissible permissible, String node) {
		if (!(permissible instanceof Player player)) {
			return super.strictHas(permissible, node);
		}

		return convertTriState(this.adapter.getUser(player).getCachedData().getPermissionData(luckPerms.getContextManager().getQueryOptions(player)).checkPermission(node));
	}

	@Nullable
	private User lookupUser(final String playerName, @Nullable UUID uuid) {
		if (playerName == null) {
			return null;
		}

		final Player player = plugin.getServer().getPlayerExact(playerName);
		if (player != null) {
			return adapter.getUser(player);
		}

		if (uuid == null) {
			uuid = luckPerms.getUserManager().lookupUniqueId(playerName).join();
		}

		if (uuid == null) {
			return null;
		}

		return luckPerms.getUserManager().loadUser(uuid, playerName).join();
	}

	private TriState convertTriState(final Tristate tristate) {
		return switch (tristate) {
			case TRUE -> TriState.TRUE;
			case FALSE -> TriState.FALSE;
			case UNDEFINED -> TriState.NOT_SET;
		};
	}

	private int parseInt(String string) {
		if (string == null) {
			return -1;
		}

		try {
			return Integer.parseInt(string);
		} catch (NumberFormatException e) {
			return -1;
		}
	}
}
