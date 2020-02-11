package com.palmergames.bukkit.towny.tasks;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.exceptions.AlreadyRegisteredException;
import com.palmergames.bukkit.towny.exceptions.EconomyException;
import com.palmergames.bukkit.towny.exceptions.NotRegisteredException;
import com.palmergames.bukkit.towny.exceptions.TownyException;
import com.palmergames.bukkit.towny.object.PlotGroup;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.bukkit.util.BukkitTools;

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
	private volatile Player player;
	private volatile Resident resident;
	@SuppressWarnings("unused")
	private volatile TownyWorld world;
	private List<WorldCoord> selection;
	private boolean claim, admin, groupClaim;

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
				TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_process_claim"));
			else
				TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_process_unclaim"));
		}

		if (selection != null) {
			
			for (WorldCoord worldCoord : selection) {
				
				try {
					if (worldCoord.getTownBlock().hasPlotObjectGroup() && residentGroupClaim(selection)) {
						claimed++;
					
					
						worldCoord.getTownBlock().getPlotObjectGroup().setResident(resident);
						worldCoord.getTownBlock().getPlotObjectGroup().setPrice(-1);
						TownyMessaging.sendPrefixedTownMessage(worldCoord.getTownBlock().getTown(), String.format(TownySettings.getLangString("msg_player_successfully_bought_group_x"), player.getName(), worldCoord.getTownBlock().getPlotObjectGroup().getName()));
						
						TownyUniverse.getInstance().getDataSource().savePlotGroup(worldCoord.getTownBlock().getPlotObjectGroup());
						
						break;
					}
				} catch (Exception e) {
					TownyMessaging.sendErrorMsg(player, e.getMessage());
				}

				// Make sure this is a valid world (mainly when unclaiming).
				try {
					this.world = worldCoord.getTownyWorld();
				} catch (NotRegisteredException e) {
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_err_not_configured"));
					continue;
				}
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
				} catch (EconomyException e) {
					/*
					 * Can't pay, but try the rest as we may be
					 * re-possessing and claiming for personal plots.
					 */
					TownyMessaging.sendErrorMsg(player, e.getError());
				} catch (TownyException x) {
					TownyMessaging.sendErrorMsg(player, x.getMessage());
				}

			}
		} else if (!claim) {
			residentUnclaimAll();
		}

		if (player != null) {
			if (claim) {
				if ((selection != null) && (selection.size() > 0) && (claimed > 0)) {
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_claimed") + ((selection.size() > 5) ? TownySettings.getLangString("msg_total_townblocks") + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
				} else {
					TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_not_claimed_1"));
				}
			} else if (selection != null) {
				TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_unclaimed") + ((selection.size() > 5) ? TownySettings.getLangString("msg_total_townblocks") + selection.size() : Arrays.toString(selection.toArray(new WorldCoord[0]))));
			} else {
				TownyMessaging.sendMsg(player, TownySettings.getLangString("msg_unclaimed"));
			}
		}
		
		TownyUniverse.getInstance().getDataSource().saveResident(resident);
		plugin.resetCache();

	}

	/**
	 * A similar function to {@link #residentClaim(WorldCoord)}, that deals
	 * with group member, or more specifically updates plot group values, to
	 * show group ownership and membership.
	 * @param worldCoords The coordinates of the blocks to be claimed.
	 * @return A boolean indicating if the transaction was successful.
	 * @throws TownyException Whenever an object could not be retrieved.
	 * @throws EconomyException Whenever a sender cannot pay for transaction.
	 * @author Suneet Tipirneni (Siris)
	 */
	private boolean residentGroupClaim(List<WorldCoord> worldCoords) throws TownyException, EconomyException {
		
		for (int i = 0; i < worldCoords.size(); ++i) {
			
			WorldCoord worldCoord = worldCoords.get(i);
			
			try {
				TownBlock townBlock = worldCoord.getTownBlock();
				Town town = townBlock.getTown();
				PlotGroup group = townBlock.getPlotObjectGroup();

				if ((resident.hasTown() && (resident.getTown() != town) && (!townBlock.getType().equals(TownBlockType.EMBASSY))) || ((!resident.hasTown()) && (!townBlock.getType().equals(TownBlockType.EMBASSY))))
					throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
				TownyUniverse townyUniverse = TownyUniverse.getInstance();
				try {
					Resident owner = townBlock.getPlotObjectGroup().getResident();

					if (group.getPrice() != -1) {
						// Plot is for sale

						if (TownySettings.isUsingEconomy() && !resident.getAccount().payTo(group.getPrice(), owner, "Plot Group - Buy From Seller"))
							throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));

						int maxPlots = TownySettings.getMaxResidentPlots(resident);
						int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);

						//Infinite plots
						if (maxPlots != -1) {
							maxPlots = maxPlots + extraPlots;
						}

						if (maxPlots >= 0 && resident.getTownBlocks().size() + group.getTownBlocks().size() > maxPlots)
							throw new TownyException(String.format(TownySettings.getLangString("msg_max_plot_own"), maxPlots));

						TownyMessaging.sendPrefixedTownMessage(town, TownySettings.getBuyResidentPlotMsg(resident.getName(), owner.getName(), townBlock.getPlotObjectGroup().getPrice()));
						
						townBlock.setResident(resident);

						// Set the plot permissions to mirror the new owners.
						// TODO: Plot types for groups.
						//group.setType(townBlock.getType());

						townyUniverse.getDataSource().saveResident(owner);
						townyUniverse.getDataSource().savePlotGroup(group);
						townyUniverse.getDataSource().saveTownBlock(townBlock);

						if (i >= worldCoords.size() - 2) {
							TownyMessaging.sendPrefixedTownMessage(town, String.format(TownySettings.getLangString("msg_player_successfully_bought_group_x"),resident.getName(), group.getName()));
						}

						// Update any caches for this WorldCoord
						plugin.updateCache(worldCoord);
					} else if (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())) {
						//Plot isn't for sale but re-possessing for town.

						if (TownySettings.isUsingEconomy() && !town.getAccount().payTo(0.0, owner, "Plot - Buy Back"))
							throw new TownyException(TownySettings.getLangString("msg_town_no_money_purchase_plot"));

						TownyMessaging.sendPrefixedTownMessage(town, TownySettings.getBuyResidentPlotMsg(town.getName(), owner.getName(), 0.0));
						townBlock.setResident(resident);

						// Set the plot permissions to mirror the towns.
						//townBlock.setType(townBlock.getType());

						townyUniverse.getDataSource().saveResident(owner);
						townyUniverse.getDataSource().savePlotGroup(group);
						// Update the townBlock data file so it's no longer using custom settings.
						townyUniverse.getDataSource().saveTownBlock(townBlock);
						
					} else {
						//Should never reach here.
						throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_already_claimed"), owner.getName()));
					}

				} catch (NotRegisteredException e) {
					//Plot has no owner so it's the town selling it

					if (townBlock.getPlotObjectGroup().getPrice() == -1) {
						throw new TownyException(TownySettings.getLangString("msg_err_plot_nfs"));
					}


					double bankcap = TownySettings.getTownBankCap();
					if (bankcap > 0) {
						if (townBlock.getPlotPrice() + town.getAccount().getHoldingBalance() > bankcap)
							throw new TownyException(String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
					}

					if (TownySettings.isUsingEconomy() && !resident.getAccount().payTo(townBlock.getPlotObjectGroup().getPrice(), town, "Plot - Buy From Town"))
						throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));

					townBlock.setResident(resident);

					// Set the plot permissions to mirror the new owners.
					townBlock.setType(townBlock.getType());
					townyUniverse.getDataSource().saveTownBlock(townBlock);
					
				}
			} catch (NotRegisteredException e) {
				throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
			}
			
		}
		
		return true;
	}

	private boolean residentClaim(WorldCoord worldCoord) throws TownyException, EconomyException {

		try {
			TownBlock townBlock = worldCoord.getTownBlock();
			Town town = townBlock.getTown();
			if ((resident.hasTown() && (resident.getTown() != town) && (!townBlock.getType().equals(TownBlockType.EMBASSY))) || ((!resident.hasTown()) && (!townBlock.getType().equals(TownBlockType.EMBASSY))))
				throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
			TownyUniverse townyUniverse = TownyUniverse.getInstance();

			try {
				Resident owner = townBlock.getResident();

				if (townBlock.getPlotPrice() != -1) {
					// Plot is for sale

					if (TownySettings.isUsingEconomy() && !resident.getAccount().payTo(townBlock.getPlotPrice(), owner, "Plot - Buy From Seller"))
						throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));

					int maxPlots = TownySettings.getMaxResidentPlots(resident);
					int extraPlots = TownySettings.getMaxResidentExtraPlots(resident);
					
					//Infinite plots
					if (maxPlots != -1) {
						maxPlots = maxPlots + extraPlots;
					}
					
					if (maxPlots >= 0 && resident.getTownBlocks().size() + 1 > maxPlots)
						throw new TownyException(String.format(TownySettings.getLangString("msg_max_plot_own"), maxPlots));

					TownyMessaging.sendPrefixedTownMessage(town, TownySettings.getBuyResidentPlotMsg(resident.getName(), owner.getName(), townBlock.getPlotPrice()));
					townBlock.setPlotPrice(-1);
					townBlock.setResident(resident);

					// Set the plot permissions to mirror the new owners.
					townBlock.setType(townBlock.getType());
					
					townyUniverse.getDataSource().saveResident(owner);
					townyUniverse.getDataSource().saveTownBlock(townBlock);

					// Update any caches for this WorldCoord
					plugin.updateCache(worldCoord);
					return true;
				} else if (player.hasPermission(PermissionNodes.TOWNY_COMMAND_PLOT_ASMAYOR.getNode())) {
					//Plot isn't for sale but re-possessing for town.

					if (TownySettings.isUsingEconomy() && !town.getAccount().payTo(0.0, owner, "Plot - Buy Back"))
						throw new TownyException(TownySettings.getLangString("msg_town_no_money_purchase_plot"));

					TownyMessaging.sendPrefixedTownMessage(town, TownySettings.getBuyResidentPlotMsg(town.getName(), owner.getName(), 0.0));
					townBlock.setResident(null);
					townBlock.setPlotPrice(-1);

					// Set the plot permissions to mirror the towns.
					townBlock.setType(townBlock.getType());
					
					townyUniverse.getDataSource().saveResident(owner);
					// Update the townBlock data file so it's no longer using custom settings.
					townyUniverse.getDataSource().saveTownBlock(townBlock);

					return true;
				} else {
					//Should never reach here.
					throw new AlreadyRegisteredException(String.format(TownySettings.getLangString("msg_already_claimed"), owner.getName()));
				}

			} catch (NotRegisteredException e) {
				//Plot has no owner so it's the town selling it

				if (townBlock.getPlotPrice() == -1)
					throw new TownyException(TownySettings.getLangString("msg_err_plot_nfs"));
				
				double bankcap = TownySettings.getTownBankCap();
				if (bankcap > 0) {
					if (townBlock.getPlotPrice() + town.getAccount().getHoldingBalance() > bankcap)
						throw new TownyException(String.format(TownySettings.getLangString("msg_err_deposit_capped"), bankcap));
				}

				if (TownySettings.isUsingEconomy() && !resident.getAccount().payTo(townBlock.getPlotPrice(), town, "Plot - Buy From Town"))
					throw new TownyException(TownySettings.getLangString("msg_no_money_purchase_plot"));

				townBlock.setPlotPrice(-1);
				townBlock.setResident(resident);

				// Set the plot permissions to mirror the new owners.
				townBlock.setType(townBlock.getType());
				townyUniverse.getDataSource().saveTownBlock(townBlock);

				return true;
			}
		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_err_not_part_town"));
		}
	}

	private boolean residentUnclaim(WorldCoord worldCoord) throws TownyException {

		try {
			TownBlock townBlock = worldCoord.getTownBlock();

			townBlock.setResident(null);
			townBlock.setPlotPrice(townBlock.getTown().getPlotTypePrice(townBlock.getType()));

			// Set the plot permissions to mirror the towns.
			townBlock.setType(townBlock.getType());
			TownyUniverse.getInstance().getDataSource().saveTownBlock(townBlock);

			plugin.updateCache(worldCoord);

		} catch (NotRegisteredException e) {
			throw new TownyException(TownySettings.getLangString("msg_not_own_place"));
		}

		return true;
	}

	private void residentUnclaimAll() {

		List<TownBlock> selection = new ArrayList<>(resident.getTownBlocks());

		for (TownBlock townBlock : selection) {
			try {
				residentUnclaim(townBlock.getWorldCoord());
			} catch (TownyException e) {
				TownyMessaging.sendErrorMsg(player, e.getMessage());
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
			TownyUniverse townyUniverse = TownyUniverse.getInstance();
			
			townBlock.setPlotPrice(-1);
			townBlock.setResident(resident);
			townBlock.setType(townBlock.getType());
			townyUniverse.getDataSource().saveTownBlock(townBlock);
			
			TownyMessaging.sendMessage(BukkitTools.getPlayer(resident.getName()), String.format(TownySettings.getLangString("msg_admin_has_given_you_a_plot"), worldCoord.toString()));
		} catch (NotRegisteredException e) {
			//Probably not owned by a town.
			throw new TownyException(TownySettings.getLangString("msg_not_claimed_1"));			

		}
	}

}
