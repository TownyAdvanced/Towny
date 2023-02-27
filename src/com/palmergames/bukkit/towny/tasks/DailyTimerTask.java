package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.PreNewDayEvent;
import com.palmergames.bukkit.towny.event.time.dailytaxes.NewDayTaxAndUpkeepPreCollectionEvent;
import com.palmergames.bukkit.towny.event.time.dailytaxes.PreTownPaysNationTaxEvent;
import com.palmergames.bukkit.towny.event.town.TownUnconquerEvent;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.util.StringMgmt;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import org.bukkit.Bukkit;

public class DailyTimerTask extends TownyTimerTask {
	
	private double totalTownUpkeep = 0.0;
	private double totalNationUpkeep = 0.0;
	private final List<String> bankruptedTowns = new ArrayList<>();
	private final List<String> removedTowns = new ArrayList<>();
	private final List<String> removedNations = new ArrayList<>();

	public DailyTimerTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {

		long start = System.currentTimeMillis();
		totalTownUpkeep = 0.0;
		totalNationUpkeep = 0.0;
		bankruptedTowns.clear();
		removedTowns.clear();
		removedNations.clear();

		BukkitTools.fireEvent(new PreNewDayEvent()); // Pre-New Day Event
		
		TownyMessaging.sendDebugMsg("New Day");

		/*
		 * If enabled, collect taxes and then server upkeep costs.
		 */		
		if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily()) {
			if (!BukkitTools.isEventCancelled(new NewDayTaxAndUpkeepPreCollectionEvent())) {
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_day_tax"));
				TownyMessaging.sendDebugMsg("Collecting Town Taxes");
				collectTownTaxes();
				TownyMessaging.sendDebugMsg("Collecting Nation Taxes");
				collectNationTaxes();
				TownyMessaging.sendDebugMsg("Collecting Town Costs");
				collectTownCosts();
				TownyMessaging.sendDebugMsg("Collecting Nation Costs");
				collectNationCosts();
			} else {
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_day"));
			}
		} else
			TownyMessaging.sendGlobalMessage(Translatable.of("msg_new_day"));

		/*
		 * If enabled, remove old residents who haven't logged in for the configured number of days.
		 */	
		if (TownySettings.isDeletingOldResidents()) {
			Bukkit.getScheduler().runTaskAsynchronously(plugin, new ResidentPurge(null, TownySettings.getDeleteTime() * 1000, TownySettings.isDeleteTownlessOnly(), null));
		}
		
		//Clean up unused NPC residents
		Bukkit.getScheduler().runTaskAsynchronously(plugin, new NPCCleanupTask());

		/*
		 * If enabled, remove all 0-plot towns.
		 */
		if (TownySettings.isNewDayDeleting0PlotTowns()) {
			List<String> deletedTowns = new ArrayList<>();
			for (Town town : universe.getTowns()) {
				if (town.getTownBlocks().size() == 0) {
					deletedTowns.add(town.getName());
					removedTowns.add(town.getName());
					universe.getDataSource().removeTown(town);
				}
			}
			if (!deletedTowns.isEmpty())
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_the_following_towns_were_deleted_for_having_0_claims", String.join(", ", deletedTowns)));
		}
		
		/*
		 * Reduce the number of days conquered towns are conquered for.
		 */
		for (Town town : universe.getTowns()) {
			if (town.isConquered()) {
				if (town.getConqueredDays() == 1)
					Bukkit.getScheduler().runTask(plugin, () -> unconquer(town));
				else
					town.setConqueredDays(town.getConqueredDays() - 1);
			}
		}

		/*
		 * Run backup on a separate thread, to let the DailyTimerTask thread terminate as intended.
		 */
		if (TownySettings.isBackingUpDaily()) {			
			universe.performCleanupAndBackup();
		}

		// Fire the new-day event.
		BukkitTools.fireEvent(new NewDayEvent(bankruptedTowns, removedTowns, removedNations, totalTownUpkeep, totalNationUpkeep, start));
		
		TownyMessaging.sendDebugMsg("Finished New Day Code");
		TownyMessaging.sendDebugMsg("Universe Stats:");
		TownyMessaging.sendDebugMsg("    Residents: " + universe.getNumResidents());
		TownyMessaging.sendDebugMsg("    Towns: " + universe.getTowns().size());
		TownyMessaging.sendDebugMsg("    Nations: " + universe.getNumNations());
		for (TownyWorld world : universe.getTownyWorlds())
			TownyMessaging.sendDebugMsg("    " + world.getName() + " (townblocks): " + universe.getTownBlocks().size());

		TownyMessaging.sendDebugMsg("Memory (Java Heap):");
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (max)", Runtime.getRuntime().maxMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (total)", Runtime.getRuntime().totalMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (free)", Runtime.getRuntime().freeMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (used=total-free)", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024));
		Towny.getPlugin().getLogger().info("Towny DailyTimerTask took " + (System.currentTimeMillis() - start) + "ms to process.");
		
		// Run the new day scheduler again one minute later to begin scheduling the next New Day.
		if (!NewDayScheduler.isNewDaySchedulerRunning())
			Bukkit.getScheduler().runTaskLater(plugin, new NewDayScheduler(plugin), 60 * 20);
	}

	private void unconquer(Town town) {
		if (BukkitTools.isEventCancelled(new TownUnconquerEvent(town)))
			return;
		
		town.setConquered(false);
		town.setConqueredDays(0);
	}

	/**
	 * Collect taxes for all nations due from their member towns
	 */
	public void collectNationTaxes() {
		List<Nation> nations = new ArrayList<>(universe.getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation;

		while (nationItr.hasNext()) {
			nation = nationItr.next();
			/*
			 * Only collect tax for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (universe.hasNation(nation.getName()))
				collectNationTaxes(nation);
		}
	}

	/**
	 * Collect taxes due to the nation from it's member towns.
	 * 
	 * @param nation - Nation to collect taxes from.
	 */
	protected void collectNationTaxes(Nation nation) {
		
		if (nation.getTaxes() > 0) {

			double taxAmount = nation.getTaxes();
			List<String> localNewlyDelinquentTowns = new ArrayList<>();
			List<String> localTownsDestroyed = new ArrayList<>();
			List<Town> towns = new ArrayList<>(nation.getTowns());
			ListIterator<Town> townItr = towns.listIterator();
			Town town;

			while (townItr.hasNext()) {
				town = townItr.next();

				/*
				 * Only collect nation tax from this town if it really still
				 * exists.
				 * We are running in an Async thread so MUST verify all objects.
				 */
				if (universe.hasTown(town.getName())) {
					if ((town.isCapital() && !TownySettings.doCapitalsPayNationTax()) || !town.hasUpkeep() || town.isRuined())
						continue;
					
					if (nation.isTaxPercentage()) {
						taxAmount = town.getAccount().getHoldingBalance() * taxAmount / 100;
						taxAmount = Math.min(taxAmount, nation.getMaxPercentTaxAmount());
					}

					PreTownPaysNationTaxEvent event = new PreTownPaysNationTaxEvent(town, nation, taxAmount);
					if (BukkitTools.isEventCancelled(event)) {
						TownyMessaging.sendPrefixedTownMessage(town, event.getCancelMessage());
						continue;
					}
					taxAmount = event.getTax();
					
					// Handle if the bank cannot be paid because of the cap. It might be more than
					// the bank can accept, so we reduce it to the amount that the bank can accept,
					// even if it becomes 0.
					if (nation.getBankCap() != 0 && taxAmount + nation.getAccount().getHoldingBalance() > nation.getBankCap())
						taxAmount = nation.getBankCap() - nation.getAccount().getHoldingBalance();
					
					if (taxAmount == 0)
						continue;
					
					if (town.getAccount().canPayFromHoldings(taxAmount)) {
					// Town is able to pay the nation's tax.
						town.getAccount().payTo(taxAmount, nation, "Nation Tax to " + nation.getName());
						TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_payed_nation_tax", TownyEconomyHandler.getFormattedBalance(taxAmount)));
					} else {
					// Town is unable to pay the nation's tax.
						if (!TownySettings.isTownBankruptcyEnabled() || !TownySettings.doBankruptTownsPayNationTax()) {
						// Bankruptcy disabled, remove town for not paying nation tax, 
						// OR Bankruptcy enabled but towns aren't allowed to use debt to pay nation tax. 
							
							if (TownySettings.doesNationTaxDeleteConqueredTownsWhichCannotPay() && town.isConquered()) {
								universe.getDataSource().removeTown(town);
								localTownsDestroyed.add(town.getName());
								continue;
							}
							
							localNewlyDelinquentTowns.add(town.getName());		
							town.removeNation();
							TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_the_nation_tax_of", TownyEconomyHandler.getFormattedBalance(taxAmount)));
							continue;
						}

						// Bankruptcy enabled and towns are allowed to use debt to pay nation tax.
						boolean townWasBankrupt = town.isBankrupt();
						town.getAccount().setDebtCap(MoneyUtil.getEstimatedValueOfTown(town));
						
						if (town.getAccount().getHoldingBalance() - taxAmount < town.getAccount().getDebtCap() * -1) {
						// Towns that would go over their debtcap to pay nation tax, need the amount they pay reduced to what their debt cap can cover.
						// This will result in towns that become fully indebted paying 0 nation tax eventually.

							if (TownySettings.isNationTaxKickingTownsThatReachDebtCap()) {
							// Alternatively, when configured, a nation will kick a town that  
							// can no longer pay the full nation tax with their allowed debt. 
								localNewlyDelinquentTowns.add(town.getName());		
								town.removeNation();
								TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_the_nation_tax_of", TownyEconomyHandler.getFormattedBalance(nation.getTaxes())));
								continue;
							}
							
							taxAmount = town.getAccount().getDebtCap() - Math.abs(town.getAccount().getHoldingBalance());
						}

						// Pay the nation tax with at least some amount of debt.
						town.getAccount().withdraw(taxAmount, "Nation Tax to " + nation.getName()); // .withdraw() is used because other economy methods do not allow a town to go into debt.
						nation.getAccount().deposit(taxAmount, "Nation Tax from " + town.getName());
						TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_payed_nation_tax_with_debt", TownyEconomyHandler.getFormattedBalance(taxAmount)));

						// Check if the town was newly bankrupted and punish them for it.
						if (!townWasBankrupt) {
							town.setOpen(false);
							town.save();
							localNewlyDelinquentTowns.add(town.getName());
						}
					}
				}
			}

			String msg1 = "msg_couldnt_pay_tax";
			String msg2 = "msg_couldnt_pay_nation_tax_multiple";
			if (TownySettings.isTownBankruptcyEnabled() && TownySettings.doBankruptTownsPayNationTax()) { 
				msg1 = "msg_town_bankrupt_by_nation_tax";
				msg2 = "msg_town_bankrupt_by_nation_tax_multiple";
			}
			if (localNewlyDelinquentTowns != null && !localNewlyDelinquentTowns.isEmpty())
				if (localNewlyDelinquentTowns.size() == 1)
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of(msg1, localNewlyDelinquentTowns.get(0), Translatable.of("nation_sing")));
				else
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of(msg2).append(StringMgmt.join(localNewlyDelinquentTowns, ", ")));
			
			if (localTownsDestroyed != null && !localTownsDestroyed.isEmpty())
				if (localTownsDestroyed.size() == 1)
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_destroyed_by_nation_tax", localTownsDestroyed.get(0)));
				else
					TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_destroyed_by_nation_tax_multiple").append(StringMgmt.join(localTownsDestroyed, ", ")));
			
		}

	}

	/**
	 * Collect taxes for all towns due from their residents.
	 */
	public void collectTownTaxes() {
		List<Town> towns = new ArrayList<>(universe.getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only collect resident tax for this town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (universe.hasTown(town.getName()) && !town.isRuined())
				collectTownTaxes(town);
		}
	}

	/**
	 * Collect taxes due to the town from it's residents.
	 * 
	 * @param town - Town to collect taxes from
	 */
	protected void collectTownTaxes(Town town) {
		// Resident Tax
		if (town.getTaxes() > 0) {

			List<Resident> residents = new ArrayList<>(town.getResidents());
			ListIterator<Resident> residentItr = residents.listIterator();
			List<String> removedResidents = new ArrayList<>();
			Resident resident;

			while (residentItr.hasNext()) {
				resident = residentItr.next();

				double tax = town.getTaxes();
				/*
				 * Only collect resident tax from this resident if it really
				 * still exists. We are running in an Async thread so MUST
				 * verify all objects.
				 */
				if (universe.hasResident(resident.getName())) {

					if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC() || resident.isMayor()) {
						TownyMessaging.sendMsg(resident, Translatable.of("msg_tax_exempt"));
						continue;
					} else if (town.isTaxPercentage()) {
						tax = resident.getAccount().getHoldingBalance() * tax / 100;
						
						// Make sure that the town percent tax doesn't remove above the
						// allotted amount of cash.
						tax = Math.min(tax, town.getMaxPercentTaxAmount());

						// Handle if the bank cannot be paid because of the cap. Since it is a % 
						// they will be able to pay but it might be more than the bank can accept,
						// so we reduce it to the amount that the bank can accept, even if it
						// becomes 0.
						if (town.getBankCap() != 0 && tax + town.getAccount().getHoldingBalance() > town.getBankCap())
							tax = town.getBankCap() - town.getAccount().getHoldingBalance();
						
						if (tax == 0)
							continue;
						
						resident.getAccount().payTo(tax, town, "Town Tax (Percentage)");
					} else {
						// Check if the bank could take the money, reduce it to 0 if required so that 
						// players do not get kicked in a situation they could be paying but cannot because
						// of the bank cap.
						if (town.getBankCap() != 0 && tax + town.getAccount().getHoldingBalance() > town.getBankCap())
							tax = town.getBankCap() - town.getAccount().getHoldingBalance();
						
						
						
						
						if (tax == 0)
							continue;
						
						if (resident.getAccount().canPayFromHoldings(tax))
							resident.getAccount().payTo(tax, town, "Town tax (FlatRate)");
						else {
							removedResidents.add(resident.getName());					
							// remove this resident from the town.
							resident.removeTown();
						}
					}
				}
			}
			if (removedResidents != null && !removedResidents.isEmpty()) {
				if (removedResidents.size() == 1) 
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_tax", removedResidents.get(0), Translatable.of("town_sing")));
				else
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_town_tax_multiple").append(StringMgmt.join(removedResidents, ", ")));
			}
		}

		// Plot Tax
		List<TownBlock> townBlocks = new ArrayList<>(town.getTownBlocks());
		List<String> lostPlots = new ArrayList<>();
		ListIterator<TownBlock> townBlockItr = townBlocks.listIterator();
		TownBlock townBlock;

		while (townBlockItr.hasNext()) {
			townBlock = townBlockItr.next();
			double tax = townBlock.getType().getTax(town);

			if (!townBlock.hasResident() || tax == 0 ||
				!TownySettings.isNegativePlotTaxAllowed() && tax < 1)
				continue;

			Resident resident = townBlock.getResidentOrNull();

			/*
			 * Only collect plot tax from this resident if it really
			 * still exists. We are running in an Async thread so MUST
			 * verify all objects.
			 */
			if (universe.hasResident(resident.getName())) {
				if (resident.hasTown() && resident.getTownOrNull() == town)
					if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC())
						continue;

				// The PlotTax might be negative, in order to pay the resident for owning a special plot type.
				if (tax < 0 && town.getAccount().canPayFromHoldings(Math.abs(tax))) {
					tax = Math.abs(tax);
					town.getAccount().payTo(tax, resident.getAccount(), String.format("Plot Tax Payment To Resident (%s)", townBlock.getType()));
					continue;
				}

				// If the tax would put the town over the bank cap we reduce what will be
				// paid by the plot owner to what will be allowed.
				if (town.getBankCap() != 0 && tax + town.getAccount().getHoldingBalance() > town.getBankCap())
					tax = town.getBankCap() - town.getAccount().getHoldingBalance();

				if (tax == 0)
					continue;

				if (!resident.getAccount().payTo(tax, town, String.format("Plot Tax (%s)", townBlock.getType()))) {
					if (!lostPlots.contains(resident.getName()))
						lostPlots.add(resident.getName());

					townBlock.setResident(null);

					// Set the plot price.
					if (TownySettings.doesPlotTaxNonPaymentSetPlotForSale())
						townBlock.setPlotPrice(town.getPlotTypePrice(townBlock.getType()));
					else
						townBlock.setPlotPrice(-1);

					// Set the plot permissions to mirror the towns.
					townBlock.setType(townBlock.getType());

					townBlock.save();
				}
			}
		}

		if (lostPlots != null && !lostPlots.isEmpty()) {
			if (lostPlots.size() == 1) 
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_plot_taxes", lostPlots.get(0)));
			else
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_plot_taxes_multiple").append(StringMgmt.join(lostPlots, ", ")));
		}
	}

	/**
	 * Collect or pay upkeep for all towns.
	 */
	public void collectTownCosts() {
		List<Town> towns = new ArrayList<>(universe.getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();

			/*
			 * Only charge/pay upkeep for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (universe.hasTown(town.getName()) && town.hasUpkeep() && !town.isRuined()) {

				double upkeep = TownySettings.getTownUpkeepCost(town);
				double upkeepPenalty = TownySettings.getTownPenaltyUpkeepCost(town);
				if (upkeepPenalty > 0 && upkeep > 0)
					upkeep = upkeep + upkeepPenalty;
			
				totalTownUpkeep = totalTownUpkeep + upkeep;
				if (upkeep > 0) {
					
					if (town.getAccount().canPayFromHoldings(upkeep)) {
					// Town is able to pay the upkeep.
						town.getAccount().withdraw(upkeep, "Town Upkeep");
						TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_payed_upkeep", TownyEconomyHandler.getFormattedBalance(upkeep)));
					} else {
					// Town is unable to pay the upkeep.
						if (!TownySettings.isTownBankruptcyEnabled()) {
						// Bankruptcy is disabled, remove the town for not paying upkeep.
							TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_upkeep", TownyEconomyHandler.getFormattedBalance(upkeep)));
							universe.getDataSource().removeTown(town);
							removedTowns.add(town.getName());
							continue;
						}
						
						// Bankruptcy is enabled.
						boolean townWasBankrupt = town.isBankrupt();
						town.getAccount().setDebtCap(MoneyUtil.getEstimatedValueOfTown(town));
					
						if (town.getAccount().getHoldingBalance() - upkeep < town.getAccount().getDebtCap() * -1) {
						// The town will exceed their debt cap to pay the upkeep.
						// Eventually when the cap is reached they will pay 0 upkeep.
												
							if (TownySettings.isUpkeepDeletingTownsThatReachDebtCap()) {
							// Alternatively, if configured, towns will not be allowed to exceed
							// their debt and be deleted from the server for non-payment finally.
								TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_upkeep", TownyEconomyHandler.getFormattedBalance(upkeep)));
								universe.getDataSource().removeTown(town);
								removedTowns.add(town.getName());
								continue;
							}
							upkeep = town.getAccount().getDebtCap() - Math.abs(town.getAccount().getHoldingBalance());
						}
						
						// Finally pay the upkeep or the modified upkeep up to the debtcap. 
						town.getAccount().withdraw(upkeep, "Town Upkeep");
						TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_payed_upkeep_with_debt", TownyEconomyHandler.getFormattedBalance(upkeep)));
						
						// Check if the town was newly bankrupted and punish them for it.
						if(!townWasBankrupt) {
							town.setOpen(false);
							town.save();
							bankruptedTowns.add(town.getName());
						}
					}

					
				} else if (upkeep < 0) {
					// Negative upkeep
					upkeep = Math.abs(upkeep);
					
					if (TownySettings.isUpkeepPayingPlots()) {
						// Pay each plot owner a share of the negative
						// upkeep
						List<TownBlock> plots = new ArrayList<>(town.getTownBlocks());
						double payment = upkeep / plots.size();
						double townPayment = 0;

						for (TownBlock townBlock : plots) {
							if (townBlock.hasResident()) {
								Resident resident = townBlock.getResidentOrNull();
								if (resident != null)
									resident.getAccount().deposit(payment, "Negative Town Upkeep - Plot income");
							} else
								townPayment = townPayment + payment;

						}
						if (townPayment > 0)
							town.getAccount().deposit(townPayment, "Negative Town Upkeep - Plot income");

					} else {
						// Not paying plot owners so just pay the town
						town.getAccount().deposit(upkeep, "Negative Town Upkeep");
					}

				}
				
				// Charge towns for keeping a peaceful status.
				if (town.isNeutral()) {
					double neutralityCost = TownySettings.getTownNeutralityCost(town);
					if (neutralityCost > 0) {
						if ((town.isBankrupt() && !TownySettings.canBankruptTownsPayForNeutrality())
							|| !town.getAccount().withdraw(neutralityCost, "Town Peace Upkeep")) {
								town.setNeutral(false);
								town.save();
								TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_not_peaceful"));
							} else {
								TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_paid_for_neutral_status", TownyEconomyHandler.getFormattedBalance(neutralityCost)));
							}
						
					}
				}
			}			
		}

		String msg1 = "msg_bankrupt_town2";
		String msg2 = "msg_bankrupt_town_multiple";
		if(TownySettings.isTownBankruptcyEnabled() && TownySettings.isUpkeepDeletingTownsThatReachDebtCap()) {
				plugin.resetCache(); //Allow perms change to take effect immediately
				msg1 = "msg_town_reached_debtcap_and_is_disbanded";
				msg2 = "msg_town_reached_debtcap_and_is_disbanded_multiple";
		}
		
		if (bankruptedTowns != null && !bankruptedTowns.isEmpty())
			if (bankruptedTowns.size() == 1)
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_town_bankrupt_by_upkeep", bankruptedTowns.get(0)));
			else
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_town_bankrupt_by_upkeep_multiple").append(StringMgmt.join(bankruptedTowns, ", ")));
		if (removedTowns != null && !removedTowns.isEmpty())
			if (removedTowns.size() == 1)
				TownyMessaging.sendGlobalMessage(Translatable.of(msg1, removedTowns.get(0)));
			else
				TownyMessaging.sendGlobalMessage(Translatable.of(msg2).append(StringMgmt.join(removedTowns, ", ")));
	}

	/**
	 * Collect upkeep due from all nations.
	 */
	public void collectNationCosts() {
		List<Nation> nations = new ArrayList<>(universe.getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation;

		while (nationItr.hasNext()) {
			nation = nationItr.next();

			/*
			 * Only charge upkeep for this nation if it really still exists,
			 * and its capital town also pays upkeep costs.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (universe.hasNation(nation.getUUID()) && nation.getCapital().hasUpkeep()) {

				double upkeep = TownySettings.getNationUpkeepCost(nation);

				totalNationUpkeep = totalNationUpkeep + upkeep;
				if (upkeep > 0) {
					// Town is paying upkeep
					
					if (nation.getAccount().canPayFromHoldings(upkeep)) {
						nation.getAccount().withdraw(upkeep, "Nation Upkeep");
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_your_nation_payed_upkeep", TownyEconomyHandler.getFormattedBalance(upkeep)));						
					} else {
						TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_your_nation_couldnt_pay_upkeep", TownyEconomyHandler.getFormattedBalance(upkeep)));
						universe.getDataSource().removeNation(nation);
						removedNations.add(nation.getName());
					}
				} else if (upkeep < 0) {
					nation.getAccount().withdraw(upkeep, "Negative Nation Upkeep");
				}

				// Charge nations for keeping a peaceful status.
				if (nation.isNeutral()) {
					double neutralityCost = TownySettings.getNationNeutralityCost(nation);
					if (neutralityCost > 0) {
						if (!nation.getAccount().withdraw(neutralityCost, "Nation Peace Upkeep")) {
							nation.setNeutral(false);
							nation.save();
							TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_not_peaceful"));
						} else {
							TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_paid_for_neutral_status", TownyEconomyHandler.getFormattedBalance(neutralityCost)));
						}
					}
				}
			}
		}
		if (removedNations != null && !removedNations.isEmpty()) {
			if (removedNations.size() == 1)
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_bankrupt_nation2", removedNations.get(0)));
			else
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_bankrupt_nation_multiple").append(StringMgmt.join(removedNations, ", ")));
		}
	}
}
