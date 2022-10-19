package com.palmergames.bukkit.towny.tasks;

import java.util.ArrayList;

import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.Translatable;
import org.bukkit.Bukkit;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.time.NewHourEvent;
import com.palmergames.bukkit.towny.invites.InviteHandler;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.jail.UnJailReason;
import com.palmergames.bukkit.towny.utils.JailUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.util.BukkitTools;

/**
 * This class represents the hourly timer task
 * It is generally set to run once per hour
 * This rate can be configured.
 *
 * @author Goosius
 */
public class HourlyTimerTask extends TownyTimerTask {

	public HourlyTimerTask(Towny plugin) {
		super(plugin);
	}

	@Override
	public void run() {
		if (TownySettings.getTownRuinsEnabled()) {
			TownRuinUtil.evaluateRuinedTownRemovals();
		}
		
		if (TownySettings.getInviteExpirationTime() > 0)
			InviteHandler.searchForExpiredInvites();

		if (!universe.getJailedResidentMap().isEmpty())
			decrementJailedHoursAndIncurJailFees();
		
		/*
		 * Fire an event other plugins can use.
		 */
		BukkitTools.fireEvent(new NewHourEvent(System.currentTimeMillis()));
	}

	/*
	 * Reduce the number of hours jailed residents are jailed for and remove hourly jail fee from town bank if possible
	 */
	private void decrementJailedHoursAndIncurJailFees() {
		double hourlyJailFee = TownyEconomyHandler.isActive() && TownySettings.hourlyJailFee() > 0 ? TownySettings.hourlyJailFee() : 0;
		for (Resident resident : new ArrayList<>(universe.getJailedResidentMap()))
			if (resident.hasJailTime()) {
				// Resident has served their sentence.
				if (resident.getJailHours() <= 1) {
					Bukkit.getScheduler().runTaskLater(plugin, () -> JailUtil.unJailResident(resident, UnJailReason.SENTENCE_SERVED), 20);
					continue;
				}

				// The jailing Town might have to pay to keep the resident locked up.
				if (hourlyJailFee > 0) {
					Town jailTown = resident.getJailTown();
					if (!jailTown.getAccount().withdraw(hourlyJailFee, "Jailee Hourly Fee for " + resident.getName())) {
						// Town receives unjail message stating lack of money from within JailUtil.
						Bukkit.getScheduler().runTaskLater(plugin, () -> JailUtil.unJailResident(resident, UnJailReason.INSUFFICIENT_FUNDS), 20);
						continue;
					} else {
						TownyMessaging.sendPrefixedTownMessage(jailTown, Translatable.of("msg_x_has_been_withdrawn_for_upkeep_of_prisoner_x", hourlyJailFee,resident));
					}
				}

				// Reduce the hours and save the resident.
				resident.setJailHours(resident.getJailHours() - 1);
				resident.save();
			}
	}
}