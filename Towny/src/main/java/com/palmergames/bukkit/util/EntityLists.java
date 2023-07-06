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
	
	public static final EntityLists DYEABLE = newBuilder().add("sheep", "wolf", "cat").build();
	
	public static final EntityLists SWITCH_PROTECTED = newBuilder().add("chest_minecart", "furnace_minecart", "hopper_minecart", "chest_boat").build();
	
	public static final EntityLists RIGHT_CLICK_PROTECTED = newBuilder().add("tropical_fish", "salmon", "cod", "item_frame", "glow_item_frame", "painting", "leash_knot", "command_block_minecart", "tnt_minecart", "spawner_minecart", "tadpole", "axolotl").build();
	
	public static final EntityLists DESTROY_PROTECTED = newBuilder().add("item_frame", "glow_item_frame", "painting", "armor_stand", "end_crystal", "minecart", "chest_minecart", "command_block_minecart", "hopper_minecart").build();
	
	public static final EntityLists ITEM_FRAMES = newBuilder().add("item_frame", "glow_item_frame").build();
	
	public static final EntityLists HANGING = newBuilder().add("item_frame", "glow_item_frame", "painting").build();
	
	public static final EntityLists BOATS = newBuilder().endsWith("boat").endsWith("raft").build();
	
	public static final EntityLists EXPLOSIVE = newBuilder().add("creeper").endsWith("fireball").add("firework_rocket", "tnt_minecart", "tnt", "wither", "wither_skull", "end_crystal").build();
	
	public static final EntityLists PVE_EXPLOSIVE = newBuilder().add("creeper").endsWith("fireball").add("wither", "wither_skull", "end_crystal").build();
	
	public static final EntityLists PVP_EXPLOSIVE = newBuilder().add("firework_rocket", "tnt_minecart", "tnt", "end_crystal").build();
	
	public static Builder<EntityType, EntityLists> newBuilder() {
		return new Builder<>(Registry.ENTITY_TYPE, EntityType.class, EntityLists::new);
	}
}
