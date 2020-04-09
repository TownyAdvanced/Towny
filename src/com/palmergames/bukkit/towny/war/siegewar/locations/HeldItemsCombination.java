package com.palmergames.bukkit.towny.war.siegewar.locations;

import org.bukkit.Material;

public final class HeldItemsCombination {
	private boolean ignoreOffHand;  //true if off hand is irrelevant
	private boolean ignoreMainHand;  //true if main hand is irrelevant
	private final Material offhandItemType;  //AIR if off hand must be empty
	private final Material mainHandItemType;  //AIR if main hand must be empty

	public HeldItemsCombination(Material offhandItemType, Material mainHandItemType,
								boolean ignoreOffHand, boolean ignoreMainHand) {
		this.ignoreOffHand = ignoreOffHand;
		this.ignoreMainHand = ignoreMainHand;
		this.offhandItemType = offhandItemType;
		this.mainHandItemType = mainHandItemType;
	}

	public Material getMainHandItemType() {
		return mainHandItemType;
	}

	public Material getOffHandItemType() {
		return offhandItemType;
	}

	public boolean isIgnoreOffHand() {
		return ignoreOffHand;
	}

	public boolean isIgnoreMainHand() {
		return ignoreMainHand;
	}
}
