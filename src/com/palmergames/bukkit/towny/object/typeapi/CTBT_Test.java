package com.palmergames.bukkit.towny.object.typeapi;

import com.palmergames.bukkit.towny.event.actions.*;
import com.palmergames.bukkit.towny.event.damage.TownyDamageEvent;
import com.palmergames.bukkit.towny.object.TownBlock;

/**
 * Test class
 * To be deleted
 */
public class CTBT_Test extends CustomTownBlockType {
	public CTBT_Test() {
		super("towny_hospital", "Hospital");
		setHandler(new TownBlockTypeHandler() {

			@Override
			public boolean onTownyBuild(TownBlock townBlock) {
				return false;
			}

			@Override
			public boolean onTownyDestroy(TownBlock townBlock) {
				return false;
			}

			@Override
			public boolean onTownyItemUse(TownBlock townBlock) {
				return false;
			}

			@Override
			public boolean onTownySwitch(TownBlock townBlock) {
				return false;
			}

			@Override
			public boolean onTownyBurn(TownBlock townBlock) {
				return false;
			}

			@Override
			public boolean onTownyExplosion(TownBlock townBlock) {
				return false;
			}

			@Override
			public boolean onTownyDamage(TownBlock townBlock) {
				return false;
			}
		});
	}
}
