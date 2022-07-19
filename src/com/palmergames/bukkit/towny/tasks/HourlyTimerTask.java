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
			decrementJailedHours();
		
		/*
		 * Fire an event other plugins can use.
		 */
		Bukkit.getPluginManager().callEvent(new NewHourEvent(System.currentTimeMillis()));
	}

	/*
	 * Reduce the number of hours jailed residents are jailed for and remove hourly jail fee from town bank if possible
	 */
	private void decrementJailedHours() {
		for (Resident resident : new ArrayList<>(universe.getJailedResidentMap()))
			if (resident.hasJailTime())
				if (resident.getJailHours() <= 1)
					Bukkit.getScheduler().runTaskLater(plugin, () -> JailUtil.unJailResident(resident, UnJailReason.SENTENCE_SERVED), 20);
				else {
					resident.setJailHours(resident.getJailHours() - 1);
					resident.save();
				}
		if (TownyEconomyHandler.isActive() && TownySettings.hourlyJailFee() >= 0)
		{
			// Check if towns can afford upkeep for prisoners and if not to release them
			for (Resident resident : new ArrayList<>(universe.getJailedResidentMap()))
			{
				Town town = resident.getJailTown();
				double hourlyJailFee = TownySettings.hourlyJailFee();
				if (!town.getAccount().canPayFromHoldings(hourlyJailFee))
				{
					Bukkit.getScheduler().runTaskLater(plugin, () -> JailUtil.unJailResident(resident, UnJailReason.INSUFFICIENT_FUNDS), 20);
				}
				else
				{
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_x_has_been_withdrawn_for_upkeep_of_prisoner_x", hourlyJailFee,resident));
					String reason = "Jailee Upkeep for " + resident.getName();
					town.getAccount().withdraw(hourlyJailFee, reason);
				}
					
			}
		}
	}
}