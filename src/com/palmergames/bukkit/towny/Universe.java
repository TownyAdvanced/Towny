package com.palmergames.bukkit.towny;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

/**
 * A universe base class supporting the registration of a sub-universes and super-universes.
 * <p>
 * All sub-universes and super-universes must be instances of {@link Universe} or a subclass thereof.  Sub-universes and
 * and super-universes are registered by class such that only one instance of a given class can be registered as a
 * sub/super-universe of a universe at any given time.  (Note that this does not prevent a universe from simultaneously
 * having different instances of a class as a sub-universe and a super-universe.)
 */
public class Universe {
    private final Map<Class<? extends Universe>, Universe> subUniverses = new HashMap<>();
    private final Map<Class<? extends Universe>, Universe> superUniverses = new HashMap<>();

    /**
     * Deregister a sub-universe of this universe.
     *
     * @param subUniverse The sub-universe to register.
     * @return true if registration is successful.
     * @throws RuntimeException if deregistration fails partway and rollback is unsuccessful.
     */
    protected boolean deregister(@NotNull Universe subUniverse) {
        Class<? extends Universe> subUniverseClass = subUniverse.getClass();
        boolean success = subUniverses.remove(subUniverseClass, subUniverse);
        if (success) {
            Class<? extends Universe> thisClass = this.getClass();
            success = superUniverses.remove(thisClass, this);
            if (!success) // Undo sub-universe deregistration if super-universe deregistration failed.
                if (subUniverses.remove(subUniverseClass, subUniverse))
                    throw new RuntimeException("unable to roll back incomplete deregistration of a "
                            + subUniverseClass.getCanonicalName() + " instance (" + subUniverse.toString() + ") from a "
                            + thisClass.getCanonicalName() + " instance (" + this.toString() + ")");
        }
        return success;
    }

    /**
     * Get the sub-universe of the specified class if one is registered.
     *
     * @param subUniverseClass the class of the sub-universe.
     * @return the sub-universe of the specified class if one is registered, null otherwise.
     */
    @Nullable
    public final Universe getSubuniverse(@NotNull Class<? extends Universe> subUniverseClass) {
        return subUniverses.get(subUniverseClass);
    }

    /**
     * Get the super-universe of the specified class if one is registered.
     *
     * @param superUniverseClass the class of the super-universe.
     * @return the super-universe of the specified class if one is registered, null otherwise.
     */
    @Nullable
    public final Universe getSuperUniverse(@NotNull Class<? extends Universe> superUniverseClass) {
        return superUniverses.get(superUniverseClass);
    }

    /**
     * Test if a sub-universe of the specified class is registered.
     *
     * @param subUniverseClass the class of the sub-universe.
     * @return true if a sub-universe of the specified class is registered.
     */
    @Contract(pure = true)
    public final boolean hasSubUniverse(@NotNull Class<? extends Universe> subUniverseClass) {
        return subUniverses.containsKey(subUniverseClass);
    }

    /**
     * Test if a super-universe of the specified class is registered.
     *
     * @param superUniverseClass the class of the super-universe.
     * @return true if a super-universe of the specified class is registered.
     */
    @Contract(pure = true)
    public final boolean hasSuperUniverse(@NotNull Class<? extends Universe> superUniverseClass) {
        return superUniverses.containsKey(superUniverseClass);
    }

    /**
     * Rest if the specified sub-universe is registered.
     *
     * @param subUniverse the sub-universe.
     * @return true if the sub-universe is registered.
     */
    public final boolean isSubuniverse(@NotNull Universe subUniverse) {
        Class<? extends Universe> subUniverseClass = subUniverse.getClass();
        return subUniverses.get(subUniverseClass) == subUniverse;
    }

    /**
     * Test if the specified super-universe is registered.
     *
     * @param superUniverse the super-universe.
     * @return true if the super-universe is registered.
     */
    public final boolean isSuperUniverse(@NotNull Universe superUniverse) {
        Class<? extends Universe> superUniverseClass = superUniverse.getClass();
        return superUniverses.get(superUniverseClass) == superUniverse;
    }

    /**
     * Register a sub-universe of this universe.
     * <p>
     * At most one instance of a class can be registered at a given time.  Attempting to register an instance of a class
     * that has already been registered will fail.
     *
     * @param subUniverse The sub-universe to register.
     * @return true if registration is successful.
     * @throws RuntimeException if registration fails partway and rollback is unsuccessful.
     */
    protected final boolean register(@NotNull Universe subUniverse) {
        Class<? extends Universe> subUniverseClass = subUniverse.getClass();
        boolean success = subUniverses.putIfAbsent(subUniverseClass, subUniverse) == null;
        if (success) {
            Class<? extends Universe> thisClass = this.getClass();
            success = (superUniverses.putIfAbsent(thisClass, this) == null);
            if (!success) // Undo sub-universe registration if super-universe registration failed.
                if (subUniverses.remove(subUniverseClass, subUniverse))
                    throw new RuntimeException("unable to roll back incomplete registration of a "
                            + subUniverseClass.getCanonicalName() + " instance (" + subUniverse.toString() + ") to a "
                            + thisClass.getCanonicalName() + " instance (" + this.toString() + ")");
        }
        return success;
    }
}
