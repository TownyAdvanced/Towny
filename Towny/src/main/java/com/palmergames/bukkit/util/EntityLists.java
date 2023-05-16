package com.palmergames.bukkit.util;

import com.palmergames.bukkit.towny.object.AbstractRegistryList;
import org.bukkit.Registry;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;

public class EntityLists extends AbstractRegistryList<EntityType> {

	public EntityLists(Collection<EntityType> collection) {
		super(Registry.ENTITY_TYPE, collection);
	}
	
	public boolean contains(@NotNull Entity entity) {
		return this.contains(entity.getType());
	}
	
	public static final EntityLists VEHICLES = newBuilder().startsWith("minecart").endsWith("boat").endsWith("raft").build();
	
	public static final EntityLists MOUNTABLE = newBuilder().add("horse", "strider", "pig", "donkey", "mule", "trader_llama", "camel").build();
	
	public static final EntityLists DYEABLE = newBuilder().add("sheep", "wolf").build();
	
	public static final EntityLists SWITCH_PROTECTED = newBuilder().add("minecart_chest", "minecart_furnace", "minecart_hopper", "chest_boat").build();
	
	public static final EntityLists RIGHT_CLICK_PROTECTED = newBuilder().add("tropical_fish", "salmon", "cod", "item_frame", "glow_item_frame", "painting", "leash_hitch", "leash_knot", "command_block_minecart", "tnt_minecart", "spawner_minecart", "tadpole", "axolotl").build();
	
	public static final EntityLists ITEM_FRAMES = newBuilder().add("item_frame", "glow_item_frame").build();
	
	public static final EntityLists HANGING = newBuilder().add("item_frame", "glow_item_frame", "painting").build();
	
	public static final EntityLists BOATS = newBuilder().endsWith("boat").endsWith("raft").build();
	
	public static Builder<EntityType, EntityLists> newBuilder() {
		return new Builder<>(Registry.ENTITY_TYPE, EntityType.class, EntityLists::new);
	}
}
