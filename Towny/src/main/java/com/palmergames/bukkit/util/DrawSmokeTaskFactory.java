package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import org.bukkit.Color;
import org.bukkit.Location;
import org.bukkit.Particle;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;

/**
 * @author Chris H (Zren / Shade)
 *         Date: 4/15/12
 */
public class DrawSmokeTaskFactory {

	public static Consumer<Location> showToPlayer(@NotNull Player player) {
		return showToPlayer(player, WorldCoord.parseWorldCoord(player));
	}
	
	public static Consumer<Location> showToPlayer(@NotNull Player player, @NotNull WorldCoord worldCoord) {
		Resident resident = TownyAPI.getInstance().getResident(player);
		
		return showToPlayer(player, resident == null ? Color.GRAY : getAffiliationColor(resident, worldCoord));
	}
	
	public static Consumer<Location> showToPlayer(@NotNull Player player, @NotNull Color particleColor) {
		final Particle.DustOptions dustOptions = new Particle.DustOptions(particleColor, 2);
		
		return location -> player.spawnParticle(BukkitParticle.getBorderParticle(), location.add(0.5, 1.5, 0.5), 5, dustOptions);
	}

	public static Color getAffiliationColor(Resident resident, WorldCoord coord) {
		Town residentTown = resident.getTownOrNull();
		Town town = coord.getTownOrNull();

		if (residentTown == null || town == null)
			return Color.GRAY;

		if (CombatUtil.isAlly(residentTown, town))
			return Color.GREEN;
		else if (CombatUtil.isEnemy(residentTown, town))
			return Color.RED;
		else
			return Color.GRAY;
	}
}