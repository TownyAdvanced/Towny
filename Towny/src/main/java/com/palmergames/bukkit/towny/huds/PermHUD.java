package com.palmergames.bukkit.towny.huds;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyEconomyHandler;
import com.palmergames.bukkit.towny.huds.providers.HUD;
import com.palmergames.bukkit.towny.huds.providers.ServerHUD;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockOwner;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownyPermission;
import com.palmergames.bukkit.towny.object.TownyPermission.ActionType;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.WorldCoord;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.Colors;

import net.kyori.adventure.text.Component;
import java.util.LinkedList;
import java.util.UUID;
import java.util.function.BiFunction;
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PermHUD implements HUDImplementer {

	private static final String BOLD = Colors.BOLD;
	private static final String DARK_GREEN = Colors.DARK_GREEN;
	private static final String DARK_RED = Colors.DARK_RED;
	private static final String GOLD = Colors.GOLD;
	private static final String GRAY = Colors.GRAY;
	private static final String RESET = Colors.RESET;
	private static final String UNDERLINED = Colors.UNDERLINED;
	private static final String WHITE = Colors.WHITE;
	private static final String YELLOW = Colors.YELLOW;

	private final static BiFunction<Boolean, Translator, Component> translateOnOff = (test, translator) -> miniMessage(test ? translator.of("status_on") : translator.of("status_off"));

	final HUD hud;

	public PermHUD(HUD hud) {
		this.hud = hud;
	}

	@Override
	public HUD getHUD() {
		return hud;
	}

	public static void updatePerms(Player p) {
		updatePerms(p, WorldCoord.parseWorldCoord(p));
	}

	public static void updatePerms(Player p, WorldCoord worldCoord) {
		Translator translator = Translator.locale(p);
		ServerHUD hud = HUDManager.getHUD("permHUD");
		if (hud == null) {
			Towny.getPlugin().getLogger().warning("PermHUD could not find permHUD from HUDManager for player: " + p.getName());
			return;
		}

		if (worldCoord.isWilderness())
			sendWildernessHUD(p, worldCoord.getTownyWorld(), translator, hud);
		else
			sendTownHUD(p, worldCoord, translator, hud);
	}

	private static void sendWildernessHUD(Player p, TownyWorld world, Translator translator, ServerHUD hud) {

		Component build = translateOnOff.apply(world.getUnclaimedZoneBuild(), translator);
		Component destroy = translateOnOff.apply(world.getUnclaimedZoneDestroy(), translator);
		Component switching = translateOnOff.apply(world.getUnclaimedZoneSwitch(), translator);
		Component item = translateOnOff.apply(world.getUnclaimedZoneItemUse(), translator);
		Component pvp = translateOnOff.apply(world.isPVP(), translator);
		Component explosions = translateOnOff.apply(world.isExpl(), translator);
		Component firespread = translateOnOff.apply(world.isFire(), translator);
		Component mobspawn = translateOnOff.apply(world.hasWildernessMobs(), translator);

		LinkedList<Component> sbComponents = new LinkedList<>();
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("unclaimed_zone_name")));
		sbComponents.add(Component.empty());
		sbComponents.add(miniMessage(YELLOW + UNDERLINED + translator.of("msg_perm_hud_title") + RESET));

		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_build")).append(build));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_destroy")).append(destroy));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_switch")).append(switching));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_item_use")).append(item));

		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_pvp")).append(pvp));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_explosions")).append(explosions));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_firespread")).append(firespread));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_mobspawns")).append(mobspawn));

		UUID uuid = p.getUniqueId();
		hud.setTitle(uuid, miniMessage(HUDManager.check(getFormattedWildernessName(p.getWorld()))));
		hud.setLines(uuid, sbComponents);
	}

	public static void sendTownHUD(Player p, WorldCoord worldCoord, Translator translator, ServerHUD hud) {

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		TownBlockOwner owner = townBlock.getTownBlockOwner();
		String type = WHITE + (townBlock.getType().equals(TownBlockType.RESIDENTIAL) ? " " : townBlock.getType().getName());
		Component forSale = getPlotPrice(translator, townBlock, townBlock.hasPlotObjectGroup());
		TownyPermission tp = townBlock.getPermissions();
		boolean residentOwned = owner instanceof Resident;
		Component build = getPermLine(tp, ActionType.BUILD, residentOwned);
		Component destroy = getPermLine(tp, ActionType.DESTROY, residentOwned);
		Component switching = getPermLine(tp, ActionType.SWITCH, residentOwned);
		Component item = getPermLine(tp, ActionType.ITEM_USE, residentOwned);
		TownyWorld world = townBlock.getWorld();
		Component pvp = translateOnOff.apply(!CombatUtil.preventPvP(world, townBlock), translator);
		Component explosions = translateOnOff.apply(world.isForceExpl() || tp.explosion, translator);
		Component firespread = translateOnOff.apply(world.isForceFire() || tp.fire, translator);
		Component mobspawn = translateOnOff.apply(world.isForceTownMobs() || tp.mobs || townBlock.getTownOrNull().isAdminEnabledMobs(), translator);

		LinkedList<Component> sbComponents = new LinkedList<>();
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_plot_type") + type));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_plot_for_sale")).append(forSale));
		sbComponents.add(miniMessage(YELLOW + UNDERLINED + translator.of("msg_perm_hud_title") + RESET));

		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_build")).append(build));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_destroy")).append(destroy));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_switch")).append(switching));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_item_use")).append(item));

		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_pvp")).append(pvp));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_explosions")).append(explosions));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_firespread")).append(firespread));
		sbComponents.add(miniMessage(DARK_GREEN + translator.of("msg_perm_hud_mobspawns")).append(mobspawn));

		sbComponents.add(miniMessage(YELLOW + UNDERLINED  + translator.of("msg_perm_hud_key")));
		sbComponents.add(formatKey("f", translator.of("msg_perm_hud_friend"), "r", translator.of("msg_perm_hud_resident")));
		sbComponents.add(formatKey("t", translator.of("msg_perm_hud_town"), "n", translator.of("msg_perm_hud_nation")));
		sbComponents.add(formatKey("a", translator.of("msg_perm_hud_ally"), "o", translator.of("msg_perm_hud_outsider")));

		UUID uuid = p.getUniqueId();
		hud.setTitle(uuid, miniMessage(GOLD + owner.getName() + (townBlock.hasResident() ? " (" + townBlock.getTownOrNull().getName() + ")" : "")));
		hud.setLines(uuid, sbComponents);
	}

	private static Component getPlotPrice(Translator translator, TownBlock townBlock, boolean plotGroup) {
		String forSale = translator.of("msg_perm_hud_no");
		if (TownyEconomyHandler.isActive()) {
			forSale = plotGroup && townBlock.getPlotObjectGroup().getPrice() > -1
				? prettyMoney(townBlock.getPlotObjectGroup().getPrice())
				: townBlock.isForSale() ? prettyMoney(townBlock.getPlotPrice()) : forSale;
		} else {
			// No economy is active but plots can be put up for sale (they're just free.)
			forSale = (plotGroup && townBlock.getPlotObjectGroup().getPrice() > -1) || (!plotGroup && townBlock.isForSale())
					? translator.of("msg_perm_hud_yes") : forSale;
		}
		return miniMessage(WHITE + forSale);
	}

	private static Component getPermLine(TownyPermission tp, ActionType actionType, boolean residentOwned) {
		String v = residentOwned ? "f" : "r";
		String u = residentOwned ? "t" : "n";
		return miniMessage(WHITE + (tp.getResidentPerm(actionType) ? v : "-") + (tp.getNationPerm(actionType) ? u : "-") +
				(tp.getAllyPerm(actionType) ? "a" : "-") + (tp.getOutsiderPerm(actionType) ? "o" : "-"));
	}

	private static Component formatKey(String symbol, String explanation, String symbol2, String explanation2) {
		return miniMessage(DARK_GREEN + BOLD + symbol + RESET + WHITE + " - " + GRAY + explanation + " " + DARK_GREEN + BOLD + symbol2 + RESET + WHITE + " - " + GRAY + explanation2);
	}

	private static String getFormattedWildernessName(World w) {
		StringBuilder wildernessName = new StringBuilder().append(DARK_RED).append(BOLD);
		if (TownyAPI.getInstance().isTownyWorld(w)) 
			wildernessName.append(TownyAPI.getInstance().getTownyWorld(w).getFormattedUnclaimedZoneName());
		else 
			wildernessName.append("Unknown");

		return wildernessName.toString();
	}

	private static Component miniMessage(String string) {
		return TownyComponents.miniMessage(string);
	}

	private static String prettyMoney(double price) {
		return TownyEconomyHandler.getFormattedBalance(price);
	}

}
