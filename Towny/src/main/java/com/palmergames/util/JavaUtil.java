package com.palmergames.util;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.CopyOption;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class JavaUtil {

	/**
	 * Recursively check if the interface inherits the super interface. Returns
	 * false if not an interface. Returns true if sup = sub.
	 * 
	 * @param sup The class of the interface you think it is a subinterface of.
	 * @param sub The possible subinterface of the super interface.
	 * @return true if it is a subinterface.
	 */

	public static boolean isSubInterface(Class<?> sup, Class<?> sub) {

		if (sup.isInterface() && sub.isInterface()) {
			if (sup.equals(sub))
				return true;
			for (Class<?> c : sub.getInterfaces())
				if (isSubInterface(sup, c))
					return true;
		}
		return false;
	}

	public static List<String> readTextFromJar(String path) throws IOException {
		try (InputStream is = readResource(path)) {
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
				return reader.lines().collect(Collectors.toList());
			}
		}
	}
	
	@NotNull
	public static InputStream readResource(String resource) throws IOException {
		InputStream is = JavaUtil.class.getResourceAsStream(resource);
		if (is == null)
			throw new FileNotFoundException("Could not find '" + resource + "' inside the jar as a resource.");

		return is;
	}
	
	public static void saveResource(String resource, Path destination, CopyOption... options) throws IOException {
		try (InputStream is = readResource(resource)) {
			Files.copy(is, destination, options);
		}
	}
	
	public static boolean classExists(@NotNull String className) {
		try {
			Class.forName(className);
			return true;
		} catch (ClassNotFoundException e) {
			return false;
		}
	}
	
	public static <T> T make(Supplier<T> supplier) {
		return supplier.get();
	}
	
	public static <T> T make(T initial, Consumer<T> initializer) {
		initializer.accept(initial);
		return initial;
	}

	public static @Nullable MethodHandle getMethodHandle(final @NotNull String className, final @NotNull String methodName) {
		try {
			return getMethodHandle(Class.forName(className), methodName);
		} catch (ClassNotFoundException e) {
			return null;
		}
	}

	public static @Nullable MethodHandle getMethodHandle(final @NotNull Class<?> clazz, final @NotNull String methodName) {
		try {
			final Method method = clazz.getDeclaredMethod(methodName);
			method.setAccessible(true);
			return MethodHandles.publicLookup().unreflect(method);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}
	
	public static @Nullable MethodHandle getMethodHandle(final @NotNull Class<?> clazz, final @NotNull String methodName, final Class<?>... paramTypes) {
		try {
			final Method method = clazz.getDeclaredMethod(methodName, paramTypes);
			method.setAccessible(true);
			return MethodHandles.publicLookup().unreflect(method);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}
	
	public static @Nullable MethodHandle getFieldHandle(final @NotNull Class<?> clazz, final @NotNull String fieldName) {
		try {
			final Field field = clazz.getDeclaredField(fieldName);
			field.setAccessible(true);
			return MethodHandles.publicLookup().unreflectGetter(field);
		} catch (ReflectiveOperationException e) {
			return null;
		}
	}

	@NotNull
	@Contract(pure = true)
	public static UUID changeUUIDVersion(final @NotNull UUID uuid, final int version) {
		if (uuid.version() == version)
			return uuid;

		final ByteBuffer buffer = ByteBuffer.wrap(new byte[16]);
		buffer.putLong(uuid.getMostSignificantBits());
		buffer.putLong(uuid.getLeastSignificantBits());

		final byte[] bytes = buffer.array();

		bytes[6] = (byte) (version << 4);

		final ByteBuffer rewrapped = ByteBuffer.wrap(bytes);
		final long mostSig = rewrapped.getLong();
		final long leastSig = rewrapped.getLong();

		return new UUID(mostSig, leastSig);
	}

	@Nullable
	public static UUID parseUUIDOrNull(final String uuid) {
		if (uuid == null) {
			return null;
		}

		try {
			return UUID.fromString(uuid);
		} catch (IllegalArgumentException e) {
			return null;
		}
	}
}
