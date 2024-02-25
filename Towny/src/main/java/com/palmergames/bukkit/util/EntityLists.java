package com.palmergames.bukkit.util;

import com.google.common.collect.ImmutableSet;
import com.palmergames.bukkit.towny.object.AbstractRegistryList;
import org.bukkit.Registry;
import org.bukkit.entity.Animals;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class EntityLists extends AbstractRegistryList<EntityType> {

	public EntityLists(Collection<EntityType> collection) {
		super(Registry.ENTITY_TYPE, collection);
	}
	
	public boolean contains(@NotNull Entity entity) {
		return this.contains(entity.getType());
	}
	
	public static final EntityLists VEHICLES = newBuilder().startsWith("minecart").endsWith("minecart").endsWith("boat").endsWith("raft").build();
	
	public static final EntityLists MOUNTABLE = newBuilder().add("horse", "strider", "pig", "donkey", "mule", "trader_llama", "camel").build();
	
	public static final EntityLists MILKABLE = newBuilder().add("cow", "mooshroom", "goat").build();
	
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
	
	public static final EntityLists ANIMALS = newBuilder().filter(type -> type.getEntityClass() != null && Animals.class.isAssignableFrom(type.getEntityClass())).build();
	
	private static final Map<String, EntityLists> GROUPS = new HashMap<>();

	@NotNull
	@Unmodifiable
	public static Set<EntityType> getGrouping(@NotNull String groupName) {
		final EntityLists grouping = GROUPS.get(groupName.toLowerCase(Locale.ROOT));

		return grouping != null ? ImmutableSet.copyOf(grouping.tagged) : ImmutableSet.of();
	}
	
	@ApiStatus.Internal
	public static void clearGroups() {
		GROUPS.clear();
	}

	public static boolean hasGroup(@NotNull String groupName) {
		return GROUPS.containsKey(groupName.toLowerCase(Locale.ROOT));
	}

	public static void addGroup(@NotNull String groupName, @NotNull EntityLists group) {
		GROUPS.put(groupName.toLowerCase(Locale.ROOT), group);
	}
	
	public static Builder<EntityType, EntityLists> newBuilder() {
		return new Builder<>(Registry.ENTITY_TYPE, EntityType.class, EntityLists::new).filter(type -> type != EntityType.UNKNOWN);
	}
}
