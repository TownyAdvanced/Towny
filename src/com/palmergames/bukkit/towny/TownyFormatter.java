package com.palmergames.bukkit.towny;

import com.palmergames.bukkit.towny.event.statusscreen.NationStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.ResidentStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownBlockStatusScreenEvent;
import com.palmergames.bukkit.towny.event.statusscreen.TownStatusScreenEvent;
import com.palmergames.bukkit.towny.object.Coord;
import com.palmergames.bukkit.towny.object.Government;
import com.palmergames.bukkit.towny.object.Nameable;
import com.palmergames.bukkit.towny.object.Nation;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.ResidentList;
import com.palmergames.bukkit.towny.object.SpawnLocation;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.TownBlockType;
import com.palmergames.bukkit.towny.object.TownBlockTypeCache;
import com.palmergames.bukkit.towny.object.TownBlockTypeHandler;
import com.palmergames.bukkit.towny.object.TownyObject;
import com.palmergames.bukkit.towny.object.TownyWorld;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.towny.object.Translator;
import com.palmergames.bukkit.towny.object.metadata.CustomDataField;
import com.palmergames.bukkit.towny.object.statusscreens.StatusScreen;
import com.palmergames.bukkit.towny.permissions.TownyPerms;
import com.palmergames.bukkit.towny.utils.CombatUtil;
import com.palmergames.bukkit.towny.utils.MoneyUtil;
import com.palmergames.bukkit.towny.utils.ResidentUtil;
import com.palmergames.bukkit.towny.utils.TownRuinUtil;
import com.palmergames.bukkit.towny.utils.TownyComponents;
import com.palmergames.bukkit.util.BukkitTools;
import com.palmergames.bukkit.util.ChatTools;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.util.StringMgmt;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class TownyFormatter {
	public static final SimpleDateFormat lastOnlineFormat = new SimpleDateFormat("MMMMM dd '@' HH:mm");
	public static final SimpleDateFormat lastOnlineFormatIncludeYear = new SimpleDateFormat("MMMMM dd yyyy");
	public static final SimpleDateFormat registeredFormat = new SimpleDateFormat("MMM d yyyy");
	public static final SimpleDateFormat fullDateFormat = new SimpleDateFormat("MMMMM dd yyyy '@' HH:mm");

	/**
	 * 1 = Description 2 = Count
	 * 
	 * Colours: 3 = Description and : 4 = Count 5 = Colour for the start of the
	 * list
	 */
	public static final String listPrefixFormat = "%3$s%1$s %4$s[%2$d]%3$s:%5$s ";
	public static final String keyValueFormat = "%s%s %s%s";
	public static final String keyFormat = "%s%s";
	public static final String hoverFormat = "%s[%s%s%s]";
	public static final String bracketFormat = " %1$s[%2$s %3$s%1$s]";
	
	public static void initialize() {}

	/*
	 * TownyObject StatusScreen makers.
	 */
	
	/**
	 * Gets the status screen of a TownBlock
	 * 
	 * @param townBlock the TownBlock to check
	 * @param player Player who will be sent this status.   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(TownBlock townBlock, Player player) {

		StatusScreen screen = new StatusScreen(player);
		final Translator translator = Translator.locale(Translation.getLocale(player));
		
		TownyObject owner;
		Town town = townBlock.getTownOrNull();
		TownyWorld world = townBlock.getWorld();
		boolean preventPVP = CombatUtil.preventPvP(world, townBlock);

		if (townBlock.hasResident())
			owner = townBlock.getResidentOrNull();
		else
			owner = town;

		screen.addComponentOf("townblock_title", ChatTools.formatTitle("(" + townBlock.getCoord().toString() + ") " + owner.getFormattedName() + ((playerIsOnlineAndVisible(owner.getName(), player)) ? translator.of("online") : "")));
		if (townBlock.getClaimedAt() > 0)
			screen.addComponentOf("claimedat", colourKeyValue(translator.of("msg_plot_perm_claimed_at"), registeredFormat.format(townBlock.getClaimedAt())));
		if (!townBlock.getType().equals(TownBlockType.RESIDENTIAL))
			screen.addComponentOf("townblock_plotType", colourKeyValue(translator.of("status_plot_type"), townBlock.getType().toString()));
		screen.addComponentOf("perm", colourKeyValue(translator.of("status_perm"), ((owner instanceof Resident) ? townBlock.getPermissions().getColourString().replace("n", "t") : townBlock.getPermissions().getColourString().replace("f", "r"))));
		screen.addComponentOf("pvp", colourKeyValue(translator.of("status_pvp"), ((!preventPVP) ? translator.of("status_on"): translator.of("status_off")))); 
		screen.addComponentOf("explosion", colourKeyValue(translator.of("explosions"), ((world.isForceExpl() || townBlock.getPermissions().explosion) ? translator.of("status_on"): translator.of("status_off")))); 
		screen.addComponentOf("firespread", colourKeyValue(translator.of("firespread"), ((town.isFire() || world.isForceFire() || townBlock.getPermissions().fire) ? translator.of("status_on"):translator.of("status_off")))); 
		screen.addComponentOf("mobspawns", colourKeyValue(translator.of("mobspawns"), ((world.isForceTownMobs() || townBlock.getPermissions().mobs) ?  translator.of("status_on"): translator.of("status_off"))));

		if (townBlock.hasPlotObjectGroup())
			screen.addComponentOf("plotgroup", colourKey(translator.of("status_plot_group_name_and_size", townBlock.getPlotObjectGroup().getName(), townBlock.getPlotObjectGroup().getTownBlocks().size())));
		
		if (townBlock.getTrustedResidents().size() > 0)
			screen.addComponentOf("trusted", getFormattedTownyObjects(translator.of("status_trustedlist"), new ArrayList<>(townBlock.getTrustedResidents())));
		
		// Add any metadata which opt to be visible.
		List<Component> fields = getExtraFields(townBlock);
		if (!fields.isEmpty()) {
			TextComponent comp = Component.empty();
			for (Component fieldComp : fields) {
				comp = comp.append(Component.newline()).append(fieldComp);
			}
			screen.addComponentOf("extraFields", comp);
		}
		
		TownBlockStatusScreenEvent event = new TownBlockStatusScreenEvent(screen, player, townBlock);
		Bukkit.getPluginManager().callEvent(event);
		if (event.hasAdditionalLines()) {
			TextComponent comp = Component.empty();
			for (int i = 0; i < event.getAdditionalLines().size(); i++)
				comp = comp.append(Component.newline()).append(event.getAdditionalLines().get(i));
			screen.addComponentOf("eventAddedLines", comp);
		}
		
		return screen;
	}

	/**
	 *  Gets the status screen of a Resident
	 *  
	 * @param resident the resident to check the status of
	 * @param sender The sender who executed the command.
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(Resident resident, CommandSender sender) {

		StatusScreen screen = new StatusScreen(sender);
		final Translator translator = Translator.locale(Translation.getLocale(sender));

		// ___[ King Harlus ]___
		screen.addComponentOf("title", ChatTools.formatTitle(resident.getFormattedName() + (playerIsOnlineAndVisible(resident.getName(), sender) ? translator.of("online2") : "")));

		// First used if last online is this year, 2nd used if last online is early than this year.
		// Registered: Sept 3 2009 | Last Online: March 7 @ 14:30
		// Registered: Sept 3 2009 | Last Online: March 7 2009
		screen.addComponentOf("registered", getResidentRegisteredLine(resident, translator));
		if (!resident.isNPC())
			screen.addComponentOf("lastonline", getResidentLastOnline(resident, translator));
		
		// Town: Camelot
		Component townLine = colourKeyValue(translator.of("status_town"), (!resident.hasTown() ? translator.comp("status_no_town") : Component.text(resident.getTownOrNull().getFormattedName()).append(formatPopulationBrackets(resident.getTownOrNull().getResidents().size()))));
		if (!resident.hasTown())
			screen.addComponentOf("town", townLine);
		else {
			Town town = resident.getTownOrNull();
			List<Component> residents = getFormattedNames(town.getResidents());
			if (residents.size() > 34)
				residents = shortenOverlengthArray(residents, 35, translator);
			screen.addComponentOf("town", townLine
				.hoverEvent(HoverEvent.showText(TownyComponents.miniMessage(Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingTown(), town.getFormattedName())))
					.append(Component.newline())
					.append(getResidentJoinedTownDate(resident, translator))
					.append(Component.newline())
					.append(colourKeyValue(translator.of("rank_list_mayor"), town.getMayor().getFormattedName()))
					.append(Component.newline())
					.append(colourKeyValue(translator.of("res_list"), StringMgmt.join(residents, ", ")))
					.append(Component.newline())
					.append(translator.comp("status_hover_click_for_more"))))
				.clickEvent(ClickEvent.runCommand("/towny:town " + town.getName()))
			);
		}
		
		// Nation: Azur Empire
		if (resident.hasNation()) {
			Nation nation = resident.getNationOrNull();
			// Shown in Hover Text: Towns [44]: James City, Carry Grove, Mason Town
			List<Component> towns = getFormattedNames(nation.getTowns());
			if (towns.size() > 10)
				towns = shortenOverlengthArray(towns, 11, translator);

			screen.addComponentOf("nation", colourKeyValue(translator.of("status_town_nation"), nation.getName() + formatPopulationBrackets(nation.getTowns().size()))
					.hoverEvent(HoverEvent.showText(TownyComponents.miniMessage(Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingNation(), nation.getFormattedName())))
							.append(Component.newline())
							.append(colourKeyValue(translator.of("status_nation_king"), nation.getCapital().getMayor().getFormattedName()))
							.append(Component.newline())
							.append(colourKeyValue(translator.of("town_plu"), StringMgmt.join(towns, ", ")))
							.append(Component.newline())
							.append(translator.comp("status_hover_click_for_more"))))
					.clickEvent(ClickEvent.runCommand("/towny:nation " + nation.getName()))
					);
		}
		
		// Bank: 534 coins
				if (TownyEconomyHandler.isActive())
					screen.addComponentOf("bank", colourKeyValue(translator.of("status_bank"), resident.getAccount().getHoldingFormattedBalance())
						.hoverEvent(HoverEvent.showText(translator.comp("status_hover_click_for_more")))
						.clickEvent(ClickEvent.runCommand("/towny:resident tax " + resident.getName())));
		
		// Owner of: 4 plots
		// Perm: Build = f-- Destroy = fa- Switch = fao Item = ---
		screen.addComponentOf("ownsXPlots", colourKey(translator.of("owner_of_x_plots", resident.getTownBlocks().size())));
		screen.addComponentOf("perm", colourKeyValue(translator.of("status_perm"), resident.getPermissions().getColourComponent().replaceText(TextReplacementConfig.builder().match("n").replacement("t").build())));
		screen.addComponentOf("pvp", colourKeyValue(translator.of("status_pvp"), (resident.getPermissions().pvp) ? translator.of("status_on"): translator.of("status_off")));
		screen.addComponentOf("explosions", colourKeyValue(translator.of("explosions"), (resident.getPermissions().explosion) ? translator.of("status_on"): translator.of("status_off"))); 
		screen.addComponentOf("firespread", colourKeyValue(translator.of("firespread"), (resident.getPermissions().fire) ? translator.of("status_on"): translator.of("status_off"))); 
		screen.addComponentOf("mobspawns", colourKeyValue(translator.of("mobspawns"), (resident.getPermissions().mobs) ? translator.of("status_on"): translator.of("status_off")));
		
		if (resident.isNPC()) {
			screen.addComponentOf("npcstatus", translator.of("msg_status_npc", resident.getName()));
			// Add any metadata which opt to be visible.
			List<Component> fields = getExtraFields(resident);
			if (!fields.isEmpty()) {
				TextComponent comp = Component.empty();
				for (Component fieldComp : fields) {
					comp = comp.append(Component.newline()).append(fieldComp);
				}
				screen.addComponentOf("extraFields", comp);
			}
			return screen;
		}
		
		// Embassies in: Camelot, London, Tokyo.
		List<Town> townEmbassies = getResidentsEmbassyTowns(resident);
		if (townEmbassies.size() > 0)
			screen.addComponentOf("embassiesInTowns", getFormattedTownyObjects(translator.of("status_embassy_town"), new ArrayList<>(townEmbassies)));
			
		// Town ranks
		if (resident.hasTown() && !resident.getTownRanks().isEmpty())
			screen.addComponentOf("townRanks", colourKeyValue(translator.of("status_town_ranks"), StringMgmt.capitalize(StringMgmt.join(resident.getTownRanks(), ", "))));
		
		//Nation ranks
		if (resident.hasNation() && !resident.getNationRanks().isEmpty())
			screen.addComponentOf("nationRanks", colourKeyValue(translator.of("status_nation_ranks"),StringMgmt.capitalize(StringMgmt.join(resident.getNationRanks(), ", "))));
		
		// Jailed: yes if they are jailed.
		if (resident.isJailed())
			screen.addComponentOf("jailLine", getResidentJailedLine(resident, translator));
		
		// Friends [12]: James, Carry, Mason
		if (resident.getFriends() != null && !resident.getFriends().isEmpty())
			screen.addComponentOf("friendsLine", getFormattedTownyObjects(translator.of("status_friends"), new ArrayList<>(resident.getFriends())));
		
		// Add any metadata which opt to be visible.
		List<Component> fields = getExtraFields(resident);
		if (!fields.isEmpty()) {
			TextComponent comp = Component.empty();
			for (Component fieldComp : fields) {
				comp = comp.append(Component.newline()).append(fieldComp);
			}
			screen.addComponentOf("extraFields", comp);
		}
			
		ResidentStatusScreenEvent event = new ResidentStatusScreenEvent(screen, sender, resident);
		Bukkit.getPluginManager().callEvent(event);
		if (event.hasAdditionalLines()) {
			Component eventAddedLines = Component.empty();
			for (Component additionalLine : event.getAdditionalLines())
				eventAddedLines = eventAddedLines.append(Component.newline()).append(additionalLine);
			
			screen.addComponentOf("eventAddedLines", eventAddedLines);
		}
		return screen;
	}

	/**
	 * Gets the status screen of a Town.
	 * 
	 * @param town the town in which to check.
	 * @param sender CommandSender who will be sent the status.   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(Town town, CommandSender sender) {

		final Locale locale = Translation.getLocale(sender);
		final Translator translator = Translator.locale(locale);
		StatusScreen screen = new StatusScreen(sender);
		TownyWorld world = town.getHomeblockWorld();

		// ___[ Raccoon City ]___
		screen.addComponentOf("title", ChatTools.formatTitle(town));
		
		// (PvP) (Open) (Peaceful)
		List<String> sub = getTownSubtitle(town, world, translator);
		if (!sub.isEmpty())
			screen.addComponentOf("subtitle", ChatTools.formatSubTitle(TownyComponents.miniMessage(StringMgmt.join(sub, " "))));
		
		// Board: Get your fried chicken
		if (town.getBoard() != null && !town.getBoard().isEmpty())
			screen.addComponentOf("board", colourKeyValue(translator.of("status_town_board"), town.getBoard()));

		// Created Date
		if (town.getRegistered() != 0) 
			screen.addComponentOf("registered", colourKeyValue(translator.of("status_founded"), registeredFormat.format(town.getRegistered())));

		// Town Size: 0 / 16 [Bought: 0/48] [Bonus: 0] [Home: 33,44]
		if (!town.hasUnlimitedClaims())
			screen.addComponentOf("townblocks", colourKeyValue(translator.of("status_town_size"), translator.of("status_fractions", town.getTownBlocks().size(), TownySettings.getMaxTownBlocks(town)))
				.append(TownySettings.isSellingBonusBlocks(town) ? colourBracketElement(translator.of("status_town_size_bought"), translator.of("status_fractions", town.getPurchasedBlocks(), TownySettings.getMaxPurchasedBlocks(town))) : Component.empty())
				.append(town.getBonusBlocks() > 0 ? colourBracketElement(translator.of("status_town_size_bonus"), String.valueOf(town.getBonusBlocks())) : Component.empty())
				.append(TownySettings.getNationBonusBlocks(town) > 0 ? colourBracketElement(translator.of("status_town_size_nationbonus"), String.valueOf(TownySettings.getNationBonusBlocks(town))) : Component.empty()));
		// Town Size: 0 / ∞
		else
			screen.addComponentOf("townblocks", colourKeyValue(translator.of("status_town_size"), translator.of("status_fractions", town.getTownBlocks().size(), town.getMaxTownBlocksAsAString())));

		if (town.isPublic()) {
			Component homeComponent = TownyComponents.miniMessage(translator.of("status_home_element",
				(TownySettings.getTownDisplaysXYZ() ?
					(town.hasSpawn() ? BukkitTools.convertCoordtoXYZ(town.getSpawnOrNull()) : translator.of("status_no_town")) :
					(town.hasHomeBlock() ? town.getHomeBlockOrNull().getCoord().toString() : translator.of("status_no_town"))
			)));

			String webUrl = formatWebUrl(town);
			if (!webUrl.isEmpty())
				homeComponent = homeComponent.clickEvent(ClickEvent.openUrl(webUrl)).hoverEvent(HoverEvent.showText(TownyComponents.miniMessage(translator.of("msg_view_on_web"))));

			screen.addComponentOf("home", homeComponent);
		}

		// Outposts: 3
		if (TownySettings.isAllowingOutposts()) {
			Component outpostLine = Component.empty();
			if (TownySettings.isOutpostsLimitedByLevels()) {
				outpostLine = colourKeyValue(translator.of("status_town_outposts"), translator.of("status_fractions", town.getMaxOutpostSpawn(), town.getOutpostLimit()));
				if (town.hasNation()) {
					int nationBonus = (Integer) TownySettings.getNationLevel(town.getNationOrNull()).get(TownySettings.NationLevel.NATION_BONUS_OUTPOST_LIMIT);
					if (nationBonus > 0)					
						outpostLine.append(colourBracketElement(translator.of("status_town_size_nationbonus"), String.valueOf(nationBonus)));
				}
			} else if (town.hasOutpostSpawn()) {
				outpostLine = colourKeyValue(translator.of("status_town_outposts"), String.valueOf(town.getMaxOutpostSpawn()));
			}
			screen.addComponentOf("outposts", outpostLine
				.hoverEvent(HoverEvent.showText(translator.comp("status_hover_click_for_more")))
				.clickEvent(ClickEvent.runCommand("/towny:town outpost list")));
		}

		// Permissions: B=rnao D=---- S=rna- I=rnao
		screen.addComponentOf("perm", colourKeyValue(translator.of("status_perm"), town.getPermissions().getColourString().replace("f", "r")));
		screen.addComponentOf("explosion", colourKeyValue(translator.of("explosions"), (town.isBANG() || world.isForceExpl()) ? translator.of("status_on"): translator.of("status_off")));
		screen.addComponentOf("firespread", colourKeyValue(translator.of("firespread"), (town.isFire() || world.isForceFire()) ? translator.of("status_on"): translator.of("status_off"))); 
		screen.addComponentOf("mobspawns", colourKeyValue(translator.of("mobspawns"), (town.hasMobs() || world.isForceTownMobs()) ? translator.of("status_on"): translator.of("status_off")));

		if (TownySettings.getTownRuinsEnabled() && town.isRuined()) {
			screen.addComponentOf("ruinedTime", colourKey(translator.of("msg_time_remaining_before_full_removal", TownySettings.getTownRuinsMaxDurationHours() - TownRuinUtil.getTimeSinceRuining(town))));
			if (TownySettings.getTownRuinsReclaimEnabled()) {
				if (TownRuinUtil.getTimeSinceRuining(town) < TownySettings.getTownRuinsMinDurationHours())
					screen.addComponentOf("reclaim", colourKeyImportant(translator.of("msg_time_until_reclaim_available", TownySettings.getTownRuinsMinDurationHours() - TownRuinUtil.getTimeSinceRuining(town))));
				else 
					screen.addComponentOf("reclaim", colourKeyImportant(translator.of("msg_reclaim_available")));
			}
			// Only display the remaining fields if town is not ruined
		} else {
			// | Bank: 534 coins
			if (TownyEconomyHandler.isActive())
				addTownMoneyComponents(town, translator, screen);

			// Mayor: MrSand
			screen.addComponentOf("mayor", colourKeyValue(translator.of("rank_list_mayor"), town.getMayor().getFormattedName())
				.hoverEvent(HoverEvent.showText(Component.text(translator.of("registered_last_online", registeredFormat.format(town.getMayor().getRegistered()), lastOnlineFormatIncludeYear.format(town.getMayor().getLastOnline())))
					.append(Component.newline())
					.append(translator.comp("status_hover_click_for_more"))))
				.clickEvent(ClickEvent.runCommand("/towny:resident " + town.getMayor().getName()))
			);

			// Nation: Azur Empire
			if (town.hasNation()) {
				// Shown in Hover Text: Towns [44]: James City, Carry Grove, Mason Town
				List<Component> towns = getFormattedNames(town.getNationOrNull().getTowns());
				if (towns.size() > 10)
					towns = shortenOverlengthArray(towns, 11, translator);

				Component hover = TownyComponents.miniMessage(Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingNation(), town.getNationOrNull().getFormattedName())))
						.append(Component.newline())
						.append(getTownJoinedNationDate(town, translator))
						.append(Component.newline())
						.append(colourKeyValue(translator.of("status_nation_king"), town.getNationOrNull().getCapital().getMayor().getFormattedName()))
						.append(Component.newline())
						.append(colourKeyValue(translator.of("town_plu"), StringMgmt.join(towns, ", ")));
				int nationZoneSize = town.getNationZoneSize();
				if (nationZoneSize > 0)
					hover = hover.append(Component.newline().append(colourKeyValue(translator.of("status_nation_zone_size"), town.isNationZoneEnabled() ? String.valueOf(nationZoneSize) : translator.of("status_off_bad"))));
				hover = hover.append(Component.newline())
						.append(translator.comp("status_hover_click_for_more"));
				
				screen.addComponentOf("nation", colourKeyValue(translator.of("status_town_nation"), town.getNationOrNull().getName() + formatPopulationBrackets(town.getNationOrNull().getTowns().size()))
					.hoverEvent(hover.asHoverEvent())
					.clickEvent(ClickEvent.runCommand("/towny:nation " + town.getNationOrNull().getName()))
				);
			}
			
			screen.addComponentOf("newline", Component.newline());
			// Assistants [2]: Sammy, Ginger
			List<Component> rankList = getRanks(town, locale);
			Component ranks = Component.empty();

			for (Component rankComponent : rankList)
				ranks = ranks.append(rankComponent).append(Component.newline());
			
			if (rankList.size() > 0)
				screen.addComponentOf("townranks", colourHoverKey(translator.of("status_rank_list"))
					.hoverEvent(HoverEvent.showText(ranks
						.append(translator.comp("status_hover_click_for_more"))))
					.clickEvent(ClickEvent.runCommand("/towny:town ranklist " + town.getName())));

			// Residents [12]: James, Carry, Mason
			List<Component> residents = getFormattedNames(town.getResidents());
			if (residents.size() > 34)
				residents = shortenOverlengthArray(residents, 35, translator);
			screen.addComponentOf("residents", colourHoverKey(translator.of("res_list"))
				.hoverEvent(HoverEvent.showText(getFormattedComponent(translator.of("res_list"), residents, town.getResidents().size())
					.append(Component.newline())
					.append(Component.text(translator.of("status_hover_click_for_more")))))
				.clickEvent(ClickEvent.runCommand("/towny:town reslist " + town.getName())));
			
			// Plots 
			TextComponent text = Component.empty();
			Map<TownBlockType, Integer> cache = town.getTownBlockTypeCache().getCache(TownBlockTypeCache.CacheType.ALL);
			for (TownBlockType type : TownBlockTypeHandler.getTypes().values()) {
				int value = cache.getOrDefault(type, 0);
				text = text.append(colourKeyValue(translator.of("status_plot_hover", type.getFormattedName()), String.valueOf(value))).append(Component.newline());
			}
			text = text.append(Component.text(translator.of("status_hover_click_for_more")));
			screen.addComponentOf("plots", colourHoverKey(translator.of("status_plot_string"))
				.hoverEvent(HoverEvent.showText(text))
				.clickEvent(ClickEvent.runCommand("/towny:town plots " + town.getName())));

		}
		
		// Add any metadata which opt to be visible.
		List<Component> fields = getExtraFields(town);
		if (!fields.isEmpty()) {
			TextComponent comp = Component.empty();
			for (Component fieldComp : fields) {
				comp = comp.append(Component.newline()).append(fieldComp);
			}
			screen.addComponentOf("extraFields", comp);
		}
			
		TownStatusScreenEvent event = new TownStatusScreenEvent(screen, sender, town);
		Bukkit.getPluginManager().callEvent(event);
		if (event.hasAdditionalLines()) {
			TextComponent comp = Component.empty();
			for (int i = 0; i < event.getAdditionalLines().size(); i++)
				comp = comp.append(Component.newline()).append(event.getAdditionalLines().get(i));
			screen.addComponentOf("eventAddedLines", comp);
		}
		return screen;
	}

	/**
	 * Gets the status screen of a Nation.
	 * 
	 * @param nation the nation to check against.
	 * @param sender CommandSender who will be sent the status.   
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(final Nation nation, final CommandSender sender) {

		StatusScreen screen = new StatusScreen(sender);
		Locale locale = Translation.getLocale(sender);
		final Translator translator = Translator.locale(locale);

		// ___[ Azur Empire (Open)]___
		screen.addComponentOf("nation_title", ChatTools.formatTitle(nation));
		List<String> sub = getNationSubtitle(nation, translator);
		if (!sub.isEmpty())
			screen.addComponentOf("subtitle", ChatTools.formatSubTitle(TownyComponents.miniMessage(StringMgmt.join(sub, " "))));

		// Board: Get your fried chicken
		if (nation.getBoard() != null &&!nation.getBoard().isEmpty())
			screen.addComponentOf("board", colourKeyValue(translator.of("status_town_board"), nation.getBoard()));
		
		// Created Date
		long registered = nation.getRegistered();
		if (registered != 0)
			screen.addComponentOf("registered", colourKeyValue(translator.of("status_founded"), registeredFormat.format(nation.getRegistered())));

		// Bank: 534 coins
		if (TownyEconomyHandler.isActive())
			addNationMoneyComponentsToScreen(nation, translator, screen);

		if (nation.isPublic()) {
			Component homeComponent = TownyComponents.miniMessage(translator.of("status_home_element", (nation.hasSpawn() ? Coord.parseCoord(nation.getSpawnOrNull()).toString() : translator.of("status_no_town"))));
			
			String webUrl = formatWebUrl(nation);
			if (!webUrl.isEmpty())
				homeComponent = homeComponent.clickEvent(ClickEvent.openUrl(webUrl)).hoverEvent(HoverEvent.showText(Component.text(translator.of("msg_view_on_web"), NamedTextColor.GRAY)));

			screen.addComponentOf("home", homeComponent);
		}

		// King: King Harlus
		if (nation.getNumTowns() > 0 && nation.hasCapital() && nation.getCapital().hasMayor()) {
			Resident king = nation.getCapital().getMayor();
			screen.addComponentOf("king", colourKeyValue(translator.of("status_nation_king"), king.getFormattedName())
				.hoverEvent(HoverEvent.showText(TownyComponents.miniMessage(translator.of("registered_last_online", registeredFormat.format(king.getRegistered()), lastOnlineFormatIncludeYear.format(king.getLastOnline())))
					.append(Component.newline())
					.append(translator.comp("status_hover_click_for_more"))))
				.clickEvent(ClickEvent.runCommand("/towny:resident " + king.getName()))
			);

			// Capital: Big City
			Town capital = nation.getCapital();
			List<Component> residents = getFormattedNames(capital.getResidents());
			if (residents.size() > 34)
				residents = shortenOverlengthArray(residents, 35, translator);
			screen.addComponentOf("capital", colourKeyValue(translator.of("status_capital"), nation.getCapital().getFormattedName())
				.hoverEvent(HoverEvent.showText(TownyComponents.miniMessage(Colors.translateColorCodes(String.format(TownySettings.getPAPIFormattingTown(), capital.getFormattedName())))
					.append(Component.newline())
					.append(colourKeyValue(translator.of("rank_list_mayor"), king.getFormattedName()))
					.append(Component.newline())
					.append(colourKeyValue(translator.of("res_list"), StringMgmt.join(residents, ", ")))
					.append(Component.newline())
					.append(translator.comp("status_hover_click_for_more"))))
				.clickEvent(ClickEvent.runCommand("/towny:town " + capital.getName()))
			);
			
			// Nation Zone Size: 3
			if (TownySettings.getNationZonesEnabled())
				screen.addComponentOf("nationzone", colourKeyValue(translator.of("status_nation_zone_size"), String.valueOf(nation.getNationZoneSize())));
		}
		
		screen.addComponentOf("newline", Component.newline());
		// Assistants [2]: Sammy, Ginger
		List<Component> rankList = getRanks(nation, locale);
		Component ranks = Component.empty();
		
		for (Component rankComponent : rankList)
			ranks = ranks.append(rankComponent).append(Component.newline());
		
		if (rankList.size() > 0)
			screen.addComponentOf("nationranks", colourHoverKey(translator.of("status_rank_list"))
				.hoverEvent(HoverEvent.showText(ranks.append(translator.comp("status_hover_click_for_more"))))
				.clickEvent(ClickEvent.runCommand("/towny:nation ranklist " + nation.getName())));
		
		// Towns [44]: James City, Carry Grove, Mason Town
		List<Component> towns = getFormattedNames(nation.getTowns());
		if (towns.size() > 10)
			towns = shortenOverlengthArray(towns, 11, translator);
		screen.addComponentOf("towns", colourHoverKey(translator.of("status_nation_towns"))
			.hoverEvent(HoverEvent.showText(getFormattedComponent(translator.of("status_nation_towns"), towns, nation.getTowns().size())
				.append(Component.newline())
				.append(TownyComponents.miniMessage(translator.of("status_hover_click_for_more")))))
			.clickEvent(ClickEvent.runCommand("/towny:nation townlist " + nation.getName()))
		);
		
		// Allies [4]: James Nation, Carry Territory, Mason Country
		List<Component> allies = getFormattedNames(nation.getAllies());
		if (allies.size() > 10)
			allies = shortenOverlengthArray(allies, 11, translator);
		if (allies.size() > 0)
			screen.addComponentOf("allies", colourHoverKey(translator.of("status_nation_allies"))
				.hoverEvent(HoverEvent.showText(getFormattedComponent(translator.of("status_nation_allies"), allies, nation.getAllies().size())
					.append(Component.newline())
					.append(TownyComponents.miniMessage(translator.of("status_hover_click_for_more")))))
				.clickEvent(ClickEvent.runCommand("/towny:nation allylist " + nation.getName()))
			);

		// Enemies [4]: James Nation, Carry Territory, Mason Country
		List<Component> enemies = getFormattedNames(nation.getEnemies());
		if (enemies.size() > 10)
			enemies = shortenOverlengthArray(enemies, 11, translator);
		if (enemies.size() > 0)
			screen.addComponentOf("enemies", colourHoverKey(translator.of("status_nation_enemies"))
				.hoverEvent(HoverEvent.showText(getFormattedComponent(translator.of("status_nation_enemies"), enemies, nation.getEnemies().size())
					.append(Component.newline())
					.append(translator.comp("status_hover_click_for_more"))))
				.clickEvent(ClickEvent.runCommand("/towny:nation enemylist " + nation.getName()))
			);

		// Add any metadata which opt to be visible.
		List<Component> fields = getExtraFields(nation);
		if (!fields.isEmpty()) {
			TextComponent comp = Component.empty();
			for (Component fieldComp : fields) {
				comp = comp.append(Component.newline()).append(fieldComp);
			}
			screen.addComponentOf("extraFields", comp);
		}
		
		NationStatusScreenEvent event = new NationStatusScreenEvent(screen, sender, nation);
		Bukkit.getPluginManager().callEvent(event);
		if (event.hasAdditionalLines()) {
			TextComponent comp = Component.empty();
			for (int i = 0; i < event.getAdditionalLines().size(); i++)
				comp = comp.append(Component.newline()).append(event.getAdditionalLines().get(i));
			screen.addComponentOf("eventAddedLines", comp);
		}
		return screen;
	}


	/**
	 * Gets the status screen for a World.
	 * 
	 * @param world the world to check.
	 * @param sender CommandSender who will be sent the status.
	 * @return StatusScreen containing the results.
	 */
	public static StatusScreen getStatus(TownyWorld world, CommandSender sender) {

		StatusScreen screen = new StatusScreen(sender);
		final Translator translator = Translator.locale(Translation.getLocale(sender));

		// ___[ World (PvP) ]___
		screen.addComponentOf("townyworld_title", ChatTools.formatTitle(world.getFormattedName()));
		screen.addComponentOf("subtitle", ChatTools.formatSubTitle(getWorldSubtitle(world, translator)));

		if (!world.isUsingTowny()) {
			screen.addComponentOf("not_using_towny", translator.comp("msg_set_use_towny_off"));
		} else {
			// War will be allowed in this world.
			screen.addComponentOf("war_allowed", colourKey(world.isWarAllowed() ? translator.of("msg_set_war_allowed_on") : translator.of("msg_set_war_allowed_off")));
			// ForcePvP: ON | FriendlyFire: ON 
			screen.addComponentOf("pvp", colourKeyValue(translator.of("status_world_forcepvp"), (world.isForcePVP() ? translator.of("status_on") : translator.of("status_off")))
				.append(translator.comp("status_splitter"))
				.append(colourKeyValue(translator.of("status_world_friendlyfire"), (world.isFriendlyFireEnabled() ? translator.of("status_on") : translator.of("status_off")))));
			// Fire: ON | ForceFire: ON
			screen.addComponentOf("fire", colourKeyValue(translator.of("status_world_fire"), (world.isFire() ? translator.of("status_on") : translator.of("status_off")))
				.append(translator.comp("status_splitter"))
				.append(colourKeyValue(translator.of("status_world_forcefire"), (world.isForceFire() ? translator.of("status_forced") : translator.of("status_adjustable")))));
			// Explosion: ON | ForceExplosion: ON
			screen.addComponentOf("explosions", colourKeyValue(translator.of("explosions"), (world.isExpl() ? translator.of("status_on") : translator.of("status_off")))
				.append(translator.comp("status_splitter"))
				.append(colourKeyValue(translator.of("status_world_forceexplosion"), (world.isForceExpl() ? translator.of("status_forced") : translator.of("status_adjustable")))));
			// WorldMobs: ON | Wilderness Mobs: ON
			screen.addComponentOf("mobs", colourKeyValue(translator.of("status_world_worldmobs"), (world.hasWorldMobs() ? translator.of("status_on") : translator.of("status_off")))
				.append(translator.comp("status_splitter"))
				.append(colourKeyValue(translator.of("status_world_wildernessmobs"), (world.hasWildernessMobs() ? translator.of("status_on") : translator.of("status_off")))));
			// ForceTownMobs: ON
			screen.addComponentOf("townmobs", colourKeyValue(translator.of("status_world_forcetownmobs"), (world.isForceTownMobs() ? translator.of("status_forced") : translator.of("status_adjustable"))));
			// Unclaim Revert: ON
			screen.addComponentOf("unclaim_revert", colourKeyValue(translator.of("status_world_unclaimrevert"), (world.isUsingPlotManagementRevert() ? translator.of("status_on_good") : translator.of("status_off_bad")))); 
			// Entity Explosion Revert: ON | Block Explosion Revert: ON
			screen.addComponentOf("explosion_reverts", colourKeyValue(translator.of("status_world_explrevert_entity"), (world.isUsingPlotManagementWildEntityRevert() ? translator.of("status_on_good") : translator.of("status_off_bad")))
				.append(translator.comp("status_splitter"))
				.append(colourKeyValue(translator.of("status_world_explrevert_block"), (world.isUsingPlotManagementWildBlockRevert() ? translator.of("status_on_good") : translator.of("status_off_bad")))));
			// Plot Clear Block Delete: ON (see /towny plotclearblocks) | OFF
			screen.addComponentOf("plot_clear", colourKeyValue(translator.of("status_plot_clear_deletion"), (world.isUsingPlotManagementMayorDelete() ? translator.of("status_on") + " <green>(see /towny plotclearblocks)" : translator.of("status_off")))); 
			// Wilderness:
			//     Build, Destroy, Switch, ItemUse
			//     Ignored Blocks: see /towny wildsblocks
			screen.addComponentOf("wildernessnewline1", Component.newline());
			screen.addComponentOf("wilderness", colourKey(world.getUnclaimedZoneName() + ": "));
			screen.addComponentOf("wildernessnewline2", Component.newline());
			screen.addComponentOf("perms1", TownyComponents.miniMessage("    " + (world.getUnclaimedZoneBuild() ? "<green>" : "<red>") + translator.of("build") + "<gray>, " + 
													(world.getUnclaimedZoneDestroy() ? "<green>" : "<red>") + translator.of("destroy") + "<gray>, " + 
													(world.getUnclaimedZoneSwitch() ? "<green>" : "<red>") + translator.of("switch") + "<gray>, " + 
													(world.getUnclaimedZoneItemUse() ? "<green>" : "<red>") + translator.of("item_use")));
			screen.addComponentOf("wildernessnewline3", Component.newline());
			screen.addComponentOf("perms2", Component.text("    ").append(colourKey(translator.of("status_world_ignoredblocks"))).append(Component.text(" see /towny wildsblocks", NamedTextColor.GREEN)));

			// Add any metadata which opt to be visible.
			List<Component> fields = getExtraFields(world);
			if (!fields.isEmpty()) {
				TextComponent comp = Component.empty();
				for (Component fieldComp : fields) {
					comp = comp.append(Component.newline()).append(fieldComp);
				}
				screen.addComponentOf("extraFields", comp);
			}
		}
		
		return screen;
	}
	

	/*
	 * Utility methods used in the Status Screens.
	 */
	
	private static Component colourKeyValue(String key, String value) {
		return TownyComponents.miniMessage(String.format(keyValueFormat, Translation.of("status_format_key_value_key"), key, Translation.of("status_format_key_value_value"), value));
	}

	private static Component colourKeyValue(String key, Component value) {
		return TownyComponents.miniMessage(String.format(keyValueFormat, Translation.of("status_format_key_value_key"), key, Translation.of("status_format_key_value_value"), TownyComponents.unMiniMessage(value)));
	}
	
	private static Component colourKey(String key) {
		return TownyComponents.miniMessage(String.format(keyFormat, Translation.of("status_format_key_value_key"), key));
	}
	
	private static Component colourKeyImportant(String key) {
		return TownyComponents.miniMessage(String.format(keyFormat, Translation.of("status_format_key_important"), key));
	}
	
	private static Component colourBracketElement(String key, String value) {
		return TownyComponents.miniMessage(String.format(bracketFormat, Translation.of("status_format_bracket_element"), key, value));
	}
	
	private static Component colourHoverKey(String key) {
		return TownyComponents.miniMessage(String.format(hoverFormat, Translation.of("status_format_hover_bracket_colour"), Translation.of("status_format_hover_key"), key, Translation.of("status_format_hover_bracket_colour")));
	}
	
	private static Component formatPopulationBrackets(int size) {
		return TownyComponents.miniMessage(String.format(" %s[%s]", Translation.of("status_format_list_2"), size));
	}
	
	/**
	 * Gets the registered line for the Resident StatusScreen.
	 * @param resident Resident who's status we are getting.
	 * @param translator Translator used for lang choice.
	 * @return Component with registered date formatted for use in the StatusScreen. 
	 */
	private static Component getResidentRegisteredLine(Resident resident, Translator translator) {
		return (!resident.isNPC() ? colourKeyValue(translator.of("status_registered"), registeredFormat.format(resident.getRegistered())) : colourKeyValue(translator.of("npc_created"), registeredFormat.format(resident.getRegistered())));
	}
	
	/**
	 * Gets the last online line for the Resident StatusScreen.
	 * @param resident Resident who's status we are getting.
	 * @param translator Translator used for lang choice.
	 * @return Component with last online times formatted for use in the StatusScreen. 
	 */
	private static Component getResidentLastOnline(Resident resident, Translator translator) {
		return (sameYear(resident) ? colourKeyValue(translator.of("status_lastonline"), lastOnlineFormat.format(resident.getLastOnline())) : colourKeyValue(translator.of("status_lastonline"), lastOnlineFormatIncludeYear.format(resident.getLastOnline())));
	}
	
	private static Component getResidentJoinedTownDate(Resident resident, Translator translator) {
		return colourKeyValue(translator.of("status_joined_town"), resident.getJoinedTownAt() > 0 ? lastOnlineFormatIncludeYear.format(resident.getJoinedTownAt()) : translator.of("status_unknown"));
	}
	
	private static Component getTownJoinedNationDate(Town town, Translator translator) {
		return colourKeyValue(translator.of("status_joined_nation"), town.getJoinedNationAt() > 0 ? lastOnlineFormatIncludeYear.format(town.getJoinedNationAt()) : translator.of("status_unknown"));
	}
	
	private static boolean sameYear(Resident resident) {
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(resident.getLastOnline());
		int currentYear = cal.get(Calendar.YEAR);
		cal.setTimeInMillis(System.currentTimeMillis());
		int lastOnlineYear = cal.get(Calendar.YEAR);
		return currentYear == lastOnlineYear;
	}
	
	/**
	 * Gets the jail line for the Resident StatusScreen.
	 * @param resident Resident of the StatusScreen.
	 * @param translator Translator used in language switching.
	 * @return String formatted jailLine.
	 */
	private static Component getResidentJailedLine(Resident resident, Translator translator) {
		Component jailLine = colourKeyValue(translator.of("status_jailed"), (resident.isJailed() ? translator.of("status_bad_yes") : translator.of("status_good_no")));
		if (resident.isJailed())
			jailLine.append(colourKey(translator.of("jailed_in_town", resident.getJailTown().getName())));
		if (resident.isJailed() && resident.hasJailTime())
			jailLine.append(colourKey(translator.of("msg_jailed_for_x_hours", resident.getJailHours())));
		return jailLine;
	}
	
	/**
	 * Gets a list of Towns which the given resident owns embassy plots in.
	 * @param resident the given Resident
	 * @return List of Towns in which the resident owns embassies.
	 */
	private static List<Town> getResidentsEmbassyTowns(Resident resident) {
		List<Town> townEmbassies = new ArrayList<>();
		String actualTown = resident.hasTown() ? TownyAPI.getInstance().getResidentTownOrNull(resident).getName() : "";
		
		for(TownBlock tB : resident.getTownBlocks()) {
			Town town = tB.getTownOrNull();
			if (town == null) continue;
			if (!actualTown.equals(town.getName()) && !townEmbassies.contains(town))
				townEmbassies.add(town);
		}
		return townEmbassies;
	}
	
	/**
	 * Returns a List of Strings, in which each string is formatted: RankName [#]: names, of, people, with, the, rank.
	 * @param gov Government (Town or Nation) for which to gather ranks of.
	 * @param locale Locale used in lang selection.
	 * @return List of Strings describe above.
	 */
	private static List<Component> getRanks(Government gov, Locale locale) {
		List<Component> rankList = new ArrayList<>();
		List<Resident> residents = new ArrayList<>(gov.getResidents());
		List<String> ranks;
		if (gov instanceof Nation)
			ranks = TownyPerms.getNationRanks();
		else 
			ranks = TownyPerms.getTownRanks();
		List<Resident> residentWithRank = new ArrayList<>();

		for (String rank : ranks) {
			for (Resident r : residents) {
				if (gov instanceof Nation)
					if ((r.getNationRanks() != null) && (r.getNationRanks().contains(rank)))
						residentWithRank.add(r);
				if (gov instanceof Town)
					if ((r.getTownRanks() != null) && (r.getTownRanks().contains(rank)))
						residentWithRank.add(r);
			}
			if (!residentWithRank.isEmpty())
				rankList.add(getFormattedTownyObjects(StringMgmt.capitalize(rank), new ArrayList<>(residentWithRank)));
			residentWithRank.clear();
		}
		if (gov instanceof Town town && town.getTrustedResidents().size() > 0)
			rankList.add(getFormattedTownyObjects(Translation.of("status_trustedlist", locale), new ArrayList<>(town.getTrustedResidents())));
		
		return rankList;
	}
	
	/**
	 * Shortens and array if longer than i, postfixing with "and more..."
	 * @param array List of components, usually names of TownyObjects.
	 * @param i int representing which index of the array will be the cutoff and changed to "and more..."
	 * @param translator Translator used to select language for message.
	 * @return Shortened Array of components.
	 */
	private static List<Component> shortenOverlengthArray(List<Component> array, int i, Translator translator) {
		if (array.size() > i + 1) {
			array.subList(i + 1, array.size()).clear();
			array.set(i, translator.comp("status_town_reslist_overlength"));
		}

		return array;
	}

	/**
	 * Populates the StatusScreen with the various bank and money components.
	 * @param town Town of which to generate a bankstring.
	 * @param translator Translator used in choosing language.
	 * @param screen StatusScreen to add components to.
	 */
	private static void addTownMoneyComponents(Town town, Translator translator, StatusScreen screen) {
		screen.addComponentOf("moneynewline", Component.newline());
		screen.addComponentOf("bankString", colourKeyValue(translator.of("status_bank"), town.getAccount().getHoldingFormattedBalance()));
		if (town.isBankrupt()) {
			if (town.getAccount().getDebtCap() == 0)
				town.getAccount().setDebtCap(MoneyUtil.getEstimatedValueOfTown(town));
			screen.addComponentOf("bankrupt", TownyComponents.miniMessage(translator.comp("status_bank_bankrupt").append(Component.space()).append(colourKeyValue(translator.of("status_debtcap"), "-" + formatMoney(town.getAccount().getDebtCap())))));
		}
		if (town.hasUpkeep())
			screen.addComponentOf("upkeep", TownyComponents.miniMessage(translator.comp("status_splitter").append(colourKey(translator.of("status_bank_town2"))).append(Component.space()).append(colourKeyImportant(formatMoney(BigDecimal.valueOf(TownySettings.getTownUpkeepCost(town)).setScale(2, RoundingMode.HALF_UP).doubleValue())))));
		if (TownySettings.getUpkeepPenalty() > 0 && town.isOverClaimed())
			screen.addComponentOf("upkeepPenalty", TownyComponents.miniMessage(translator.comp("status_splitter").append(colourKey(translator.of("status_bank_town_penalty_upkeep"))).append(Component.space()).append(colourKeyImportant(formatMoney(TownySettings.getTownPenaltyUpkeepCost(town))))));
		if (town.isNeutral() && TownySettings.getTownNeutralityCost() > 0)
			screen.addComponentOf("neutralityCost", TownyComponents.miniMessage(translator.comp("status_splitter").append(colourKey(translator.of("status_neutrality_cost"))).append(Component.space()).append(colourKeyImportant(formatMoney(TownySettings.getTownNeutralityCost())))));
		screen.addComponentOf("towntax", TownyComponents.miniMessage(translator.comp("status_splitter").append(colourKey(translator.of("status_bank_town3"))).append(Component.space()).append(colourKeyImportant(town.isTaxPercentage() ? town.getTaxes() + "%" : formatMoney(town.getTaxes())))));
	}

	/**
	 * Populates the StatusScreen with the various bank and money components.
	 * @param nation Nation of which to generate a bankstring.
	 * @param translator Translator used in choosing language.
	 * @param screen StatusScreen to add components to.
	 */
	private static void addNationMoneyComponentsToScreen(Nation nation, Translator translator, StatusScreen screen) {
		screen.addComponentOf("moneynewline", Component.newline());
		screen.addComponentOf("bankString", colourKeyValue(translator.of("status_bank"), nation.getAccount().getHoldingFormattedBalance()));
		if (TownySettings.getNationUpkeepCost(nation) > 0)
			screen.addComponentOf("nationupkeep", TownyComponents.miniMessage(translator.comp("status_splitter").append(colourKey(translator.of("status_bank_town2"))).append(Component.space()).append(colourKeyImportant(formatMoney(TownySettings.getNationUpkeepCost(nation))))));
		if (nation.isNeutral() && TownySettings.getNationNeutralityCost() > 0)
			screen.addComponentOf("neutralityCost", TownyComponents.miniMessage(translator.comp("status_splitter").append(colourKey(translator.of("status_neutrality_cost"))).append(Component.space()).append(colourKeyImportant(formatMoney(TownySettings.getNationNeutralityCost())))));
		screen.addComponentOf("nationtax", TownyComponents.miniMessage(translator.comp("status_splitter").append(colourKey(translator.of("status_nation_tax"))).append(Component.space()).append(colourKeyImportant(formatMoney(nation.getTaxes())))));
	}

	private static String formatMoney(double money) {
		return TownyEconomyHandler.getFormattedBalance(money);
	}

	/**
	 * Returns the 2nd line of the Town StatusScreen.
	 * @param town Town for which to get the StatusScreen.
	 * @param world TownyWorld in which the town considers home. 
	 * @param translator Translator used in language selection.
	 * @return Formatted 2nd line of the Town StatusScreen.
	 */
	private static List<String> getTownSubtitle(Town town, TownyWorld world, Translator translator) {
		List<String> sub = new ArrayList<>();
		if (!town.isAdminDisabledPVP() && (town.isPVP() || world.isForcePVP()))
			sub.add(translator.of("status_title_pvp"));
		if (town.isOpen())
			sub.add(translator.of("status_title_open"));
		if (town.isPublic())
			sub.add(translator.of("status_public"));
		if (town.isNeutral())
			sub.add(translator.of("status_town_title_peaceful"));
		if (town.isConquered())
			sub.add(translator.of("msg_conquered"));
		return sub;
	}

	/**
	 * Returns the 2nd line of the Nation StatusScreen.
	 * @param nation Nation for which to get the StatusScreen.
	 * @param translator Translator used in language selection.
	 * @return Formatted 2nd line of the Nation StatusScreen.
	 */
	private static List<String> getNationSubtitle(Nation nation, Translator translator) {
		List<String> sub = new ArrayList<>();
		if (nation.isOpen())
			sub.add(translator.of("status_title_open"));
		if (nation.isPublic())
			sub.add(translator.of("status_public"));
		if (nation.isNeutral())
			sub.add(translator.of("status_town_title_peaceful"));
		return sub;
	}

	/**
	 * Returns the 2nd line of the World StatusScreen.
	 * @param world TownyWorld for which to get the StatusScreen. 
	 * @param translator Translator used in language selection.
	 * @return Formatted 2nd line of the World StatusScreen.
	 */
	private static Component getWorldSubtitle(TownyWorld world, Translator translator) {
		List<Component> subtitle = new ArrayList<>();
		
		if (world.isPVP() || world.isForcePVP())
			subtitle.add(translator.comp("status_title_pvp"));
		if (world.isClaimable())
			subtitle.add(translator.comp("status_world_claimable"));
		else 
			subtitle.add(translator.comp("status_world_noclaims"));
		
		return TownyComponents.joinList(subtitle, Component.space());
	}
	
	/**
	 * Returns a list of MetaData used in the StatusScreens.
	 * @param to TownyObject for which to gather the metadata of.
	 * @return List of visible metadata.
	 */
	public static List<Component> getExtraFields(TownyObject to) {
		if (!to.hasMeta())
			return new ArrayList<>();
		
		List<Component> extraFields = new ArrayList<>();
		for (CustomDataField<?> cdf : to.getMetadata()) {
			if (!cdf.shouldDisplayInStatus())
				continue;
			
			Component newAdd = cdf.getLabelAsComp();
			
			// Apply green color if component is just plain text
			if (newAdd.style().color() == null) {
				NamedTextColor kvColor = Colors.toNamedTextColor(Translation.of("status_format_key_value_key"));
				newAdd = newAdd.color(kvColor);
			}
			
			newAdd = newAdd.append(Component.text(": ").mergeStyle(newAdd))
				.append(cdf.formatValueAsComp());
			
			extraFields.add(newAdd);
		}

		return extraFields;
	}

	/**
	 * Determine whether the named player is vanished from the  
	 * @param name the name of the player who is having /res NAME used upon them.
	 * @param sender Sender who ran the /res NAME command.
	 * @return true if the name is online and can be seen by the player. 
	 */
	private static boolean playerIsOnlineAndVisible(String name, CommandSender sender) {
		if (sender instanceof Player player)
			return BukkitTools.isOnline(name) && player.canSee(BukkitTools.getPlayer(name));
		else if (sender instanceof ConsoleCommandSender)
			return BukkitTools.isOnline(name);
		else
			return false;
	}
	
	/*
	 * Methods used throughout Towny. 
	 */
	
	/**
	 * Used in /n online and /t online.
	 * @param prefix String prefix to use.
	 * @param residentList ResidentList representing the town or nation.
	 * @param player Player which is doing the looking, used for Vanishing.
	 * @return String of formatted residents listed and prefixed.
	 */
	public static Component getFormattedOnlineResidents(String prefix, ResidentList residentList, Player player) {
		return getFormattedTownyObjects(prefix, new ArrayList<>(ResidentUtil.getOnlineResidentsViewable(player, residentList)));
	}

	/**
	 * Used to prefix, count and list the given object.
	 * @param prefix String applied to beginning of the list.
	 * @param objectList List of TownyObjects to list.
	 * @return Formatted, prefixed list of TownyObjects.
	 */
	public static Component getFormattedTownyObjects(String prefix, List<TownyObject> objectList) {
		return TownyComponents.miniMessage(String.format(listPrefixFormat, prefix, objectList.size(), Translation.of("status_format_list_1"), Translation.of("status_format_list_2"), Translation.of("status_format_list_3")) + TownyComponents.unMiniMessage(TownyComponents.joinList(getFormattedTownyNames(objectList), Component.text(", ")))); 
	}
	
	/**
	 * Used to prefix, count and list the given strings.
	 * @param prefix String applied to beginning of the list.
	 * @param list List of Strings to list.
	 * @return Formatted, prefixed list of Strings.
	 */
	public static Component getFormattedComponent(String prefix, List<Component> list) {
		return TownyComponents.miniMessage(String.format(listPrefixFormat, prefix, list.size(), Translation.of("status_format_list_1"), Translation.of("status_format_list_2"), Translation.of("status_format_list_3"))).append(TownyComponents.joinList(list, Component.text(", "))); 
	}
	
	/**
	 * Used to prefix, count and list the given strings.
	 * @param prefix String applied to beginning of the list.
	 * @param list List of Strings to list.
	 * @return Formatted, prefixed list of Strings.
	 */
	public static Component getFormattedComponent(String prefix, List<Component> list, int size) {
		return TownyComponents.miniMessage(String.format(listPrefixFormat, prefix, size, Translation.of("status_format_list_1"), Translation.of("status_format_list_2"), Translation.of("status_format_list_3"))).append(TownyComponents.joinList(list, Component.text(", "))); 
	}

	/**
	 * Returns a list of names, with ColourCodes translated.
	 * If the list is under 20 long, it will use the formatted names.
	 * @param objects List of TownyObjects of which to make a list of names.
	 * @return List of Names coloured, and potentially formatted.
	 */
	public static List<Component> getFormattedTownyNames(List<TownyObject> objects) {
		List<Component> names = new ArrayList<>();
		for (TownyObject obj : objects) {
			names.add(TownyComponents.miniMessage(objects.size() < 20 ? obj.getFormattedName() : obj.getName()));
		}
		
		return names;
	}

	/**
	 * Returns an Array of names, using their Formatted (long) names, with ColourCodes translated.
	 * @param objects Array of TownyObjects of which to make a list of names.
	 * @return Array of Names, formatted and coloured.
	 */
	public static List<Component> getFormattedNames(List<? extends Nameable> objects) {
		List<Component> names = new ArrayList<>();
		for (Nameable obj : objects) {
			names.add(TownyComponents.miniMessage(obj.getFormattedName()));
		}
		
		return names;
	}
	
	/**
	 * Returns the tax info this resident will have to pay at the next new day.
	 * 
	 * @param resident the resident to check
	 * @param locale Locale to use while translating   
	 * @return tax status message
	 */
	public static List<Component> getTaxStatus(Resident resident, Locale locale) {

		List<Component> out = new ArrayList<>();
		final Translator translator = Translator.locale(locale);
		
		Town town;
		boolean taxExempt = TownyPerms.getResidentPerms(resident).containsKey("towny.tax_exempt");
		double plotTax = 0.0;
		double townTax = 0.0;

		out.add(ChatTools.formatTitle(translator.of("status_resident_tax_title", resident.getName())));

		out.add(colourKey(translator.of("owner_of_x_plots", resident.getTownBlocks().size())));

		/*
		 * Calculate what the player will be paying their town for tax.
		 */
		if (resident.hasTown()) {
			town = TownyAPI.getInstance().getResidentTownOrNull(resident);

			if (taxExempt) {
				out.add(colourKey(translator.of("status_res_taxexempt")));
			} else {
				if (town.isTaxPercentage())
					townTax = Math.min(resident.getAccount().getHoldingBalance() * town.getTaxes() / 100, town.getMaxPercentTaxAmount());
				else
					townTax = town.getTaxes();
				out.add(colourKeyValue(translator.of("status_res_tax"), formatMoney(townTax)));
			}
		}

		/*
		 * Calculate what the player will be paying for their plots' tax.
		 */
		if (resident.getTownBlocks().size() > 0) {

			for (TownBlock townBlock : new ArrayList<>(resident.getTownBlocks())) {
				town = townBlock.getTownOrNull();
				if (town != null) {
					if (taxExempt && town.hasResident(resident)) // Resident will not pay any tax for plots owned by their towns.
						continue;
					plotTax += townBlock.getType().getTax(town);
				}
			}

			out.add(colourKeyValue(translator.of("status_res_plottax"), formatMoney(plotTax)));
		}
		out.add(colourKeyValue(translator.of("status_res_totaltax"), formatMoney(townTax + plotTax)));

		return out;
	}

	/**
	 * Returns a Chat Formatted List of all town residents who hold a rank.
	 * 
	 * @param town the town for which to check against.
	 * @return a list containing formatted rank data.
	 */
	public static List<Component> getRanksForTown(Town town, Locale locale) {
		final Translator translator = Translator.locale(locale);
		List<Component> ranklist = new ArrayList<>();
		ranklist.add(ChatTools.formatTitle(translator.of("rank_list_title", town.getFormattedName())));
		ranklist.add(colourKeyValue(translator.of("rank_list_mayor"), town.getMayor().getFormattedName()));

		ranklist.addAll(getRanks(town, locale));
		return ranklist;
	}

	/**
	 * Returns a Chat Formatted List of all nation residents who hold a rank.
	 * 
	 * @param nation the nation for which to check against.
	 * @return a list containing formatted rank data.
	 */
	public static List<Component> getRanksForNation(Nation nation, Locale locale) {
		final Translator translator = Translator.locale(locale);
		List<Component> rankList = new ArrayList<>();
		rankList.add(ChatTools.formatTitle(translator.of("rank_list_title", nation.getFormattedName())));
		rankList.add(colourKeyValue(translator.of("status_nation_king"), nation.getKing().getFormattedName()));

		rankList.addAll(getRanks(nation, locale));
		return rankList;
	}
	
	/**
	 * @return the Time.
	 */
	public static String getTime() {
		SimpleDateFormat sdf = new SimpleDateFormat("hh:mm aa");
		return sdf.format(System.currentTimeMillis());
	}
	
	private static String formatWebUrl(SpawnLocation spawnLocation) {
		String webUrl = "";
		if (TownySettings.isUsingWebMapStatusScreens() && spawnLocation.hasSpawn() && !TownySettings.getWebMapUrl().isEmpty())
			webUrl = TownySettings.getWebMapUrl()
				.replaceAll("\\{world}", spawnLocation.getSpawnOrNull().getWorld().getName())
				.replaceAll("\\{x}", "" + spawnLocation.getSpawnOrNull().getBlockX())
				.replaceAll("\\{z}", "" + spawnLocation.getSpawnOrNull().getBlockZ());

		return webUrl;
	}

}
