package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.EmptyNationException;
import com.palmergames.bukkit.towny.exceptions.EmptyTownException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.permissions.TownyPerms;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import static com.palmergames.bukkit.towny.object.TownyObservableType.COLLECTED_NATION_TAX;
import static com.palmergames.bukkit.towny.object.TownyObservableType.COLLECTED_TONW_TAX;
import static com.palmergames.bukkit.towny.object.TownyObservableType.UPKEEP_NATION;
import static com.palmergames.bukkit.towny.object.TownyObservableType.UPKEEP_TOWN;

public class DailyTimerTask extends TownyTimerTask {

	public DailyTimerTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {

		long start = System.currentTimeMillis();

		TownyMessaging.sendDebugMsg("New Day");

		// Collect taxes
		if (TownyEconomyHandler.isActive() && TownySettings.isTaxingDaily()) {
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day_tax")));
			try {
				TownyMessaging.sendDebugMsg("Collecting Town Taxes");
				collectTownTaxes();
				TownyMessaging.sendDebugMsg("Collecting Nation Taxes");
				collectNationTaxes();
				TownyMessaging.sendDebugMsg("Collecting Town Costs");
				collectTownCosts();
				TownyMessaging.sendDebugMsg("Collecting Nation Costs");
				collectNationCosts();
			} catch (EconomyException e) {
			} catch (TownyException e) {
				// TODO king exception
				e.printStackTrace();
			}
		} else
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day")));

		// Automatically delete old residents
		if (TownySettings.isDeletingOldResidents()) {
			// Run a purge in it's own thread
			new ResidentPurge(plugin, null, TownySettings.getDeleteTime() * 1000).start();
		}

		// Backups
		TownyMessaging.sendDebugMsg("Cleaning up old backups.");
		TownyUniverse.getDataSource().cleanupBackups();
		if (TownySettings.isBackingUpDaily())
			try {
				TownyMessaging.sendDebugMsg("Making backup.");
				TownyUniverse.getDataSource().backup();
			} catch (IOException e) {
				TownyMessaging.sendErrorMsg("Could not create backup.");
				e.printStackTrace();
			}

		TownyMessaging.sendDebugMsg("Finished New Day Code");
		TownyMessaging.sendDebugMsg("Universe Stats:");
		TownyMessaging.sendDebugMsg("    Residents: " + TownyUniverse.getDataSource().getResidents().size());
		TownyMessaging.sendDebugMsg("    Towns: " + TownyUniverse.getDataSource().getTowns().size());
		TownyMessaging.sendDebugMsg("    Nations: " + TownyUniverse.getDataSource().getNations().size());
		for (TownyWorld world : TownyUniverse.getDataSource().getWorlds())
			TownyMessaging.sendDebugMsg("    " + world.getName() + " (townblocks): " + world.getTownBlocks().size());

		TownyMessaging.sendDebugMsg("Memory (Java Heap):");
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (max)", Runtime.getRuntime().maxMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (total)", Runtime.getRuntime().totalMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (free)", Runtime.getRuntime().freeMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (used=total-free)", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024));
		TownyMessaging.sendDebugMsg("newDay took " + (System.currentTimeMillis() - start) + "ms");
	}

	/**
	 * Collect taxes for all nations due from their member towns
	 * 
	 * @throws EconomyException - EconomyException
	 */
	public void collectNationTaxes() throws EconomyException {

		List<Nation> nations = new ArrayList<Nation>(TownyUniverse.getDataSource().getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation = null;

		while (nationItr.hasNext()) {
			nation = nationItr.next();
			/*
			 * Only collect tax for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (TownyUniverse.getDataSource().hasNation(nation.getName()))
				collectNationTaxes(nation);
		}
		universe.setChangedNotify(COLLECTED_NATION_TAX);
	}

	/**
	 * Collect taxes due to the nation from it's member towns.
	 * 
	 * @param nation - Nation to collect taxes from.
	 * @throws EconomyException - EconomyException
	 */
	protected void collectNationTaxes(Nation nation) throws EconomyException {

		if (nation.getTaxes() > 0) {

			List<Town> towns = new ArrayList<Town>(nation.getTowns());
			ListIterator<Town> townItr = towns.listIterator();
			Town town = null;

			while (townItr.hasNext()) {
				town = townItr.next();

				/*
				 * Only collect nation tax from this town if it really still
				 * exists.
				 * We are running in an Async thread so MUST verify all objects.
				 */
				if (TownyUniverse.getDataSource().hasTown(town.getName())) {
					if (town.isCapital() || !town.hasUpkeep())
						continue;
					if (!town.payTo(nation.getTaxes(), nation, "Nation Tax")) {
						try {
							TownyMessaging.sendNationMessage(nation, TownySettings.getCouldntPayTaxesMsg(town, "nation"));
							nation.removeTown(town);
						} catch (EmptyNationException e) {
							// Always has 1 town (capital) so ignore
						} catch (NotRegisteredException e) {
						}
						TownyUniverse.getDataSource().saveTown(town);
						TownyUniverse.getDataSource().saveNation(nation);
					}
				} else
					TownyMessaging.sendTownMessage(town, TownySettings.getPayedTownTaxMsg() + nation.getTaxes());
			}
		}

	}

	/**
	 * Collect taxes for all towns due from their residents.
	 * 
	 * @throws EconomyException - EconomyException
	 */
	public void collectTownTaxes() throws EconomyException {

		List<Town> towns = new ArrayList<Town>(TownyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town = null;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only collect resident tax for this town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (TownyUniverse.getDataSource().hasTown(town.getName()))
				collectTownTaxes(town);
		}
		universe.setChangedNotify(COLLECTED_TONW_TAX);
	}

	/**
	 * Collect taxes due to the town from it's residents.
	 * 
	 * @param town - Town to collect taxes from
	 * @throws EconomyException - EconomyException
	 */
	protected void collectTownTaxes(Town town) throws EconomyException {

		// Resident Tax
		if (town.getTaxes() > 0) {

			List<Resident> residents = new ArrayList<Resident>(town.getResidents());
			ListIterator<Resident> residentItr = residents.listIterator();
			Resident resident = null;

			while (residentItr.hasNext()) {
				resident = residentItr.next();

				/*
				 * Only collect resident tax from this resident if it really
				 * still exists. We are running in an Async thread so MUST
				 * verify all objects.
				 */
				if (TownyUniverse.getDataSource().hasResident(resident.getName())) {

					if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC()) {
						try {
							TownyMessaging.sendResidentMessage(resident, TownySettings.getTaxExemptMsg());
						} catch (TownyException e) {
							// Player is not online
						}
						continue;
					} else if (town.isTaxPercentage()) {
						double cost = resident.getHoldingBalance() * town.getTaxes() / 100;
						resident.payTo(cost, town, "Town Tax (Percentage)");
						/*
						 * Don't send individual message anymore to ease up on
						 * the lag. try {
						 * TownyMessaging.sendResidentMessage(resident,
						 * TownySettings.getPayedResidentTaxMsg() + cost); }
						 * catch (TownyException e) { // Player is not online }
						 */
					} else if (!resident.payTo(town.getTaxes(), town, "Town Tax")) {
						TownyMessaging.sendTownMessage(town, TownySettings.getCouldntPayTaxesMsg(resident, "town"));
						
						try {
							
							// reset this resident and remove him from the town.
							resident.clear();
							TownyUniverse.getDataSource().saveTown(town);
							
						} catch (EmptyTownException e) {
							
							// No mayor so remove the town.
							TownyUniverse.getDataSource().removeTown(town);
							
						}
						
						TownyUniverse.getDataSource().saveResident(resident);
						
					}// else
					/*
					 * Don't send individual message anymore to ease up on the
					 * lag. try { TownyMessaging.sendResidentMessage(resident,
					 * TownySettings.getPayedResidentTaxMsg() +
					 * town.getTaxes()); } catch (TownyException e) { // Player
					 * is not online }
					 */
				}
			}
		}

		// Plot Tax
		if (town.getPlotTax() > 0 || town.getCommercialPlotTax() > 0 || town.getEmbassyPlotTax() > 0) {
			// Hashtable<Resident, Integer> townPlots = new Hashtable<Resident,
			// Integer>();
			// Hashtable<Resident, Double> townTaxes = new Hashtable<Resident,
			// Double>();

			List<TownBlock> townBlocks = new ArrayList<TownBlock>(town.getTownBlocks());
			ListIterator<TownBlock> townBlockItr = townBlocks.listIterator();
			TownBlock townBlock = null;

			while (townBlockItr.hasNext()) {
				townBlock = townBlockItr.next();

				if (!townBlock.hasResident())
					continue;
				try {
					Resident resident = townBlock.getResident();

					/*
					 * Only collect plot tax from this resident if it really
					 * still exists. We are running in an Async thread so MUST
					 * verify all objects.
					 */
					if (TownyUniverse.getDataSource().hasResident(resident.getName())) {
						if (resident.hasTown())
							if (resident.getTown() == townBlock.getTown())
								if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC())
									continue;
							
						if (!resident.payTo(townBlock.getType().getTax(town), town, String.format("Plot Tax (%s)", townBlock.getType()))) {
							TownyMessaging.sendTownMessage(town, String.format(TownySettings.getLangString("msg_couldnt_pay_plot_taxes"), resident));

							townBlock.setResident(null);
							townBlock.setPlotPrice(-1);

							// Set the plot permissions to mirror the towns.
							townBlock.setType(townBlock.getType());

							TownyUniverse.getDataSource().saveResident(resident);
							TownyUniverse.getDataSource().saveTownBlock(townBlock);
						}// else {
							// townPlots.put(resident,
							// (townPlots.containsKey(resident) ?
							// townPlots.get(resident) : 0) + 1);
							// townTaxes.put(resident,
							// (townTaxes.containsKey(resident) ?
							// townTaxes.get(resident) : 0) +
							// townBlock.getType().getTax(town));
							// }
					}
				} catch (NotRegisteredException e) {
				}
			}
			/*
			 * Don't send individual message anymore to ease up on the lag. for
			 * (Resident resident : townPlots.keySet()) { try { int numPlots =
			 * townPlots.get(resident); double totalCost =
			 * townTaxes.get(resident);
			 * TownyMessaging.sendResidentMessage(resident,
			 * String.format(TownySettings.getLangString("msg_payed_plot_cost"),
			 * totalCost, numPlots, town.getName())); } catch (TownyException e)
			 * { // Player is not online } }
			 */

		}
	}

	/**
	 * Collect or pay upkeep for all towns.
	 * 
	 * @throws EconomyException
	 * @throws TownyException
	 */
	public void collectTownCosts() throws EconomyException, TownyException {

		List<Town> towns = new ArrayList<Town>(TownyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town = null;

		while (townItr.hasNext()) {
			town = townItr.next();

			/*
			 * Only charge/pay upkeep for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (TownyUniverse.getDataSource().hasTown(town.getName())) {

				if (town.hasUpkeep()) {
					double upkeep = TownySettings.getTownUpkeepCost(town);
				
					if (upkeep > 0) {
						// Town is paying upkeep
						if (!town.pay(upkeep, "Town Upkeep")) {
							TownyUniverse.getDataSource().removeTown(town);
							TownyMessaging.sendGlobalMessage(town.getName() + TownySettings.getLangString("msg_bankrupt_town"));
						}
					} else if (upkeep < 0) {
						// Negative upkeep
						if (TownySettings.isUpkeepPayingPlots()) {
							// Pay each plot owner a share of the negative
							// upkeep
							List<TownBlock> plots = new ArrayList<TownBlock>(town.getTownBlocks());

							for (TownBlock townBlock : plots) {
								if (townBlock.hasResident())
									townBlock.getResident().pay((upkeep / plots.size()), "Negative Town Upkeep - Plot income");
								else
									town.pay((upkeep / plots.size()), "Negative Town Upkeep - Plot income");
							}

						} else {
							// Not paying plot owners so just pay the town
							town.pay(upkeep, "Negative Town Upkeep");
						}

					}
				}
			}
		}

		universe.setChangedNotify(UPKEEP_TOWN);
	}

	/**
	 * Collect upkeep due from all nations.
	 * 
	 * @throws EconomyException
	 */
	public void collectNationCosts() throws EconomyException {

		List<Nation> nations = new ArrayList<Nation>(TownyUniverse.getDataSource().getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation = null;

		while (nationItr.hasNext()) {
			nation = nationItr.next();

			/*
			 * Only charge upkeep for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (TownyUniverse.getDataSource().hasNation(nation.getName())) {

				double upkeep = TownySettings.getNationUpkeepCost(nation);

				if (upkeep > 0) {
					// Town is paying upkeep

					if (!nation.pay(TownySettings.getNationUpkeepCost(nation), "Nation Upkeep")) {
						TownyUniverse.getDataSource().removeNation(nation);
						TownyMessaging.sendGlobalMessage(nation.getName() + TownySettings.getLangString("msg_bankrupt_nation"));
					}
					if (nation.isNeutral()) {
						if (!nation.pay(TownySettings.getNationNeutralityCost(), "Nation Peace Upkeep")) {
							try {
								nation.setNeutral(false);
							} catch (TownyException e) {
								e.printStackTrace();
							}
							TownyUniverse.getDataSource().saveNation(nation);
							TownyMessaging.sendNationMessage(nation, TownySettings.getLangString("msg_nation_not_peaceful"));
						}
					}
					
				} else if (upkeep < 0) {

					nation.pay(upkeep, "Negative Nation Upkeep");

				}
			}
		}

		universe.setChangedNotify(UPKEEP_NATION);
	}
}
