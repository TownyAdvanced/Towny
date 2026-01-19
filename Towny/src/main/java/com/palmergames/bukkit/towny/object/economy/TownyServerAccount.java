package com.palmergames.bukkit.towny.object.economy;

import java.util.UUID;

import com.palmergames.bukkit.config.ConfigNodes;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.object.EconomyHandler;
import com.palmergames.bukkit.towny.object.economy.transaction.Transaction;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.object.TownyWorld;
import org.jetbrains.annotations.ApiStatus;

/**
 * For internal use only.
 */
@ApiStatus.Internal
public final class TownyServerAccount extends Account {

	private static final UUID uuid = UUID.fromString(TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT_UUID));
	private static final String name = TownySettings.getString(ConfigNodes.ECO_CLOSED_ECONOMY_SERVER_ACCOUNT);
	private static final ThreadLocal<TownyWorld> worldLocal = ThreadLocal.withInitial(() -> TownyUniverse.getInstance().getTownyWorlds().get(0));
	public static final TownyServerAccount ACCOUNT = new TownyServerAccount();

	private TownyServerAccount() {
		super(new EconomyHandlerHolder(), name, uuid, worldLocal::get, false);
	}

	@Override
	protected synchronized boolean addMoney(double amount) {
		return addToServer(null, amount, this.getWorld());
	}

	@Override
	protected synchronized boolean subtractMoney(double amount) {
		return subtractFromServer(null, amount, this.getWorld());
	}

	/**
	 * Adds money to the server account (used for towny closed economy.)
	 * 
	 * @param account Account sending money to the server. 
	 * @param amount The amount to deposit.
	 * @param world The world of the deposit.
	 * @return A boolean indicating success.
	 */
	public static boolean addToServer(Account account, double amount, TownyWorld world) {
		worldLocal.set(world);
		
		try {
			boolean success = TownyEconomyHandler.add(ACCOUNT, amount);
			if (success)
				BukkitTools.fireEvent(Transaction.add(amount).paidBy(account).paidToServer().asTownyTransactionEvent());

			return success;
		} finally {
			worldLocal.remove();
		}
	}

	/**
	 * Removes money to the server account (used for towny closed economy.)
	 *
	 * @param amount The amount to withdraw.
	 * @param world The world of the withdraw.
	 * @return A boolean indicating success.
	 */
	public static boolean subtractFromServer(Account account, double amount, TownyWorld world) {
		worldLocal.set(world);
		
		try {
			boolean success = TownyEconomyHandler.subtract(ACCOUNT, amount);
			if (success)
				BukkitTools.fireEvent(Transaction.subtract(amount).paidByServer().paidTo(account).asTownyTransactionEvent());

			return success;
		} finally {
			worldLocal.remove();
		}
	}
	
	private static final class EconomyHandlerHolder implements EconomyHandler {
		@Override
		public Account getAccount() {
			return ACCOUNT;
		}

		@Override
		public String getName() {
			return name;
		}
	}
}
