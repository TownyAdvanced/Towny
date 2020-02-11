package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.*;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.exceptions.*;
import com.palmergames.bukkit.towny.object.*;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.RemoveRuinedTowns;
import com.palmergames.bukkit.towny.war.siegewar.timeractions.UpdateTownNeutralityCounters;
import com.palmergames.bukkit.util.ChatTools;
import org.bukkit.Bukkit;
import org.bukkit.scheduler.BukkitRunnable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

public class DailyTimerTask extends TownyTimerTask {
	
	private double totalTownUpkeep = 0.0;
	private double totalNationUpkeep = 0.0;
	private List<String> removedTowns = new ArrayList<>();
	private List<String> removedNations = new ArrayList<>();

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

				Bukkit.getServer().getPluginManager().callEvent(new NewDayEvent(removedTowns, removedNations, totalTownUpkeep, totalNationUpkeep, start));
				
			} catch (EconomyException ignored) {
				System.out.println("Economy Exception");
			} catch (TownyException e) {
				// TODO king exception
				e.printStackTrace();
			}
		} else
			TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_new_day")));

		// Automatically delete old residents
		if (TownySettings.isDeletingOldResidents()) {
			// Run a purge in it's own thread
			new ResidentPurge(plugin, null, TownySettings.getDeleteTime() * 1000, TownySettings.isDeleteTownlessOnly()).start();
		}

		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		
		// Reduce jailed residents jail time
		if (!townyUniverse.getJailedResidentMap().isEmpty()) {
			for (Resident resident : townyUniverse.getJailedResidentMap()) {
				if (resident.hasJailDays()) {
					if (resident.getJailDays() == 1) {
						resident.setJailDays(0);
						new BukkitRunnable() {

				            @Override
				            public void run() {				            	
				            	Town jailTown = null;
								try {
									jailTown = townyUniverse.getDataSource().getTown(resident.getJailTown());
								} catch (NotRegisteredException ignored) {
								}
								int index = resident.getJailSpawn();
				            	resident.setJailed(resident , index, jailTown);
				            }
				            
				        }.runTaskLater(this.plugin, 20);
					} else 
						resident.setJailDays(resident.getJailDays() - 1);
					
				}
				townyUniverse.getDataSource().saveResident(resident);
			}			
		}
		
		// Reduce conquered towns' conqueredDays
		for (Town towns : TownyUniverse.getInstance().getDataSource().getTowns()) {
			if (towns.isConquered()) {
				if (towns.getConqueredDays() == 1) {
					towns.setConquered(false);
					towns.setConqueredDays(0);
				} else
					towns.setConqueredDays(towns.getConqueredDays() - 1);				
			}
		}

		//Update town neutrality counters
		if(TownySettings.getWarSiegeEnabled() && TownySettings.getWarSiegeTownNeutralityEnabled()) 
			UpdateTownNeutralityCounters.updateTownNeutralityCounters();	
		
		// Backups
		TownyMessaging.sendDebugMsg("Cleaning up old backups.");

		townyUniverse.getDataSource().cleanupBackups();
		if (TownySettings.isBackingUpDaily())
			try {
				TownyMessaging.sendDebugMsg("Making backup.");
				townyUniverse.getDataSource().backup();
			} catch (IOException e) {
				TownyMessaging.sendErrorMsg("Could not create backup.");
				e.printStackTrace();
			}

		TownyMessaging.sendDebugMsg("Finished New Day Code");
		TownyMessaging.sendDebugMsg("Universe Stats:");
		TownyMessaging.sendDebugMsg("    Residents: " + townyUniverse.getDataSource().getResidents().size());
		TownyMessaging.sendDebugMsg("    Towns: " + townyUniverse.getDataSource().getTowns().size());
		TownyMessaging.sendDebugMsg("    Nations: " + townyUniverse.getDataSource().getNations().size());
		for (TownyWorld world : townyUniverse.getDataSource().getWorlds())
			TownyMessaging.sendDebugMsg("    " + world.getName() + " (townblocks): " + world.getTownBlocks().size());

		TownyMessaging.sendDebugMsg("Memory (Java Heap):");
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (max)", Runtime.getRuntime().maxMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (total)", Runtime.getRuntime().totalMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (free)", Runtime.getRuntime().freeMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (used=total-free)", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024));
		System.out.println("Towny DailyTimerTask took " + (System.currentTimeMillis() - start) + "ms to process.");
	}

	/**
	 * Collect taxes for all nations due from their member towns
	 * 
	 * @throws EconomyException - EconomyException
	 */
	public void collectNationTaxes() throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Nation> nations = new ArrayList<>(townyUniverse.getDataSource().getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation;

		while (nationItr.hasNext()) {
			nation = nationItr.next();
			/*
			 * Only collect tax for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasNation(nation.getName()))
				collectNationTaxes(nation);
		}
	}

	/**
	 * Collect taxes due to the nation from it's member towns.
	 * 
	 * @param nation - Nation to collect taxes from.
	 * @throws EconomyException - EconomyException
	 */
	protected void collectNationTaxes(Nation nation) throws EconomyException {
		if (nation.getTaxes() > 0) {

			List<Town> towns = new ArrayList<>(nation.getTowns());
			ListIterator<Town> townItr = towns.listIterator();
			Town town;
			TownyUniverse townyUniverse = TownyUniverse.getInstance();

			while (townItr.hasNext()) {
				town = townItr.next();

				/*
				 * Only collect nation tax from this town if 
				 * - It exists
				 * - It is not the capital
				 * - It is not ruined
				 * - It is not neutral
				 * 
				 * We are running in an Async thread so MUST verify all objects.
				 */
				if (townyUniverse.getDataSource().hasTown(town.getName())) {
					if (town.isCapital()
						|| !town.hasUpkeep()
						|| town.isRuined()
						|| (TownySettings.getWarSiegeEnabled() && TownySettings.getWarSiegeTownNeutralityEnabled() && town.isNeutral())) {
						continue;
					}

					if (!town.getAccount().payTo(nation.getTaxes(), nation, "Nation Tax")) {
						try {
							/*
							If involuntary town occupation is enabled,
							town taxes are treated as mandatory, like town upkeep.
							 */
							if (TownySettings.getWarSiegeEnabled() && TownySettings.getWarSiegeInvadeEnabled()) {
								universe.getDataSource().removeTown(town);
								TownyMessaging.sendGlobalMessage(town.getName() + TownySettings.getLangString("msg_bankrupt_town"));
							} else {
								removedTowns.add(town.getName());
								nation.removeTown(town);
							}

						} catch (EmptyNationException e) {
							// Always has 1 town (capital) so ignore
						} catch (NotRegisteredException ignored) {
						}
						townyUniverse.getDataSource().saveTown(town);
						townyUniverse.getDataSource().saveNation(nation);
					}
				} else
					TownyMessaging.sendPrefixedTownMessage(town, TownySettings.getPayedTownTaxMsg() + nation.getTaxes());
			}
			if (removedTowns != null) {
				if (removedTowns.size() == 1) 
					TownyMessaging.sendNationMessagePrefixed(nation, String.format(TownySettings.getLangString("msg_couldnt_pay_tax"), ChatTools.list(removedTowns), "nation"));
				else
					TownyMessaging.sendNationMessagePrefixed(nation, ChatTools.list(removedTowns, TownySettings.getLangString("msg_couldnt_pay_nation_tax_multiple")));
			}
		}

	}

	/**
	 * Collect taxes for all towns due from their residents.
	 * 
	 * @throws EconomyException - EconomyException
	 */
	public void collectTownTaxes() throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();

		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();
			/*
			 * Only collect resident tax for this town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName()) && !town.isRuined())
				collectTownTaxes(town);
		}
	}

	/**
	 * Collect taxes due to the town from it's residents.
	 * 
	 * @param town - Town to collect taxes from
	 * @throws EconomyException - EconomyException
	 */
	protected void collectTownTaxes(Town town) throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		// Resident Tax
		if (town.getTaxes() > 0) {

			List<Resident> residents = new ArrayList<>(town.getResidents());
			ListIterator<Resident> residentItr = residents.listIterator();
			List<String> removedResidents = new ArrayList<>();
			Resident resident;

			while (residentItr.hasNext()) {
				resident = residentItr.next();

				/*
				 * Only collect resident tax from this resident if it really
				 * still exists. We are running in an Async thread so MUST
				 * verify all objects.
				 */
				if (townyUniverse.getDataSource().hasResident(resident.getName())) {

					if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC()) {
						try {
							TownyMessaging.sendResidentMessage(resident, TownySettings.getTaxExemptMsg());
						} catch (TownyException e) {
							// Player is not online
						}
						continue;
					} else if (town.isTaxPercentage()) {
						double cost = resident.getAccount().getHoldingBalance() * town.getTaxes() / 100;
						resident.getAccount().payTo(cost, town, "Town Tax (Percentage)");
					} else if (!resident.getAccount().payTo(town.getTaxes(), town, "Town Tax")) {
						removedResidents.add(resident.getName());
						try {
							
							// reset this resident and remove him from the town.
							resident.clear();
							townyUniverse.getDataSource().saveTown(town);
							
						} catch (EmptyTownException e) {
							
							// No mayor so remove the town.
							townyUniverse.getDataSource().removeTown(town);
							
						}
						
						townyUniverse.getDataSource().saveResident(resident);
						
					}
				}
			}
			if (removedResidents != null) {
				if (removedResidents.size() == 1) 
					TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_couldnt_pay_tax"), ChatTools.list(removedResidents), "town"));
				else
					TownyMessaging.sendPrefixedTownMessage(town, ChatTools.list(removedResidents, TownySettings.getLangString("msg_couldnt_pay_town_tax_multiple")));
			}
		}

		// Plot Tax
		if (town.getPlotTax() > 0 || town.getCommercialPlotTax() > 0 || town.getEmbassyPlotTax() > 0) {

			List<TownBlock> townBlocks = new ArrayList<>(town.getTownBlocks());
			List<String> lostPlots = new ArrayList<>();
			ListIterator<TownBlock> townBlockItr = townBlocks.listIterator();
			TownBlock townBlock;

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
					if (townyUniverse.getDataSource().hasResident(resident.getName())) {
						if (resident.hasTown())
							if (resident.getTown() == townBlock.getTown())
								if (TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt") || resident.isNPC())
									continue;
							
						if (!resident.getAccount().payTo(townBlock.getType().getTax(town), town, String.format("Plot Tax (%s)", townBlock.getType()))) {
							if (!lostPlots.contains(resident.getName()))
									lostPlots.add(resident.getName());

							townBlock.setResident(null);
							townBlock.setPlotPrice(-1);

							// Set the plot permissions to mirror the towns.
							townBlock.setType(townBlock.getType());
							
							townyUniverse.getDataSource().saveResident(resident);
							townyUniverse.getDataSource().saveTownBlock(townBlock);
						}
					}
				} catch (NotRegisteredException ignored) {
				}
				
			}
			if (lostPlots != null) {
				if (lostPlots.size() == 1) 
					TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_couldnt_pay_plot_taxes"), ChatTools.list(lostPlots)));
				else
					TownyMessaging.sendPrefixedTownMessage(town, ChatTools.list(lostPlots, TownySettings.getLangString("msg_couldnt_pay_plot_taxes_multiple")));
			}
		}
	}

	/**
	 * Collect or pay upkeep for all towns.
	 * 
	 * @throws EconomyException if there is an error with the economy handling
	 * @throws TownyException if there is a error with Towny
	 */
	public void collectTownCosts() throws EconomyException, TownyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Town> towns = new ArrayList<>(townyUniverse.getDataSource().getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();

			/*
			 * Only charge/pay upkeep for this town if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasTown(town.getName())) {

				if (town.hasUpkeep() && !town.isRuined()) {
					double upkeep = TownySettings.getTownUpkeepCost(town);
					double upkeepPenalty = TownySettings.getTownPenaltyUpkeepCost(town);
					if (upkeepPenalty > 0 && upkeep > 0)
						upkeep = upkeep + upkeepPenalty;
				
					totalTownUpkeep = totalTownUpkeep + upkeep;
					if (upkeep > 0) {
						// Town is paying upkeep
						if (!town.getAccount().pay(upkeep, "Town Upkeep")) {
							townyUniverse.getDataSource().removeTown(town);
							removedTowns.add(town.getName());
						}
					} else if (upkeep < 0) {						
						// Negative upkeep
						if (TownySettings.isUpkeepPayingPlots()) {
							// Pay each plot owner a share of the negative
							// upkeep
							List<TownBlock> plots = new ArrayList<>(town.getTownBlocks());

							for (TownBlock townBlock : plots) {
								if (townBlock.hasResident())
									townBlock.getResident().getAccount().pay((upkeep / plots.size()), "Negative Town Upkeep - Plot income");
								else
									town.getAccount().pay((upkeep / plots.size()), "Negative Town Upkeep - Plot income");
							}

						} else {
							// Not paying plot owners so just pay the town
							town.getAccount().pay(upkeep, "Negative Town Upkeep");
						}

					}
				}
			}			
		}
		if (removedTowns != null) {
			if (removedTowns.size() == 1) 
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_bankrupt_town2"),ChatTools.list(removedTowns)));
			else
				TownyMessaging.sendGlobalMessage(ChatTools.list(removedTowns, TownySettings.getLangString("msg_bankrupt_town_multiple")));
		}	
	}

	/**
	 * Collect upkeep due from all nations.
	 * 
	 * @throws EconomyException if there is an error with Economy handling
	 */
	public void collectNationCosts() throws EconomyException {
		TownyUniverse townyUniverse = TownyUniverse.getInstance();
		List<Nation> nations = new ArrayList<>(townyUniverse.getDataSource().getNations());
		ListIterator<Nation> nationItr = nations.listIterator();
		Nation nation;

		while (nationItr.hasNext()) {
			nation = nationItr.next();

			/*
			 * Only charge upkeep for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (townyUniverse.getDataSource().hasNation(nation.getName())) {

				double upkeep = TownySettings.getNationUpkeepCost(nation);

				totalNationUpkeep = totalNationUpkeep + upkeep;
				if (upkeep > 0) {
					// Town is paying upkeep

					if (!nation.getAccount().pay(TownySettings.getNationUpkeepCost(nation), "Nation Upkeep")) {
						townyUniverse.getDataSource().removeNation(nation);
						removedNations.add(nation.getName());
					}
					if (nation.isNeutral()) {
						if (!nation.getAccount().pay(TownySettings.getNationNeutralityCost(), "Nation Peace Upkeep")) {
							try {
								nation.setNeutral(false);
							} catch (TownyException e) {
								e.printStackTrace();
							}
							townyUniverse.getDataSource().saveNation(nation);
							TownyMessaging.sendPrefixedNationMessage(nation, TownySettings.getLangString("msg_nation_not_peaceful"));
						}
					}
					
				} else if (upkeep < 0) {
					nation.getAccount().pay(upkeep, "Negative Nation Upkeep");
				}
			}
		}
		if (removedNations != null) {
			if (removedNations.size() == 1) 
				TownyMessaging.sendGlobalMessage(String.format(TownySettings.getLangString("msg_bankrupt_nation2"), ChatTools.list(removedNations)));
			else
				TownyMessaging.sendGlobalMessage(ChatTools.list(removedNations, TownySettings.getLangString("msg_bankrupt_nation_multiple")));
		}
	}
}
