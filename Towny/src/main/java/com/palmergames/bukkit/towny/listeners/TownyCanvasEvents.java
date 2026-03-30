package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyAPI;
import com.palmergames.bukkit.towny.TownyMessaging;
import com.palmergames.bukkit.towny.TownySettings;
import com.palmergames.bukkit.towny.TownyUniverse;
import com.palmergames.bukkit.towny.event.executors.TownyActionEventExecutor;
import com.palmergames.bukkit.towny.hooks.PluginIntegrations;
import com.palmergames.bukkit.towny.object.Resident;
import com.palmergames.bukkit.towny.object.Town;
import com.palmergames.bukkit.towny.object.TownBlock;
import com.palmergames.bukkit.towny.object.Translatable;
import com.palmergames.bukkit.towny.permissions.PermissionNodes;
import com.palmergames.util.JavaUtil;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.EventPriority;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.lang.invoke.MethodHandle;
import java.util.function.Consumer;
import java.util.logging.Level;

public class TownyCanvasEvents implements SoftwareDependentListener {

    private static final String ASYNC_TELEPORT_EVENT = "io.canvasmc.canvas.event.EntityTeleportAsyncEvent";

    private static final MethodHandle ASYNC_TELEPORT_EVENT_FROM = JavaUtil.getMethodHandle(ASYNC_TELEPORT_EVENT, "getFrom");
    private static final MethodHandle ASYNC_TELEPORT_EVENT_TO = JavaUtil.getMethodHandle(ASYNC_TELEPORT_EVENT, "getTo");
    private static final MethodHandle ASYNC_TELEPORT_EVENT_CAUSE = JavaUtil.getMethodHandle(ASYNC_TELEPORT_EVENT, "getCause");

    private final Towny plugin;
    private final TownyPlayerListener townyPlayerListener;

    public TownyCanvasEvents(Towny instance, TownyPlayerListener townyPlayerListener) {
        this.plugin = instance;
        this.townyPlayerListener = townyPlayerListener;
    }

    @Override
    public void register() {
        registerEvent(ASYNC_TELEPORT_EVENT, this::asyncTeleportEventListener, EventPriority.NORMAL, true);
    }

    private Consumer<EntityEvent> asyncTeleportEventListener() {
        return event -> {
            if (ASYNC_TELEPORT_EVENT_FROM == null || ASYNC_TELEPORT_EVENT_TO == null || ASYNC_TELEPORT_EVENT_CAUSE == null || !(event.getEntity() instanceof Player player))
                return;

            final Location from;
            final Location to;
            final PlayerTeleportEvent.TeleportCause cause;
            try {
                from = (Location) ASYNC_TELEPORT_EVENT_FROM.invoke(event);
                to = (Location) ASYNC_TELEPORT_EVENT_TO.invoke(event);
                cause = (PlayerTeleportEvent.TeleportCause) ASYNC_TELEPORT_EVENT_CAUSE.invoke(event);
            } catch (final Throwable e) {
                plugin.getLogger().log(Level.WARNING, "An exception occurred while invoking " + ASYNC_TELEPORT_EVENT + "#from/#to/#getCause reflectively", e);
                return;
            }

            plugin.getScheduler().run(player, () -> {
                // Let's ignore Citizens NPCs. This must come before the safemode check, as Citizens stores their NPCs
                // at the world spawn until a player loads a chunk, to which the NPC is then teleported. Towny would
                // prevent them teleporting, leaving them at spawn even after Safe Mode is cleaned up.
                if (PluginIntegrations.getInstance().isNPC(player))
                    return;

                if (plugin.isError()) {
                    ((Cancellable) event).setCancelled(true);
                    return;
                }

                Resident resident = TownyUniverse.getInstance().getResident(player.getUniqueId());
                if (resident == null)
                    return;

                boolean isAdmin = !Towny.getPlugin().hasPlayerMode(player, "adminbypass") && (resident.isAdmin() || resident.hasPermissionNode(PermissionNodes.TOWNY_ADMIN_OUTLAW_TELEPORT_BYPASS.getNode()));
                if (isAdmin) {
                    // Admins don't get restricted further but they do need to fire the PlayerChangePlotEvent.
                    townyPlayerListener.handleCellChange(player, from, to, (Cancellable) event);
                    return;
                }

                // Cancel teleport if Jailed by Towny.
                if (resident.isJailed()) {
                    if (cause == PlayerTeleportEvent.TeleportCause.COMMAND) {
                        TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_jailed_players_no_teleport"));
                        ((Cancellable) event).setCancelled(true);
                        return;
                    }
                    if (!TownySettings.JailAllowsTeleportItems() && (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL || cause == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT)) {
                        TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_jailed_players_no_teleport"));
                        ((Cancellable) event).setCancelled(true);
                        return;
                    }
                }

                // Cancel teleport if resident is outlawed in Town.
                if (!TownySettings.canOutlawsTeleportOutOfTowns()) {
                    TownBlock tb = TownyAPI.getInstance().getTownBlock(from);
                    if (tb != null && tb.hasTown()) {
                        Town town = tb.getTownOrNull();
                        if (town != null && town.hasOutlaw(resident)) {
                            if (cause == PlayerTeleportEvent.TeleportCause.COMMAND) {
                                TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_outlawed_players_no_teleport"));
                                ((Cancellable) event).setCancelled(true);
                                return;
                            }
                            if (!TownySettings.canOutlawsUseTeleportItems() && (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL || cause == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT)) {
                                TownyMessaging.sendErrorMsg(player, Translatable.of("msg_err_outlawed_players_no_teleport"));
                                ((Cancellable) event).setCancelled(true);
                                return;
                            }
                        }
                    }
                }

                // Test to see if CHORUS_FRUIT is in the item_use list.
                if (cause == PlayerTeleportEvent.TeleportCause.CONSUMABLE_EFFECT && TownySettings.isItemUseMaterial(Material.CHORUS_FRUIT, to)) {
                    //Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
                    if (!TownyActionEventExecutor.canItemuse(player, to, Material.CHORUS_FRUIT)) {
                        ((Cancellable) event).setCancelled(true);
                        return;
                    }
                }

                // Test to see if Ender pearls is in the item_use list.
                if (cause == PlayerTeleportEvent.TeleportCause.ENDER_PEARL && TownySettings.isItemUseMaterial(Material.ENDER_PEARL, to)) {
                    //Make decision on whether this is allowed using the PlayerCache and then a cancellable event.
                    if (!TownyActionEventExecutor.canItemuse(player, to, Material.ENDER_PEARL)) {
                        ((Cancellable) event).setCancelled(true);
                        return;
                    }
                }

                // Remove spawn protection if the player is teleporting since spawning.
                if (resident.hasRespawnProtection())
                    resident.removeRespawnProtection();

                // Send the event to the onPlayerMove so Towny can fire the PlayerChangePlotEvent.
                townyPlayerListener.handleCellChange(player, from, to, (Cancellable) event);
            });
        };
    }
}
