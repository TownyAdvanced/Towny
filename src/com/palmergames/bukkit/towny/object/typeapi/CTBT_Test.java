package com.palmergames.bukkit.towny.object.typeapi;

import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;

/**
 * Test class
 */
public class CTBT_Test extends CustomTownBlockType {
	public CTBT_Test() {
		super("townyAdvanced", "Hospital");
		setHandler(new TownBlockTypeHandler() {

			@Override
			public void onTownyBuild(TownyBuildEvent event) {
			
			}

			@Override
			public void onTownyDestroy(TownyDestroyEvent event) {

			}

			@Override
			public void onTownyItemUse(TownyItemuseEvent event) {

			}

			@Override
			public void onTownySwitch(TownySwitchEvent event) {

			}
		});
	}
}
