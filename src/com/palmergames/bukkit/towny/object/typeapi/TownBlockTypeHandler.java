package com.palmergames.bukkit.towny.object.typeapi;

import com.palmergames.bukkit.towny.event.actions.*;
import com.palmergames.bukkit.towny.event.damage.TownyDamageEvent;

public abstract class TownBlockTypeHandler {
	public abstract boolean onTownyBuild(TownyBuildEvent event);
	public abstract boolean onTownyDestroy(TownyDestroyEvent event);
	public abstract boolean onTownyItemUse(TownyItemuseEvent event);
	public abstract boolean onTownySwitch(TownySwitchEvent event);
	public abstract boolean onTownyBurn(TownyBurnEvent event);
	public abstract boolean onTownyExplosion(TownyExplodingBlocksEvent event);
	public abstract boolean onTownyDamage(TownyDamageEvent event);
	
}
