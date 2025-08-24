package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author ElgarL
 * 
 */
public class PlotClaim implements Runnable {

	Towny plugin;
	private final Player player;
	private final Resident resident;
	private final List<WorldCoord> selection;
	private final boolean claim;
	private final boolean admin;
	private final boolean groupClaim;

	/**
	 * @param plugin reference to towny
	 * @param player Doing the claiming, or null
	 * @param selection List of WoorldCoords to claim/unclaim
	 * @param claim or unclaim
	 * @param admin - is this admin overrided.
	 * @param resident - see player parameter
	 * @param groupClaim Indicates whether the claim is part of a plot group claim.
	 */
	public PlotClaim(Towny plugin, Player player, Resident resident, List<WorldCoord> selection, boolean claim, boolean admin, boolean groupClaim) {

		super();
		this.plugin = plugin;
		this.player = player;
		this.resident = resident;
		this.selection = selection;
		this.claim = claim;
		this.admin = admin;
		this.groupClaim = groupClaim;
	}

	@Override
	public void run() {
		if (player != null){
			if (claim)
				TownyMessaging.sendMsg(player, Translatable.of("msg_process_claim"));
			else
				TownyMessaging.sendMsg(player, Translatable.of("msg_process_unclaim"));
		}

		if (selection == null && !claim) {
			residentUnclaimAll();
			return;
		}

		if (groupClaim) {
			handleGroupClaim(selection.get(0));
			return;
		}

		if (admin) {
			adminClaim(selection);
			return;
		}

		if (!claim) {
			residentUnclaim(selection);
			return;
		}

		if (claim) {
			residentClaim(selection);
			return;
		}
	}

	private void handleGroupClaim(WorldCoord worldCoord) {

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		if (townBlock == null || !townBlock.hasPlotObjectGroup())
			return;

		final PlotGroup group = townBlock.getPlotObjectGroup();
		try {

			/*
			 * Test that they can actually own that many townblocks.
			 */
			testMaxPlotsOrThrow(group.getTownBlocks().size());

			/*
			 * Handle paying for the plot here so it is not paid for per-townblock in the group.
			 */
			double groupPrice = group.getPrice();
			if (TownyEconomyHandler.isActive() && groupPrice > 0) {
				EconomyHandler seller = group.hasResident() ? group.getResident() : group.getTown();
				String message = String.format("Plot Group - Buy From %s: %s", (group.hasResident() ? "Seller" : "Town"), seller.getName());

				if (seller instanceof Town town) { // Test that the town wouldn't go over their bank cap.
					double bankcap = town.getBankCap();
					if (bankcap > 0 && groupPrice + town.getAccount().getHoldingBalance() > bankcap)
						throw new TownyException(Translatable.of("msg_err_deposit_capped", bankcap));
				}

				// Make them pay, they have already been tested to see if they can afford it in the PlotCommand class.
				if (!resident.getAccount().payTo(groupPrice, seller, message))
					throw new TownyException(Translatable.of("msg_no_money_purchase_plot"));
			}
		} catch (TownyException e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			return;
		} catch (Exception e) {
			TownyMessaging.sendErrorMsg(player, e.getMessage());
			return;
		}

		/*
		 * Cause the actual claiming here.
		 */
		residentGroupClaim(group);
		group.setResident(resident);
		group.setPrice(-1);
		TownyMessaging.sendPrefixedTownMessage(townBlock.getTownOrNull(), Translatable.of("msg_player_successfully_bought_group_x", player.getName(), group.getName()));
		group.save();
		finishWithMessage();
	}

	private void residentGroupClaim(PlotGroup group) {
		Town town = group.getTown();
		@Nullable Resident owner = group.getResident();
		if (owner != null && group.getPrice() > 0)
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_buy_resident_plot_group", resident.getName(), owner.getName(), group.getPrice()));

		for (TownBlock townBlock : group.getTownBlocks())
			claimTownBlockForResident(townBlock);
	}

	private void residentClaim(List<WorldCoord> selection) {
		Iterator<WorldCoord> it = selection.iterator();

		while (it.hasNext()) {
			WorldCoord worldCoord = it.next();
			try {
				if (!residentClaim(worldCoord))
					it.remove(); // безопасное удаление
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			}
		}

		finishWithMessage();
	}

	private boolean residentClaim(WorldCoord worldCoord) throws TownyException {

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		if (townBlock == null)
			throw new TownyException(Translatable.of("msg_err_not_part_town"));

		if (townBlock.getPlotPrice() == -1)
			throw new TownyException(Translatable.of("msg_err_plot_nfs"));

		Town town = townBlock.getTownOrNull();
		if (!townBlock.getType().equals(TownBlockType.EMBASSY) && !town.hasResident(resident))
			throw new TownyException(Translatable.of("msg_err_not_part_town"));

		testMaxPlotsOrThrow(1);

		/*
		 * Handle paying for the plot here.
		 */
		double price = townBlock.getPlotPrice();
		if (TownyEconomyHandler.isActive() && price > 0) {
			EconomyHandler seller = townBlock.hasResident() ? townBlock.getResidentOrNull() : townBlock.getTownOrNull();
			String message = String.format("Plot - Buy From %s: %s", (townBlock.hasResident() ? "Seller" : "Town"), seller.getName());

			if (seller instanceof Town) { // Test that the town wouldn't go over their bank cap.
				double bankcap = town.getBankCap();
				if (bankcap > 0 && price + town.getAccount().getHoldingBalance() > bankcap)
					throw new TownyException(Translatable.of("msg_err_deposit_capped", bankcap));
			}

			// Make them pay, they have already been tested to see if they can afford it in the PlotCommand class.
			if (!resident.getAccount().payTo(price, seller, message))
				throw new TownyException(Translatable.of("msg_no_money_purchase_plot"));
		}

		/*
		 * Handle actual claiming here.
		 */
		final Resident owner = townBlock.getResidentOrNull();
		if (owner != null && price > 0)
			TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_buy_resident_plot", resident.getName(), owner.getName(), townBlock.getPlotPrice()));

		return claimTownBlockForResident(townBlock);
	}

	private boolean claimTownBlockForResident(TownBlock townBlock) {
		if(!townBlock.setResident(resident)) return false;
		townBlock.setPlotPrice(-1);
		townBlock.setType(townBlock.getType()); // Causes the plot perms to mirror the new owner's.
		townBlock.save();
		plugin.updateCache(townBlock.getWorldCoord());
		return true;
	}

	private void residentUnclaimAll() {
		residentUnclaim(new ArrayList<>(resident.getTownBlocks()).stream().map(TownBlock::getWorldCoord).collect(Collectors.toList()));
	}

	private void residentUnclaim(List<WorldCoord> selection) {
		Iterator<WorldCoord> it = selection.iterator();
		while (it.hasNext()) {
			WorldCoord coord = it.next();
			if (!TownyAPI.getInstance().isTownyWorld(coord.getBukkitWorld()))
				continue;

			if(!residentUnclaim(coord)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_own_place"));
				it.remove();
			}
		}

		finishWithMessage();
 	}

 	private boolean residentUnclaim(WorldCoord worldCoord) {
		if (worldCoord.isWilderness())
			return false;

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		if(!townBlock.removeResident()) return false;
		townBlock.setPlotPrice(townBlock.getTownOrNull().getPlotTypePrice(townBlock.getType()));
		// Set the plot permissions to mirror the towns.
		townBlock.setType(townBlock.getType());
		townBlock.save();
		plugin.updateCache(worldCoord);
		return true;
	}

	/*
	 * Used via /ta plot claim {name}
	 */
	private void adminClaim(List<WorldCoord> selection) {
		for (WorldCoord wc : new ArrayList<>(selection)) {
			if (!adminClaim(wc)) {
				TownyMessaging.sendErrorMsg(player, Translatable.of("msg_not_claimed", wc.toString()));
				this.selection.remove(wc);
			}
		}
		finishWithMessage();
	}

	private boolean adminClaim(WorldCoord worldCoord) {
		if (worldCoord.isWilderness())
			return false;

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		townBlock.setPlotPrice(-1);
		if(!townBlock.setResident(resident)) return false;
		townBlock.setType(townBlock.getType());
		townBlock.save();
		plugin.updateCache(worldCoord);
		TownyMessaging.sendMsg(resident, Translatable.of("msg_admin_has_given_you_a_plot", worldCoord.toString()));
		return true;
	}

	private void finishWithMessage() {
		if (player == null) 
			return;
		if (claim) {
			if ((selection != null) && (!selection.isEmpty())) {
				TownyMessaging.sendMsg(player, Translatable.of("msg_claimed").append(" ")
						.append(selection.size() > 5 ? Translatable.of("msg_total_townblocks").forLocale(player) + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
			} else {
				TownyMessaging.sendMsg(player, Translatable.of("msg_not_claimed_1"));
			}
		} else if (selection != null) {
			TownyMessaging.sendMsg(player, Translatable.of("msg_unclaimed").append(" ")
					.append(selection.size() > 5 ? Translatable.of("msg_total_townblocks").forLocale(player) + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
		} else {
			TownyMessaging.sendMsg(player, Translatable.of("msg_unclaimed"));
		}
	}

	private void testMaxPlotsOrThrow(int plotsToBuy) throws TownyException {
		int maxPlots = TownySettings.getMaxResidentPlots(resident);
		int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);

		//Infinite plots
		if (maxPlots != -1)
			maxPlots = maxPlots + extraPlots;

		if (maxPlots >= 0 && resident.getTownBlocks().size() + plotsToBuy > maxPlots)
			throw new TownyException(Translatable.of("msg_max_plot_own", maxPlots));
	}

}
