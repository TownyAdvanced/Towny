package com.palmergames.bukkit.towny.listeners;

import org.bukkit.Material;
import org.bukkit.entity.Boat;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Minecart;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.vehicle.VehicleBlockCollisionEvent;
import org.bukkit.event.vehicle.VehicleDamageEvent;
import org.bukkit.event.vehicle.VehicleEnterEvent;
import org.bukkit.inventory.ItemStack;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.utils.EntityTypeUtil;
import com.palmergames.bukkit.util.ItemLists;

/**
 * Handle events for all Vehicle related events
 * 
 * @author ElgarL
 * 
 */
public class TownyVehicleListener implements Listener {
	
	private final Towny plugin;

	public TownyVehicleListener(Towny instance) {

		plugin = instance;
	}
	
	/**
	 * Cancelling the damage will also prevent the boats/minecarts from being moved around.
	 * 
	 * @param event VehicleDamageEvent.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onVehicleDamage(VehicleDamageEvent event) {
		
		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getVehicle().getWorld()) || (event.getAttacker() == null && event.getVehicle().getFireTicks() > 0))
			return;

		if (event.getAttacker() == null) {  // Probably a respawn anchor or a TNT minecart or TNT lit by redstone.
			event.setCancelled(!TownyActionEventExecutor.canExplosionDamageEntities(event.getVehicle().getLocation(), event.getVehicle(), DamageCause.ENTITY_EXPLOSION));
			return;
		}
		
		/*
		 * Note: Player-lit-TNT and Fireballs are considered Players by the API in this instance.
		 */
		if (event.getAttacker() instanceof Player player) {
			
			/*
			 * Substitute a Material for the Entity so we can run a destroy test against it.
			 * Any entity not in the switch statement will leave vehicle null and no test will occur.
			 */
			Material vehicle = switch (event.getVehicle().getType()) {
				case MINECART:
				case MINECART_FURNACE:
				case MINECART_HOPPER:
				case MINECART_CHEST:
				case MINECART_MOB_SPAWNER:
				case MINECART_COMMAND:
				case MINECART_TNT:
				case BOAT:
				case CHEST_BOAT:
					yield EntityTypeUtil.parseEntityToMaterial(event.getVehicle().getType());
				default:
					yield null;
			};

			if (vehicle != null) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getVehicle().getLocation(), vehicle));
			}
		} else {
			if (EntityTypeUtil.isExplosive(event.getAttacker().getType()) && !TownyActionEventExecutor.canExplosionDamageEntities(event.getVehicle().getLocation(), event.getVehicle(), DamageCause.ENTITY_EXPLOSION))
				event.setCancelled(true);
		}
	}
	
	/*
	 * This is necessary because of the lacking API surrounding the Vehicle events.
	 * 
	 * Cactus return a null getAttacker() on a VehicleDamageEvent, a null getLastDamageCause() 
	 * on a VehicleDestroyEvent, and because the VehicleDamageEvent fires before the EntityDamageByBlockEvent
	 * it appears to be quite impossible to protect for the many null getAttacker()'s in the VehicleDamageEvent,
	 * while still allowing players to have minecart stations that destroy unneeded carts.
	 * 
	 * To clarify: We have to protect against a whole bunch of Null damage-causing things, of which Cactus ends
	 * up also getting caught up amongst.
	 */
	@EventHandler
	public void onVehicleCollide(VehicleBlockCollisionEvent event) {
		if (plugin.isError() || !TownyAPI.getInstance().isTownyWorld(event.getVehicle().getWorld()))
			return;
		
		if (event.getVehicle() instanceof Minecart && ItemLists.MINECART_KILLERS.contains(event.getBlock().getType())) {
			event.getVehicle().remove();
			event.getBlock().getWorld().dropItemNaturally(event.getVehicle().getLocation(), new ItemStack(EntityTypeUtil.parseEntityToMaterial(event.getVehicle().getType())));
		}
	}

	/**
	 * Handles switch use (entering into vehicles.)
	 * 
	 * @param event VehicleEnterEvent.
	 */
	@EventHandler(priority = EventPriority.HIGH, ignoreCancelled = true)
	public void onVehicleEnter(VehicleEnterEvent event) {

		if (plugin.isError()) {
			event.setCancelled(true);
			return;
		}

		if (!TownyAPI.getInstance().isTownyWorld(event.getVehicle().getWorld()))
			return;

		if (event.getEntered() instanceof Player player) {

			/*
			 * Substitute a Material for the Entity so we can run a switch test against it.
			 * Any entity not in the switch statement will leave vehicle null and no test will occur.
			 */
			Material vehicle = switch (event.getVehicle().getType()) {
				case MINECART:
				case BOAT:
				case CHEST_BOAT:
					yield EntityTypeUtil.parseEntityToMaterial(event.getVehicle().getType());
				case HORSE:
				case STRIDER:
				case PIG:
				case DONKEY:
				case MULE:
					yield Material.SADDLE;
				default:
					yield null;
			};

			if (vehicle != null) {
				//Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
				if (TownySettings.isSwitchMaterial(vehicle, event.getVehicle().getLocation()))
					event.setCancelled(!TownyActionEventExecutor.canSwitch(player, event.getVehicle().getLocation(), vehicle));
			}
		} 

		/*
		 * Dealing with a Boat being entered by a protected mob type.
		 */
		if (event.getVehicle() instanceof Boat boat && EntityTypeUtil.isInstanceOfAny(TownySettings.getProtectedEntityTypes(), event.getEntered())) {

			// Dealing with an empty boat.
			if (boat.isEmpty()) {
				// No driver, don't allow someone pushing a boat into a entity to steal the entity.
				event.setCancelled(true);
				System.out.println("Cancelled empty boat entrance.");
				return;
			}
			// Dealing with a boat with one passenger.
			Entity rider = boat.getPassengers().stream().findFirst().get();
			System.out.println("rider = " + rider.getName());
			// If it is driven by a player check for destroy permissions.
			if (rider instanceof Player player) {
				System.out.println("Cancelled player boat entrance.");
				event.setCancelled(!TownyActionEventExecutor.canDestroy(player, event.getVehicle().getLocation(), Material.GRASS_BLOCK));
				System.out.println("Cancelled player boat entrance.");
				return;
			}
			
			System.out.println("Cancelled entity boat entrance.");
			// It is another non-player in the boat, probably being pushed around by a player.
			event.setCancelled(true);
			return;
		}
	}
}
