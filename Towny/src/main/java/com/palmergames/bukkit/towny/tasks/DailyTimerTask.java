package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.DeleteTownEvent;
import com.palmergames.bukkit.towny.event.DeleteNationEvent;
import com.palmergames.bukkit.towny.event.NewDayEvent;
import com.palmergames.bukkit.towny.event.PreNewDayEvent;
import com.palmergames.bukkit.towny.event.time.dailytaxes.NewDayTaxAndUpkeepPreCollectionEvent;
import com.palmergames.bukkit.towny.event.time.dailytaxes.PreTownPaysNationTaxEvent;
import com.palmergames.bukkit.towny.event.time.dailytaxes.TownPaysNationConqueredTaxEvent;
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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.entity.Player;

public class DailyTimerTask extends TownyTimerTask {
	private static final Object NEW_DAY_LOCK = new Object();
	
	private double totalTownUpkeep = 0.0;
	private double totalNationUpkeep = 0.0;
	private double taxCollected = 0.0;
	private double conqueredTaxCollected = 0.0;
	private final List<String> bankruptedTowns = new ArrayList<>();
	private final List<String> removedTowns = new ArrayList<>();
	private final List<String> removedNations = new ArrayList<>();
	private final Map<Resident, Map<Town, Double>> residentPlotTaxMap = new ConcurrentHashMap<>();

	public DailyTimerTask(Towny plugin) {

		super(plugin);
	}

	@Override
	public void run() {
		synchronized (NEW_DAY_LOCK) {
			doNewDay();
		}
	}

	public void doNewDay() {

		long start = System.currentTimeMillis();
		totalTownUpkeep = 0.0;
		totalNationUpkeep = 0.0;
		taxCollected = 0.0;
		bankruptedTowns.clear();
		removedTowns.clear();
		removedNations.clear();
		residentPlotTaxMap.clear();

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
			plugin.getScheduler().runAsync(new ResidentPurge(null, TownySettings.getDeleteTime() * 1000, TownySettings.isDeleteTownlessOnly(), null));
		}
		
		//Clean up unused NPC residents
		plugin.getScheduler().runAsync(new NPCCleanupTask());

		/*
		 * If enabled, remove all 0-plot towns.
		 */
		if (TownySettings.isNewDayDeleting0PlotTowns()) {
			List<String> deletedTowns = new ArrayList<>();
			for (Town town : universe.getTowns()) {
				if (!town.exists())
					continue;
				if (town.getTownBlocks().isEmpty()) {
					deletedTowns.add(town.getName());
					removedTowns.add(town.getName());
					universe.getDataSource().removeTown(town, DeleteTownEvent.Cause.NO_TOWNBLOCKS);
				}
			}
			if (!deletedTowns.isEmpty())
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_the_following_towns_were_deleted_for_having_0_claims", String.join(", ", deletedTowns)));
		}
		
		/*
		 * Reduce the number of days conquered towns are conquered for.
		 */
		for (Town town : universe.getTowns()) {
			if (!town.exists())
				continue;
			if (town.isConquered()) {
				if (town.getConqueredDays() == 1)
					plugin.getScheduler().run(() -> unconquer(town));
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
			TownyMessaging.sendDebugMsg("    " + world.getName() + " (townblocks): " + world.getTownBlocks().size());

		TownyMessaging.sendDebugMsg("Memory (Java Heap):");
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (max)", Runtime.getRuntime().maxMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (total)", Runtime.getRuntime().totalMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (free)", Runtime.getRuntime().freeMemory() / 1024 / 1024));
		TownyMessaging.sendDebugMsg(String.format("%8d Mb (used=total-free)", (Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory()) / 1024 / 1024));
		Towny.getPlugin().getLogger().info("Towny DailyTimerTask took " + (System.currentTimeMillis() - start) + "ms to process.");
		
		// Run the new day scheduler again one minute later to begin scheduling the next New Day.
		if (!NewDayScheduler.isNewDaySchedulerRunning())
			plugin.getScheduler().runLater(new NewDayScheduler(plugin), 60 * 20);
	}

	private void unconquer(Town town) {
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
			taxCollected = 0.0;
			nation = nationItr.next();
			/*
			 * Only collect tax for this nation if it really still exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (nation.exists())
				collectNationTaxes(nation);
		}
	}

	/**
	 * Collect taxes due to the nation from it's member towns.
	 * 
	 * @param nation - Nation to collect taxes from.
	 */
	protected void collectNationTaxes(Nation nation) {

		double tax = nation.getTaxes();
		if (tax == 0)
			return;

		// Don't allow negative taxes if the nation uses a tax percentage or negative naiton tax is explicitly disallowed.
		if (tax < 0 && (!TownySettings.isNegativeNationTaxAllowed() || nation.isTaxPercentage()))
			return;

		List<String> newlyDelinquentTowns = new ArrayList<>();
		List<String> localTownsDestroyed = new ArrayList<>();
		List<Town> towns = new ArrayList<>(nation.getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			town = townItr.next();

			/*
			 * Only collect nation tax from this town if it really still exists. We are
			 * running in an Async thread so MUST verify all objects.
			 */
			if (!town.exists())
				continue;

			if ((town.isCapital() && !TownySettings.doCapitalsPayNationTax()) || !town.hasUpkeep() || town.isRuined())
				continue;

			String result = processTownPaysNationTax(town, nation);
			if (!result.isEmpty())
				switch (result) {
				case "destroyed" -> localTownsDestroyed.add(town.getName());
				case "delinquent" -> newlyDelinquentTowns.add(town.getName());
				}
		}

		// Some towns were unable to pay the nation tax, send a message.
		if (newlyDelinquentTowns != null && !newlyDelinquentTowns.isEmpty()) {
			boolean bankruptcyenabled = TownySettings.isTownBankruptcyEnabled() && TownySettings.doBankruptTownsPayNationTax();
			String msg1 = bankruptcyenabled ? "msg_town_bankrupt_by_nation_tax" : "msg_couldnt_pay_tax";
			String msg2 = bankruptcyenabled ? "msg_town_bankrupt_by_nation_tax_multiple" : "msg_couldnt_pay_nation_tax_multiple";

			if (newlyDelinquentTowns.size() == 1)
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of(msg1, newlyDelinquentTowns.get(0), Translatable.of("nation_sing")));
			else
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of(msg2).append(StringMgmt.join(newlyDelinquentTowns, ", ")));
		}

		// Some towns were destroyed because they were conquered and could not pay and that sort of punishment has been configured, send a message.
		if (localTownsDestroyed != null && !localTownsDestroyed.isEmpty())
			if (localTownsDestroyed.size() == 1)
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_destroyed_by_nation_tax", localTownsDestroyed.get(0)));
			else
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_town_destroyed_by_nation_tax_multiple").append(StringMgmt.join(localTownsDestroyed, ", ")));

		if (taxCollected != 0.0) {
			String msgSlug = taxCollected > 0.0 ? "msg_tax_collected_from_towns" : "msg_tax_paid_to_towns";
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of(msgSlug, prettyMoney(taxCollected)));
			taxCollected = 0.0;
		}

		if (conqueredTaxCollected > 0.0) {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_tax_collected_from_conquered_towns", prettyMoney(conqueredTaxCollected)));
			conqueredTaxCollected = 0.0;
		}
	}

	private String processTownPaysNationTax(Town town, Nation nation) {
		double taxAmount = nation.getTaxes();
		double localConqueredTax = 0.0;

		if (nation.isTaxPercentage()) {
			taxAmount = town.getAccount().getHoldingBalance() * taxAmount / 100;
			taxAmount = Math.min(taxAmount, nation.getMaxPercentTaxAmount());
		}

		if (town.isConquered()) {
			TownPaysNationConqueredTaxEvent event = new TownPaysNationConqueredTaxEvent(town, nation, nation.getConqueredTax());
			if (!BukkitTools.isEventCancelled(event) && event.getConqueredTax() > 0) {
				localConqueredTax = event.getConqueredTax();
				taxAmount += localConqueredTax;
			}
		}

		PreTownPaysNationTaxEvent event = new PreTownPaysNationTaxEvent(town, nation, taxAmount);
		if (BukkitTools.isEventCancelled(event)) {
			TownyMessaging.sendPrefixedTownMessage(town, event.getCancelMessage());
			return "";
		}
		taxAmount = event.getTax();

		// Town is going to be paid if the nation can afford it.
		if (taxAmount < 0 && !town.isConquered()) {
			payNationTaxToTown(nation, town, taxAmount);
			return "";
		}

		// Handle if the bank cannot be paid because of the cap. It might be more than
		// the bank can accept, so we reduce it to the amount that the bank can accept,
		// even if it becomes 0.
		if (nation.getBankCap() != 0 && taxAmount + nation.getAccount().getHoldingBalance() > nation.getBankCap())
			taxAmount = nation.getBankCap() - nation.getAccount().getHoldingBalance();

		// This will stop towns paying $0 and stop conquered towns from getting a less-than-zero tax charge.
		if (taxAmount <= 0)
			return "";

		// Town is able to pay the nation's tax.
		if (town.getAccount().canPayFromHoldings(taxAmount)) {
			town.getAccount().payTo(taxAmount, nation, String.format("Nation Tax to %s paid by %s.", nation.getName(), town.getName()));
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_payed_nation_tax", prettyMoney(taxAmount)));
			taxCollected += taxAmount;
			if (localConqueredTax > 0)
				conqueredTaxCollected += localConqueredTax;
			return "";
		} 

		// Town is unable to pay the nation's tax.
		if (!TownySettings.isTownBankruptcyEnabled() || !TownySettings.doBankruptTownsPayNationTax()) {
		// Bankruptcy disabled, remove town for not paying nation tax, 
		// OR Bankruptcy enabled but towns aren't allowed to use debt to pay nation tax. 
			
			if (TownySettings.doesNationTaxDeleteConqueredTownsWhichCannotPay() && town.isConquered()) {
				universe.getDataSource().removeTown(town, DeleteTownEvent.Cause.UPKEEP);
				return "destroyed";
			}

			town.removeNation();
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_the_nation_tax_of", prettyMoney(taxAmount)));
			return "delinquent";
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
				town.removeNation();
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_the_nation_tax_of", prettyMoney(nation.getTaxes())));
				return "delinquent";
			}

			taxAmount = town.getAccount().getDebtCap() - Math.abs(town.getAccount().getHoldingBalance());
		}

		// Pay the nation tax with at least some amount of debt.
		town.getAccount().withdraw(taxAmount, String.format("Nation Tax paid to %s.", nation.getName())); // .withdraw() is used because other economy methods do not allow a town to go into debt.
		nation.getAccount().deposit(taxAmount, String.format("Nation Tax paid by %s.", town.getName()));
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_payed_nation_tax_with_debt", prettyMoney(taxAmount)));
		taxCollected += taxAmount;
		if (localConqueredTax > 0)
			conqueredTaxCollected += localConqueredTax;

		// Check if the town was newly bankrupted and punish them for it.
		if (!townWasBankrupt) {
			town.setOpen(false);
			town.save();
			return "delinquent";
		}
		return "";
	}

	private void payNationTaxToTown(Nation nation, Town town, double tax) {
		if (!nation.getAccount().canPayFromHoldings(tax))
			return;
		nation.getAccount().payTo(tax, town, "Nation Tax Payment To Town");
		taxCollected += tax;
	}

	/**
	 * Collect taxes for all towns due from their residents.
	 */
	public void collectTownTaxes() {
		List<Town> towns = new ArrayList<>(universe.getTowns());
		ListIterator<Town> townItr = towns.listIterator();
		Town town;

		while (townItr.hasNext()) {
			taxCollected = 0.0;
			town = townItr.next();
			/*
			 * Only collect resident tax for this town if it really still
			 * exists.
			 * We are running in an Async thread so MUST verify all objects.
			 */
			if (!town.exists())
				continue;

			if (town.isRuined())
				continue;

			collectTownTaxes(town);
		}

		if (!residentPlotTaxMap.isEmpty())
			messageResidentsAboutPlotTaxesPaid();
	}

	/**
	 * Collect taxes due to the town from it's residents.
	 * 
	 * @param town - Town to collect taxes from
	 */
	protected void collectTownTaxes(Town town) {
		/*
		 * Taxes paid by or paid to the Residents of the town.
		 */
		collectTownResidentTax(town);
		if (taxCollected != 0.0) {
			String msgSlug = taxCollected > 0.0 ? "msg_tax_collected_from_residents" : "msg_tax_paid_to_residents";
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of(msgSlug, prettyMoney(taxCollected)));
			taxCollected = 0.0;
		}

		/*
		 * Taxes paid by or paid to the Residents that own Town land personally.
		 */
		collecTownPlotTax(town);
		if (taxCollected != 0.0) {
			String msgSlug = taxCollected > 0.0 ? "msg_tax_collected_from_plots" : "msg_tax_paid_to_residents_for_plots";
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of(msgSlug, prettyMoney(taxCollected)));
			taxCollected = 0.0;
		}
	}

	private void collectTownResidentTax(Town town) {
		double tax = town.getTaxes();
		if (tax == 0)
			return;

		// Don't allow negative taxes if the town uses a tax percentage or negative town tax is explicitly disallowed.
		if (tax < 0 && (!TownySettings.isNegativeTownTaxAllowed() || town.isTaxPercentage()))
			return;

		// Tax is over 0.
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
			if (!resident.exists())
				continue;

			if (TownyPerms.getResidentPerms(resident).get("towny.tax_exempt") == Boolean.TRUE || resident.isNPC() || (!TownySettings.doMayorsPayTownTax() && resident.isMayor())) {
				TownyMessaging.sendMsg(resident, Translatable.of("msg_tax_exempt"));
				continue;
			}

			// Remove the resident for non-payment unless they're a mayor (who might be made to pay tax.)
			if (!collectTownTaxFromResident(tax, resident, town) && !resident.isMayor())
				removedResidents.add(resident.getName());
		}

		if (removedResidents != null && !removedResidents.isEmpty()) {
			if (removedResidents.size() == 1) 
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_tax", removedResidents.get(0), Translatable.of("town_sing")));
			else
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_town_tax_multiple").append(StringMgmt.join(removedResidents, ", ")));
		}
	}

	private boolean collectTownTaxFromResident(double tax, Resident resident, Town town) {
		if (tax < 0) {
			payTownTaxToResidents(town, resident, tax);
			return true;
		}

		if (town.isTaxPercentage()) {
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
				return true;
			
			resident.getAccount().payTo(tax, town, String.format("Town Tax (Percentage) paid by %s.", resident.getName()));
			if (resident.isOnline())
				TownyMessaging.sendMsg(resident, Translatable.of("msg_you_paid_town_tax", prettyMoney(tax)));
			taxCollected += tax;
			return true;
		}

		// Check if the bank could take the money, reduce it to 0 if required so that 
		// players do not get kicked in a situation they could be paying but cannot because
		// of the bank cap.
		if (town.getBankCap() != 0 && tax + town.getAccount().getHoldingBalance() > town.getBankCap())
			tax = town.getBankCap() - town.getAccount().getHoldingBalance();

		if (tax == 0)
			return true;

		if (resident.getAccount().canPayFromHoldings(tax)) {
			resident.getAccount().payTo(tax, town, String.format("Town tax (FlatRate) paid by %s.", resident.getName()));
			if (resident.isOnline())
				TownyMessaging.sendMsg(resident, Translatable.of("msg_you_paid_town_tax", prettyMoney(tax)));
			taxCollected += tax;
			return true;
		}

		// Mayors can still be made to pay the tax, but are exempt of any real punishments.
		if (!resident.isMayor()) {
			TownyMessaging.sendMsg(resident, Translatable.of("msg_you_couldnt_pay_town_tax", prettyMoney(tax), town.getFormattedName()));
			// remove this resident from the town, they cannot pay the town tax.
			resident.removeTown();
		}

		return false;
	}

	private void payTownTaxToResidents(Town town, Resident resident, double tax) {
		if (!town.getAccount().canPayFromHoldings(tax))
			return;
		town.getAccount().payTo(tax, resident, "Town Tax Payment To Resident");
		taxCollected += tax;
	}

	private void collecTownPlotTax(Town town) {

		List<TownBlock> townBlocks = new ArrayList<>(town.getTownBlocks());
		List<String> lostPlots = new ArrayList<>();
		ListIterator<TownBlock> townBlockItr = townBlocks.listIterator();
		TownBlock townBlock;

		while (townBlockItr.hasNext()) {
			townBlock = townBlockItr.next();
			double tax = townBlock.getType().getTax(town);

			if (!townBlock.isTaxed() || !townBlock.hasResident() || tax == 0 ||
				!TownySettings.isNegativePlotTaxAllowed() && tax < 1)
				continue;

			Resident resident = townBlock.getResidentOrNull();

			/*
			 * Only collect plot tax from this resident if it really still exist and are not
			 * an NPC. We are running in an Async thread so MUST verify all objects.
			 */
			if (resident == null || !resident.exists() || resident.isNPC())
				continue;

			// Prevents Mayors/Assistants/VIPs paying taxes in their own town.
			if (town.hasResident(resident) && TownyPerms.getResidentPerms(resident).get("towny.tax_exempt") == Boolean.TRUE)
				continue;

			// The PlotTax might be negative, in order to pay the resident for owning a special plot type.
			if (tax < 0) {
				payPlotTaxToResidents(Math.abs(tax), resident, town, townBlock.getTypeName());
				continue;
			}

			// If the tax would put the town over the bank cap we reduce what will be
			// paid by the plot owner to what will be allowed.
			if (town.getBankCap() != 0 && tax + town.getAccount().getHoldingBalance() > town.getBankCap())
				tax = town.getBankCap() - town.getAccount().getHoldingBalance();

			if (tax == 0)
				continue;

			if (!collectPlotTaxFromResident(tax, resident, town, townBlock) && !lostPlots.contains(resident.getName()))
				lostPlots.add(resident.getName());

		}

		if (lostPlots != null && !lostPlots.isEmpty()) {
			if (lostPlots.size() == 1) 
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_plot_taxes", lostPlots.get(0)));
			else
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_couldnt_pay_plot_taxes_multiple").append(StringMgmt.join(lostPlots, ", ")));
		}
	}

	private boolean collectPlotTaxFromResident(double tax, Resident resident, Town town, TownBlock townBlock) {
		if (resident.getAccount().canPayFromHoldings(tax)) {
			resident.getAccount().payTo(tax, town, String.format("Plot Tax (%s) paid by %s", townBlock.getTypeName(), resident.getName()));
			
			if (resident.isOnline())
				addPaymentToPlotTaxMap(resident, town, tax);

			taxCollected += tax;
			return true;
		}

		TownyMessaging.sendMsg(resident, Translatable.of("msg_you_couldnt_pay_plot_tax", prettyMoney(tax), townBlock.toString()));
		// Could not pay the plot tax, remove the resident from the plot.
		townBlock.removeResident();

		// Set the plot price.
		if (TownySettings.doesPlotTaxNonPaymentSetPlotForSale())
			townBlock.setPlotPrice(town.getPlotTypePrice(townBlock.getType()));
		else
			townBlock.setPlotPrice(-1);

		// Set the plot permissions to mirror the towns.
		townBlock.setType(townBlock.getType());

		townBlock.save();
		return false;
	}

	private void addPaymentToPlotTaxMap(Resident resident, Town town, double tax) {
		if (!residentPlotTaxMap.containsKey(resident)) {
			Map<Town, Double> townMap = new ConcurrentHashMap<>();
			townMap.put(town, tax);
			residentPlotTaxMap.put(resident, townMap);
			return;
		}

		Map<Town, Double> townMap = residentPlotTaxMap.get(resident);
		double amount = townMap.containsKey(town) ? townMap.get(town) + tax : tax;
		townMap.put(town, amount);
		residentPlotTaxMap.put(resident, townMap);
	}

	private void messageResidentsAboutPlotTaxesPaid() {
		for (Resident resident : residentPlotTaxMap.keySet()) {
			if (!resident.isOnline())
				continue;
			Map<Town, Double> townMap = residentPlotTaxMap.get(resident);
			if (townMap.isEmpty())
				continue;
			for (Town townKey : townMap.keySet()) {
				double tax = townMap.get(townKey);
				TownyMessaging.sendMsg(resident, Translatable.of("msg_you_paid_plottax_to_town", prettyMoney(tax), townKey));
			}
		}
	}

	private void payPlotTaxToResidents(double tax, Resident resident, Town town, String typeName) {
		if (!town.getAccount().canPayFromHoldings(tax))
			return;
		town.getAccount().payTo(tax, resident, String.format("Plot Tax Payment To Resident (%s)", typeName));
		taxCollected += tax;
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
			if (!town.exists() || !town.hasUpkeep() || town.isRuined())
				continue;

			processTownUpkeep(town);
			processTownNeutralCosts(town);
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

	private void processTownUpkeep(Town town) {
		double upkeep = TownySettings.getTownUpkeepCost(town);
		double upkeepPenalty = TownySettings.getTownPenaltyUpkeepCost(town);
		if (upkeepPenalty > 0)
			upkeep = upkeep + upkeepPenalty;
	
		if (upkeep > 0) {
			chargeTownUpkeep(town, upkeep);
		} else if (upkeep < 0) {
			payTownNegativeUpkeep(upkeep, town);
		}
	}

	private void processTownNeutralCosts(Town town) {
		if (!town.isNeutral())
			return;

		double neutralityCost = TownySettings.getTownNeutralityCost(town);
		if (neutralityCost <= 0)
			return;

		// Charge towns for keeping a peaceful status.
		if ((town.isBankrupt() && !TownySettings.canBankruptTownsPayForNeutrality())
		|| !town.getAccount().withdraw(neutralityCost, "Town Peace Upkeep")) {
			town.setNeutral(false);
			town.save();
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_not_peaceful"));
		} else {
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_town_paid_for_neutral_status", prettyMoney(neutralityCost)));
		}
	}


	private void chargeTownUpkeep(Town town, double upkeep) {
		if (town.getAccount().canPayFromHoldings(upkeep)) {
			// Town is able to pay the upkeep.
			town.getAccount().withdraw(upkeep, "Town Upkeep");
			totalTownUpkeep = totalTownUpkeep + upkeep;
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_payed_upkeep", prettyMoney(upkeep)));
			return;
		}

		// Town is unable to pay the upkeep.
		if (!TownySettings.isTownBankruptcyEnabled()) {
			// Bankruptcy is disabled, remove the town for not paying upkeep.
			if (universe.getDataSource().removeTown(town, DeleteTownEvent.Cause.UPKEEP)) {
				TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_upkeep", prettyMoney(upkeep)));
				removedTowns.add(town.getName());
			}
			return;
		}

		// Bankruptcy is enabled.
		boolean townWasBankrupt = town.isBankrupt();
		town.getAccount().setDebtCap(MoneyUtil.getTownDebtCap(town, upkeep));

		if (town.getAccount().getHoldingBalance() - upkeep < town.getAccount().getDebtCap() * -1) {
			// The town will exceed their debt cap to pay the upkeep.
			// Eventually when the cap is reached they will pay 0 upkeep.

			if (TownySettings.isUpkeepDeletingTownsThatReachDebtCap()) {
				// Alternatively, if configured, towns will not be allowed to exceed
				// their debt and be deleted from the server for non-payment finally.
				if (universe.getDataSource().removeTown(town, DeleteTownEvent.Cause.BANKRUPTCY)) {
					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_couldnt_pay_upkeep", prettyMoney(upkeep)));
					removedTowns.add(town.getName());
					return;
				}
			}
			upkeep = town.getAccount().getDebtCap() - Math.abs(town.getAccount().getHoldingBalance());
		}

		// Finally pay the upkeep or the modified upkeep up to the debtcap. 
		town.getAccount().withdraw(upkeep, "Town Upkeep");
		totalTownUpkeep = totalTownUpkeep + upkeep;
		TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_your_town_payed_upkeep_with_debt", prettyMoney(upkeep)));

		// Check if the town was newly bankrupted and punish them for it.
		if(!townWasBankrupt) {
			town.setOpen(false);
			town.save();
			bankruptedTowns.add(town.getName());
		}
	}


	private void payTownNegativeUpkeep(double upkeep, Town town) {
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
			if (!nation.exists() || !nation.getCapital().hasUpkeep())
				continue;

			processNationUpkeep(nation);
			processNationNeutralCosts(nation);
		}

		if (removedNations != null && !removedNations.isEmpty()) {
			if (removedNations.size() == 1)
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_bankrupt_nation2", removedNations.get(0)));
			else
				TownyMessaging.sendGlobalMessage(Translatable.of("msg_bankrupt_nation_multiple").append(StringMgmt.join(removedNations, ", ")));
		}
	}

	private void processNationUpkeep(Nation nation) {
		double upkeep = TownySettings.getNationUpkeepCost(nation);

		if (upkeep > 0) {
			// Nation is paying upkeep
			if (nation.getAccount().canPayFromHoldings(upkeep)) {
				nation.getAccount().withdraw(upkeep, "Nation Upkeep");
				totalNationUpkeep = totalNationUpkeep + upkeep;
				TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_your_nation_payed_upkeep", prettyMoney(upkeep)));
			} else {
				List<Player> onlinePlayers = TownyAPI.getInstance().getOnlinePlayersInNation(nation); 
				if (universe.getDataSource().removeNation(nation, DeleteNationEvent.Cause.UPKEEP)) {
					String formattedUpkeep = prettyMoney(upkeep);
					onlinePlayers.forEach(p -> TownyMessaging.sendMsg(p, Translatable.of("msg_your_nation_couldnt_pay_upkeep", formattedUpkeep)));
					removedNations.add(nation.getName());
					return;
				}
			}
		} else if (upkeep < 0) {
			nation.getAccount().withdraw(upkeep, "Negative Nation Upkeep");
		}
	}

	private void processNationNeutralCosts(Nation nation) {
		if (!nation.isNeutral())
			return;

		double neutralityCost = TownySettings.getNationNeutralityCost(nation);
		if (neutralityCost <= 0)
			return;

		// Charge nations for keeping a peaceful status.
		if (!nation.getAccount().withdraw(neutralityCost, "Nation Peace Upkeep")) {
			nation.setNeutral(false);
			nation.save();
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_not_peaceful"));
		} else {
			TownyMessaging.sendPrefixedNationMessage(nation, Translatable.of("msg_nation_paid_for_neutral_status", prettyMoney(neutralityCost)));
		}
	}

	private String prettyMoney(double money) {
		return TownyEconomyHandler.getFormattedBalance(money);
	}
}
