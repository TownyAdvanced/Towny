package com.palmergames.bukkit.towny.chat;

import com.palmergames.bukkit.towny.chat.checks.KingCheck;
import com.palmergames.bukkit.towny.chat.checks.MayorCheck;
import com.palmergames.bukkit.towny.chat.types.AllyType;
import com.palmergames.bukkit.towny.chat.types.NationType;
import com.palmergames.bukkit.towny.chat.types.TownType;
import com.palmergames.bukkit.towny.chat.variables.NationVariable;
import com.palmergames.bukkit.towny.chat.variables.TitleVariable;
import com.palmergames.bukkit.towny.chat.variables.TownVariable;
import net.tnemc.tnc.core.common.chat.ChatHandler;

/**
 * @author creatorfromhell
 */
public class TheNewChatHandler extends ChatHandler {

	public TheNewChatHandler() {

		addType(new AllyType());
		addType(new NationType());
		addType(new TownType());

		addVariable(new NationVariable());
		addVariable(new TownVariable());
		addVariable(new TitleVariable());

		addCheck(new KingCheck());
		addCheck(new MayorCheck());
	}
	@Override
	public String getName() {
		return "towny";
	}
}