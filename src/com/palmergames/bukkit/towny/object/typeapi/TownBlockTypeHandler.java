package com.palmergames.bukkit.towny.object.typeapi;

import com.palmergames.bukkit.towny.event.actions.TownyBuildEvent;
import com.palmergames.bukkit.towny.event.actions.TownyDestroyEvent;
import com.palmergames.bukkit.towny.event.actions.TownyItemuseEvent;
import com.palmergames.bukkit.towny.event.actions.TownySwitchEvent;

public abstract class TownBlockTypeHandler {
	public abstract void onTownyBuild(TownyBuildEvent event);
	public abstract void onTownyDestroy(TownyDestroyEvent event);
	public abstract void onTownyItemUse(TownyItemuseEvent event);
	public abstract void onTownySwitch(TownySwitchEvent event);
}
