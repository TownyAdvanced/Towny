package com.palmergames.bukkit.towny.test;

import com.palmergames.util.Pair;
import io.papermc.paper.ServerBuildInfo;
import io.papermc.paper.registry.RegistryAccess;
import io.papermc.paper.registry.RegistryKey;
import io.papermc.paper.registry.TypedKey;
import net.kyori.adventure.key.Key;
import org.bukkit.Bukkit;
import org.bukkit.Keyed;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.Server;
import org.bukkit.entity.EntityType;
import org.junit.jupiter.api.extension.AfterAllCallback;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mockito.MockedStatic;
import org.mockito.ScopedMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

import static org.mockito.Mockito.*;

/**
 * Attempts to mock the minimal amount of things for registries to "work"
 */
@SuppressWarnings("UnstableApiUsage")
public class BukkitMockExtension implements BeforeAllCallback, AfterAllCallback {
	private static final Logger LOGGER = LoggerFactory.getLogger(BukkitMockExtension.class);

	private static RegistryAccess REGISTRY_ACCESS_MOCK = null;
	private static final MethodHandle BUKKIT_SERVER_HANDLE;
	
	static {
		MethodHandle temp = null;
		
		try {
			temp = MethodHandles.privateLookupIn(Bukkit.class, MethodHandles.lookup()).findStaticSetter(Bukkit.class, "server", Server.class);
		} catch (Throwable ignored) {}
		BUKKIT_SERVER_HANDLE = temp;
	}

	private final Set<ScopedMock> openMocks = new HashSet<>();

	public void initializeBaseMocks() {
		mockRegistries();
		mockServer();
	}

	@SuppressWarnings("removal")
	private void mockRegistries() {
		RegistryAccess registryAccessMock = REGISTRY_ACCESS_MOCK != null ? REGISTRY_ACCESS_MOCK : mock(RegistryAccess.class);

		final MockedStatic<RegistryAccess> staticRegistryMock = mockStatic(RegistryAccess.class);
		staticRegistryMock.when(RegistryAccess::registryAccess).thenReturn(registryAccessMock);
		openMocks.add(staticRegistryMock);

		if (REGISTRY_ACCESS_MOCK != null) {
			return;
		}

		REGISTRY_ACCESS_MOCK = registryAccessMock;

		when(registryAccessMock.getRegistry(any(RegistryKey.class))).thenAnswer((Answer<Registry<?>>) invocation -> {
			final Registry<?> mock = mock();
			when(mock.toString()).thenReturn("empty registry mock");
			return mock;
		});
		when(registryAccessMock.getRegistry(any(Class.class))).thenAnswer((Answer<Registry<?>>) invocation -> mock());

		mockRegistry(RegistryKey.ENTITY_TYPE, EntityType.class, retrieveFakeRegistryKeys(RegistryKey.ENTITY_TYPE, EntityType.class), registryAccessMock);
	}

	private void mockServer() {
		final Server serverMock = mock(Server.class);
		
		when(serverMock.getMinecraftVersion()).thenReturn("26.1.1");
		when(serverMock.getLogger()).thenReturn(java.util.logging.Logger.getLogger("Minecraft"));
		when(serverMock.getPluginManager()).thenReturn(mock());
		
		final ServerBuildInfo buildInfoMock = mock(ServerBuildInfo.class);
		when(buildInfoMock.asString(any())).thenReturn("");

		final MockedStatic<ServerBuildInfo> mockedStatic = mockStatic(ServerBuildInfo.class);
		mockedStatic.when(ServerBuildInfo::buildInfo).thenReturn(buildInfoMock);
		openMocks.add(mockedStatic);

		try {
			BUKKIT_SERVER_HANDLE.invokeExact(serverMock);
		} catch (Throwable e) {
			LOGGER.error("Failed to update server field", e);
		}
	}
	
	@SuppressWarnings("unchecked")
	private static <T extends Keyed> Set<Pair<TypedKey<T>, T>> retrieveRegistryKeys(final Class<?> registryKeysClass) {
		final Set<Pair<TypedKey<T>, T>> keys = new HashSet<>();

		try {
			for (final Field field : registryKeysClass.getFields()) {
				if (!Modifier.isStatic(field.getModifiers()) || !Modifier.isFinal(field.getModifiers())) {
					continue;
				}

				final TypedKey<T> value = (TypedKey<T>) field.get(null);
				keys.add(Pair.pair(value, null));
			}
		} catch (ReflectiveOperationException e) {
			LOGGER.error("Failed to retrieve registry keys for class {}", "", e);
		}
		
		return keys;
	}
	
	private static <T extends Keyed> Set<Pair<TypedKey<T>, T>> retrieveFakeRegistryKeys(final RegistryKey<T> registryKey, final Class<T> enumClass) {
		final Set<Pair<TypedKey<T>, T>> keys = new HashSet<>();

		for (final T constant : enumClass.getEnumConstants()) {
			try {
				keys.add(Pair.pair(TypedKey.create(registryKey, constant.key()), constant));
			} catch (IllegalArgumentException ignored) {}
		}

		return keys;
	}

	@SuppressWarnings({"unchecked", "removal"})
	private static <T extends Keyed> void mockRegistry(final RegistryKey<T> registryKey, final Class<T> registryClass, final Set<Pair<TypedKey<T>, T>> keys, final RegistryAccess registryAccessMock) { // extends Keyed might be removed in the future
		final Registry<T> mockRegistry = mock(Registry.class);

		when(registryAccessMock.getRegistry(eq(registryKey))).thenReturn(mockRegistry);
		when(registryAccessMock.getRegistry(eq(registryClass))).thenReturn(mockRegistry);
		
		when(mockRegistry.get(any(Key.class))).thenReturn(null);
		when(mockRegistry.get(any(NamespacedKey.class))).thenReturn(null);
		when(mockRegistry.get(any(TypedKey.class))).thenReturn(null);

		when(mockRegistry.getOrThrow(any(Key.class))).thenThrow(new NoSuchElementException());
		when(mockRegistry.getOrThrow(any(NamespacedKey.class))).thenThrow(new NoSuchElementException());
		when(mockRegistry.getOrThrow(any(TypedKey.class))).thenThrow(new NoSuchElementException());
		
		final Set<T> allMocks = new HashSet<>();
		for (final Pair<TypedKey<T>, T> pair : keys) {
			final TypedKey<T> key = pair.left();
			
			final T instance = pair.right() != null ? pair.right() : mock(registryClass);
			allMocks.add(instance);
			
			when(mockRegistry.get(eq(key.key()))).thenReturn(instance);
			when(mockRegistry.get(eq(key))).thenReturn(instance);
			
			final NamespacedKey otherKey = NamespacedKey.fromString(key.asString());
			when(mockRegistry.get(eq(Objects.requireNonNull(otherKey)))).thenReturn(instance);
			
			when(mockRegistry.getKey(eq(instance))).thenAnswer((Answer<NamespacedKey>) invocation -> otherKey);
			when(mockRegistry.getKeyOrThrow(eq(instance))).thenAnswer((Answer<NamespacedKey>) invocation -> otherKey);
		}
		
		when(mockRegistry.iterator()).thenAnswer((Answer<Iterator<T>>) invocation -> allMocks.iterator());
	}

	@Override
	public void afterAll(ExtensionContext context) {
		openMocks.forEach(ScopedMock::closeOnDemand);
		openMocks.clear();

		try {
			BUKKIT_SERVER_HANDLE.invokeExact((Server) null);
		} catch (Throwable e) {
			LOGGER.error("Failed to clear server field", e);
		}
	}

	@Override
	public void beforeAll(ExtensionContext context) {
		initializeBaseMocks();
	}
}
