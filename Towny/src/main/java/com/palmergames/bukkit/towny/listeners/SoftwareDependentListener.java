package com.palmergames.bukkit.towny.listeners;

import com.palmergames.bukkit.towny.Towny;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;

import java.util.function.Consumer;
import java.util.function.Supplier;

public interface SoftwareDependentListener extends Listener {

    void register();

    @SuppressWarnings("unchecked")
    default <T extends Event> void registerEvent(String className, Supplier<Consumer<T>> executor, EventPriority eventPriority, boolean ignoreCancelled) {
        try {
            Class<T> eventClass = (Class<T>) Class.forName(className).asSubclass(Event.class);
            registerEvent(eventClass, executor.get(), eventPriority, ignoreCancelled);
        } catch (ClassNotFoundException ignored) {}
    }

    @SuppressWarnings("unchecked")
    default <T extends Event> void registerEvent(Class<T> eventClass, Consumer<T> consumer, EventPriority eventPriority, boolean ignoreCancelled) {
        Bukkit.getPluginManager().registerEvent(eventClass, this, eventPriority, (listener, event) -> consumer.accept((T) event), Towny.getPlugin(), ignoreCancelled);
    }
}
