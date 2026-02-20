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
import org.bukkit.World;
import org.bukkit.entity.Player;

public class PermHUD implements HUDImplementer {
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

		Component build = getTranslatedOnOrOff(world.getUnclaimedZoneBuild(), translator);
		Component destroy = getTranslatedOnOrOff(world.getUnclaimedZoneDestroy(), translator);
		Component switching = getTranslatedOnOrOff(world.getUnclaimedZoneSwitch(), translator);
		Component item = getTranslatedOnOrOff(world.getUnclaimedZoneItemUse(), translator);
		Component pvp = getTranslatedOnOrOff(world.isPVP(), translator);
		Component explosions = getTranslatedOnOrOff(world.isExpl(), translator);
		Component firespread = getTranslatedOnOrOff(world.isFire(), translator);
		Component mobspawn = getTranslatedOnOrOff(world.hasWildernessMobs(), translator);

		LinkedList<Component> sbComponents = new LinkedList<>();
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("unclaimed_zone_name")));
		sbComponents.add(Component.empty());
		sbComponents.add(TownyComponents.miniMessage(Colors.YELLOW + Colors.UNDERLINED + translator.of("msg_perm_hud_title") + Colors.RESET));

		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_build") + " ").append(build));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_destroy") + " ").append(destroy));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_switch") + " ").append(switching));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_item_use") + " ").append(item));

		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_pvp") + " ").append(pvp));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_explosions") + " ").append(explosions));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_firespread") + " ").append(firespread));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_mobspawns") + " ").append(mobspawn));

		UUID uuid = p.getUniqueId();
		hud.setTitle(uuid, TownyComponents.miniMessage(HUDManager.check(getFormattedWildernessName(p.getWorld()))));
		hud.setLines(uuid, sbComponents);
	}

	public static void sendTownHUD(Player p, WorldCoord worldCoord, Translator translator, ServerHUD hud) {

		TownBlock townBlock = worldCoord.getTownBlockOrNull();
		TownBlockOwner owner = townBlock.getTownBlockOwner();
		String type = townBlock.getType().equals(TownBlockType.RESIDENTIAL) ? " " : townBlock.getType().getName();
		Component forSale = getPlotPrice(translator, townBlock, townBlock.hasPlotObjectGroup());
		TownyPermission tp = townBlock.getPermissions();
		boolean residentOwned = owner instanceof Resident;
		Component build = getPermLine(tp, ActionType.BUILD, residentOwned);
		Component destroy = getPermLine(tp, ActionType.DESTROY, residentOwned);
		Component switching = getPermLine(tp, ActionType.SWITCH, residentOwned);
		Component item = getPermLine(tp, ActionType.ITEM_USE, residentOwned);
		TownyWorld world = townBlock.getWorld();
		Component pvp = getTranslatedOnOrOff(!CombatUtil.preventPvP(world, townBlock), translator);
		Component explosions = getTranslatedOnOrOff(world.isForceExpl() || tp.explosion, translator);
		Component firespread = getTranslatedOnOrOff(world.isForceFire() || tp.fire, translator);
		Component mobspawn = getTranslatedOnOrOff(world.isForceTownMobs() || tp.mobs || townBlock.getTownOrNull().isAdminEnabledMobs(), translator);

		LinkedList<Component> sbComponents = new LinkedList<>();
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_plot_type") + type));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_plot_for_sale") + Colors.GRAY).append(forSale));
		sbComponents.add(TownyComponents.miniMessage(Colors.YELLOW + Colors.UNDERLINED + translator.of("msg_perm_hud_title") + Colors.RESET));

		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_build") + Colors.GRAY).append(build));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_destroy") + Colors.GRAY).append(destroy));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_switch") + Colors.GRAY).append(switching));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_item_use") + Colors.GRAY).append(item));

		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_pvp") + " ").append(pvp));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_explosions") + " ").append(explosions));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_firespread") + " ").append(firespread));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + translator.of("msg_perm_hud_mobspawns") + " ").append(mobspawn));

		sbComponents.add(TownyComponents.miniMessage(Colors.YELLOW + Colors.UNDERLINED  + translator.of("msg_perm_hud_key")));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + Colors.BOLD + "f " + Colors.WHITE + " - " + Colors.GRAY + translator.of("msg_perm_hud_friend") + " "
													+Colors.DARK_GREEN + Colors.BOLD + "r " + Colors.WHITE + " - " + Colors.GRAY + translator.of("msg_perm_hud_resident")));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + Colors.BOLD + "t " + Colors.WHITE + " - " + Colors.GRAY + translator.of("msg_perm_hud_town") + " "
											 		+Colors.DARK_GREEN + Colors.BOLD + "n " + Colors.WHITE + " - " + Colors.GRAY + translator.of("msg_perm_hud_nation")));
		sbComponents.add(TownyComponents.miniMessage(Colors.DARK_GREEN + Colors.BOLD + "a " + Colors.WHITE + " - " + Colors.GRAY + translator.of("msg_perm_hud_ally") + " "
											 		+Colors.DARK_GREEN + Colors.BOLD + "o " + Colors.WHITE + " - " + Colors.GRAY + translator.of("msg_perm_hud_outsider")));

		UUID uuid = p.getUniqueId();
		hud.setTitle(uuid, TownyComponents.miniMessage(Colors.GOLD + owner.getName() + (townBlock.hasResident() ? " (" + townBlock.getTownOrNull().getName() + ")" : "")));
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
		return TownyComponents.miniMessage(forSale);
	}

	private static Component getPermLine(TownyPermission tp, ActionType actionType, boolean residentOwned) {
		String v = residentOwned ? "f" : "r";
		String u = residentOwned ? "t" : "n";
		return TownyComponents.miniMessage((tp.getResidentPerm(actionType) ? v : "-") + (tp.getNationPerm(actionType) ? u : "-") +
				(tp.getAllyPerm(actionType) ? "a" : "-") + (tp.getOutsiderPerm(actionType) ? "o" : "-"));
	}

	private static Component getTranslatedOnOrOff(boolean test, Translator translator) {
		return TownyComponents.miniMessage(test ? translator.of("status_on") : translator.of("status_off"));
	}

	private static String getFormattedWildernessName(World w) {
		StringBuilder wildernessName = new StringBuilder().append(Colors.DARK_RED).append(Colors.BOLD);
		if (TownyAPI.getInstance().isTownyWorld(w)) 
			wildernessName.append(TownyAPI.getInstance().getTownyWorld(w).getFormattedUnclaimedZoneName());
		else 
			wildernessName.append("Unknown");

		return wildernessName.toString();
	}

	private static String prettyMoney(double price) {
		return TownyEconomyHandler.getFormattedBalance(price);
	}

}
