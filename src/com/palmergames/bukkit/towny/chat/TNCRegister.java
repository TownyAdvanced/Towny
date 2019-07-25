package com.palmergames.bukkit.towny.chat;

import net.tnemc.tnc.core.common.api.TNCAPI;

/**
 * @author creatorfromhell
 */
public class TNCRegister {

	public static void initialize() {
		TNCAPI.addHandler(new TheNewChatHandler());
	}
}