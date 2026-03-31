package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import com.palmergames.util.JavaUtil;
import org.bukkit.Location;
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

            townyPlayerListener.handleTeleportCellChange(player, cause, from, to, (Cancellable) event);
        };
    }
}
