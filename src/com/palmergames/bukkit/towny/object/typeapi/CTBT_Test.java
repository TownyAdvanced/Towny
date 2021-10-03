package com.palmergames.bukkit.towny.object.typeapi;

import com.palmergames.bukkit.towny.event.actions.*;
import com.palmergames.bukkit.towny.event.damage.TownyDamageEvent;

/**
 * Test class
 * To be deleted
 */
public class CTBT_Test extends CustomTownBlockType {
	public CTBT_Test() {
		super("test_hospital", "Hospital");
		setHandler(new TownBlockTypeHandler() {

			@Override
			public boolean onTownyBuild(TownyBuildEvent event) {
				return false;
			}

			@Override
			public boolean onTownyDestroy(TownyDestroyEvent event) {
				return false;
			}

			@Override
			public boolean onTownyItemUse(TownyItemuseEvent event) {
				return false;
			}

			@Override
			public boolean onTownySwitch(TownySwitchEvent event) {
				return false;
			}

			@Override
			public boolean onTownyBurn(TownyBurnEvent event) {
				return false;
			}

			@Override
			public boolean onTownyExplosion(TownyExplodingBlocksEvent event) {
				return false;
			}

			@Override
			public boolean onTownyDamage(TownyDamageEvent event) {
				return false;
			}
		});
	}
}
