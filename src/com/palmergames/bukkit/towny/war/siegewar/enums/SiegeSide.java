package com.palmergames.bukkit.towny.war.siegewar.enums;

import com.palmergames.bukkit.towny.object.Translation;

public enum SiegeSide {
	ATTACKERS("msg_attackers"), DEFENDERS("msg_defenders"), NOBODY("msg_nobody");

	SiegeSide(String langStringId) {
		this.langStringId = langStringId;
	}

	private String langStringId;

	public String getFormattedName() {
		return Translation.of(langStringId);
	}
}
