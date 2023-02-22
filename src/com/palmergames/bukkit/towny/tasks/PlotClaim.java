package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.object.WorldCoord;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author ElgarL
 * 
 */
public class PlotClaim extends Thread {

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
		this.setPriority(MIN_PRIORITY);
	}

	@Override
	public void run() {
		
		int claimed = 0;

		if (player != null){
			if (claim)
				TownyMessaging.sendMsg(player, Translatable.of("msg_process_claim"));
			else
				TownyMessaging.sendMsg(player, Translatable.of("msg_process_unclaim"));
		}

		if (selection != null) {
			
			for (WorldCoord worldCoord : selection) {
				
				try {
					if (worldCoord.getTownBlock().hasPlotObjectGroup()) {
						final PlotGroup group = worldCoord.getTownBlock().getPlotObjectGroup();
						
						/*
						 *  Handle paying for the plot here so it is not paid for per-townblock in the group.
						 */
						if (TownyEconomyHandler.isActive() && group.getPrice() > 0) {

							if (group.getResident() != null) {
								/*
								 * The plots are resident owned.
								 */
								if (!resident.getAccount().payTo(group.getPrice(), group.getResident(), "Plot Group - Buy From Seller")) {
									/*
									 * Should not be possible, as the resident has already been tested to see if they have enough to pay.
									 */
									TownyMessaging.sendErrorMsg(player, Translatable.of("msg_no_money_purchase_plot"));
									break;
								}
							} else {
								/*
								 *  The plots are town-owned.								
								 */
								double bankcap = worldCoord.getTownBlock().getPlotObjectGroup().getTown().getBankCap();
								if (bankcap > 0) {
									if (worldCoord.getTownBlock().getPlotObjectGroup().getPrice() + worldCoord.getTownBlock().getPlotObjectGroup().getTown().getAccount().getHoldingBalance() > bankcap)
										throw new TownyException(Translatable.of("msg_err_deposit_capped", bankcap));
								}
								
								if (!resident.getAccount().payTo(group.getPrice(), group.getTown(), "Plot Group - Buy From Town")) {
									/*
									 * Should not be possible, as the resident has already been tested to see if they have enough to pay.
									 */
									TownyMessaging.sendErrorMsg(player, Translatable.of("msg_no_money_purchase_plot"));
									break;
								}
							}
						}
						
						/*
						 *  Cause the actual claiming here.
						 */
						if (residentGroupClaim(selection)) {
							claimed++;
						
						
							group.setResident(resident);
							group.setPrice(-1);
							TownyMessaging.sendPrefixedTownMessage(worldCoord.getTownBlock().getTown(), Translatable.of("msg_player_successfully_bought_group_x", player.getName(), group.getName()));
							
							group.save();
							break;
						}
					}
				} catch (TownyException e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage(player));
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

				// Make sure this is a valid world (mainly when unclaiming).
				if (!TownyAPI.getInstance().isTownyWorld(worldCoord.getBukkitWorld()))
					continue;

				try {
					if (claim) {
						if (groupClaim) {
							
						} else if (!admin) {
							if (residentClaim(worldCoord))
								claimed++;
						} else {
							adminClaim(worldCoord);
							claimed++;							
						}
						
						
					} else {
						residentUnclaim(worldCoord);
					}
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(player, x.getMessage(player));
				}

			}
		} else if (!claim) {
			residentUnclaimAll();
		}

		if (player != null) {
			if (claim) {
				if ((selection != null) && (selection.size() > 0) && (claimed > 0)) {
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
		
		resident.save();
		plugin.resetCache();

	}

	/**
	 * A similar function to {@link #residentClaim(WorldCoord)}, that deals
	 * with group member, or more specifically updates plot group values, to
	 * show group ownership and membership.
	 * @param worldCoords The coordinates of the blocks to be claimed.
	 * @return A boolean indicating if the transaction was successful.
	 * @throws TownyException Whenever an object could not be retrieved.
	 * @author Suneet Tipirneni (Siris)
	 */
	private boolean residentGroupClaim(List<WorldCoord> worldCoords) throws TownyException {
		
		for (int i = 0; i < worldCoords.size(); ++i) {
			
			WorldCoord worldCoord = worldCoords.get(i);
			
			
			try {
				TownBlock townBlock = worldCoord.getTownBlock();
				Town town = townBlock.getTown();
				PlotGroup group = townBlock.getPlotObjectGroup();

				if ((resident.hasTown() && (resident.getTown() != town) && (!townBlock.getType().equals(TownBlockType.EMBASSY))) || ((!resident.hasTown()) && (!townBlock.getType().equals(TownBlockType.EMBASSY))))
					throw new TownyException(Translatable.of("msg_err_not_part_town"));

				if (townBlock.getPlotObjectGroup().hasResident()) {
					/*
					 * Plotgroup is resident-owned.
					 */
					Resident owner = townBlock.getPlotObjectGroup().getResident();

					if (group.getPrice() != -1) {
						// Plot is for sale

						int maxPlots = TownySettings.getMaxResidentPlots(resident);
						int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);

						//Infinite plots
						if (maxPlots != -1) {
							maxPlots = maxPlots + extraPlots;
						}

						if (maxPlots >= 0 && resident.getTownBlocks().size() + group.getTownBlocks().size() > maxPlots)
							throw new TownyException(Translatable.of("msg_max_plot_own", maxPlots));

						TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_buy_resident_plot", resident.getName(), owner.getName(), townBlock.getPlotObjectGroup().getPrice()));
						
						townBlock.setResident(resident);

						// Set the plot permissions to mirror the new owners.
						// TODO: Plot types for groups.
						//group.setType(townBlock.getType());

						owner.save();
						group.save();
						townBlock.save();

						if (i >= worldCoords.size() - 2) {
							TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_player_successfully_bought_group_x", resident.getName(), group.getName()));
						}

						// Update any caches for this WorldCoord
						plugin.updateCache(worldCoord);
					} else {
						//Should never reach here.
						throw new AlreadyRegisteredException(Translatable.of("msg_already_claimed", owner.getName()).forLocale(player));
					}

				} else {
					/*
					 * Plot group is town-owned.
					 */

					if (townBlock.getPlotObjectGroup().getPrice() == -1) {
						throw new TownyException(Translatable.of("msg_err_plot_nfs"));
					}

					townBlock.setResident(resident);

					// Set the plot permissions to mirror the new owners.
					townBlock.setType(townBlock.getType());
					townBlock.save();
					
				}
			} catch (NotRegisteredException e) {
				throw new TownyException(Translatable.of("msg_err_not_part_town"));
			}
			
		}
		
		return true;
	}

	private boolean residentClaim(WorldCoord worldCoord) throws TownyException {

		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			Town town = townBlock.getTown();
			if ((resident.hasTown() && (resident.getTown() != town) && (!townBlock.getType().equals(TownBlockType.EMBASSY))) || ((!resident.hasTown()) && (!townBlock.getType().equals(TownBlockType.EMBASSY))))
				throw new TownyException(Translatable.of("msg_err_not_part_town"));

			final Resident owner = townBlock.getResidentOrNull();
			if (owner != null) {

				if (townBlock.getPlotPrice() != -1) {
					// Plot is for sale

					if (TownyEconomyHandler.isActive() && townBlock.getPlotPrice() > 0 && !resident.getAccount().payTo(townBlock.getPlotPrice(), owner, "Plot - Buy From Seller"))
						throw new TownyException(Translatable.of("msg_no_money_purchase_plot"));

					int maxPlots = TownySettings.getMaxResidentPlots(resident);
					int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);
					
					//Infinite plots
					if (maxPlots != -1) {
						maxPlots = maxPlots + extraPlots;
					}
					
					if (maxPlots >= 0 && resident.getTownBlocks().size() + 1 > maxPlots)
						throw new TownyException(Translatable.of("msg_max_plot_own", maxPlots));

					TownyMessaging.sendPrefixedTownMessage(town, Translatable.of("msg_buy_resident_plot", resident.getName(), owner.getName(), townBlock.getPlotPrice()));
					townBlock.setPlotPrice(-1);
					townBlock.setResident(resident);

					// Set the plot permissions to mirror the new owners.
					townBlock.setType(townBlock.getType());
					
					owner.save();
					townBlock.save();

					// Update any caches for this WorldCoord
					plugin.updateCache(worldCoord);
					return true;
				} else {
					//Should never reach here.
					throw new AlreadyRegisteredException(Translatable.of("msg_already_claimed", owner.getName()).forLocale(player));
				}

			} else {
				//Plot has no owner so it's the town selling it

				if (townBlock.getPlotPrice() == -1)
					throw new TownyException(Translatable.of("msg_err_plot_nfs"));
				
				double bankcap = town.getBankCap();
				if (TownyEconomyHandler.isActive() && bankcap > 0) {
					if (townBlock.getPlotPrice() + town.getAccount().getHoldingBalance() > bankcap)
						throw new TownyException(Translatable.of("msg_err_deposit_capped", bankcap));
				}

				if (TownyEconomyHandler.isActive() && townBlock.getPlotPrice() > 0 && !resident.getAccount().payTo(townBlock.getPlotPrice(), town, "Plot - Buy From Town"))
					throw new TownyException(Translatable.of("msg_no_money_purchase_plot"));

				townBlock.setPlotPrice(-1);
				townBlock.setResident(resident);

				// Set the plot permissions to mirror the new owners.
				townBlock.setType(townBlock.getType());
				townBlock.save();

				return true;
			}
		} catch (NotRegisteredException e) {
			throw new TownyException(Translatable.of("msg_err_not_part_town"));
		}
	}

	private boolean residentUnclaim(WorldCoord worldCoord) throws TownyException {

		try {
			TownBlock townBlock = worldCoord.getTownBlock();

			townBlock.setResident(null);
			townBlock.setPlotPrice(townBlock.getTown().getPlotTypePrice(townBlock.getType()));

			// Set the plot permissions to mirror the towns.
			townBlock.setType(townBlock.getType());
			townBlock.save();

			plugin.updateCache(worldCoord);

		} catch (NotRegisteredException e) {
			throw new TownyException(Translatable.of("msg_not_own_place"));
		}

		return true;
	}

	private void residentUnclaimAll() {

		List<TownBlock> selection = new ArrayList<>(resident.getTownBlocks());

		for (TownBlock townBlock : selection) {
			try {
				residentUnclaim(townBlock.getWorldCoord());
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage(player));
			}

		}

	}

	/**
	 * Used via /ta plot claim {name}
	 * 
	 * @param worldCoord
	 * @throws TownyException 
	 */
	private void adminClaim(WorldCoord worldCoord) throws TownyException {

		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			@SuppressWarnings("unused") // Used to make sure a plot/town is here.
			Town town = townBlock.getTown();
			
			townBlock.setPlotPrice(-1);
			townBlock.setResident(resident);
			townBlock.setType(townBlock.getType());
			townBlock.save();
			
			TownyMessaging.sendMsg(resident, Translatable.of("msg_admin_has_given_you_a_plot", worldCoord.toString()));
		} catch (NotRegisteredException e) {
			//Probably not owned by a town.
			throw new TownyException(Translatable.of("msg_not_claimed_1"));			

		}
	}

}
