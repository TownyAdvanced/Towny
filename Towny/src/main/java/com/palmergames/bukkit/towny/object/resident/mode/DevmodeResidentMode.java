package com.palmergames.bukkit.towny.object.resident.mode;

import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.util.BukkitTools;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DevmodeResidentMode extends OptionResidentMode<Set<Player>> {

	public DevmodeResidentMode(String name, String permissionNode) {
		super(name, permissionNode);
	}

	/**
	 * Get the debug target players for a resident.
	 *
	 * @param resident {@inheritDoc}
	 * @return the debug target players for the resident.
	 */
	@Override
	@NotNull
	protected Set<Player> getOption(Resident resident) {
		Set<Player> option = super.getOption(resident);
		return option == null ? new HashSet<>() : option;
	}

	@Override
	protected void toggle(Resident resident, String... options) {
		if (ResidentModeHandler.hasMode(resident, this) && options.length == 0) {
			Set<Player> targets = getOption(resident);
			if (!targets.isEmpty()) {
				clearOptions(resident);
			}
			ResidentModeHandler.removeMode(resident, this);
			return;
		}

		List<Player> newTargets = Arrays.stream(options)
			.map(playerName -> {
				Player player = BukkitTools.getPlayerExact(playerName);
				if (player == null) {
					TownyMessaging.sendErrorMsg(resident.getPlayer(), Translatable.of("msg_err_resident_unknown", playerName));
				}
				return player;
			})
			.filter(Objects::nonNull)
			.toList();
		if (!newTargets.isEmpty()) {
			Set<Player> targets = getOption(resident);
			for (Player newTarget : newTargets) {
				if (!targets.add(newTarget))
					// newTarget is already in list and wants to be removed
					targets.remove(newTarget);
			}
			setOption(resident, targets);
		}
		if (ResidentModeHandler.hasMode(resident, this) && getOption(resident).isEmpty()) {
			// Turn off devmode as player has removed all debug target players
			ResidentModeHandler.removeMode(resident, this);
		} else {
			ResidentModeHandler.addMode(resident, this);
		}
	}
}
