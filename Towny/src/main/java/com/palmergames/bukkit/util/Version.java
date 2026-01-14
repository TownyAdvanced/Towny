package com.palmergames.bukkit.util;

import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Represents a numbering system of a string format.
 */
public class Version implements Comparable<Version> {

	private final String version;
	private static final Pattern SEPARATOR = Pattern.compile("\\.");
	private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+(" + SEPARATOR + "[0-9]+)*");
	private final String[] components;

	private Version(String version) {
		this.version = version;
		components = version.split(SEPARATOR.pattern());
	}

	/**
	 * Constructs a Version object from the given string.
	 * 
	 * <p>This method will truncate any extraneous characters found
	 * after it matches the first qualified version string.</p>
	 * 
	 * @param version A string that contains a formatted version.
	 * @return A new Version instance from the given string.
	 */
	public static Version fromString(String version) {
		if(version == null) {
			throw new IllegalArgumentException("Version can not be null");
		}

		Matcher matcher = VERSION_PATTERN.matcher(version);
		if(!matcher.find()) {
			throw new IllegalArgumentException("Invalid version format: " + version);
		}
		
		return new Version(matcher.group(0));
	}

	/**
	 * Constructs a Version object from the given plugin.
	 * 
	 * <p>This method will truncate any extraneous characters found
	 * after it matches the first qualified version string.</p>
	 * 
	 * @param plugin A Plugin which will supply a version
	 * @return A new Version instance from the given plugin.
	 */
	public static Version fromPlugin(Plugin plugin) {
		if(plugin == null) {
			throw new IllegalArgumentException("Plugin can not be null");
		}
		@SuppressWarnings("deprecation") // TODO: Clean up when this is changed for good by Paper.
		String version = plugin.getDescription().getVersion();
		Matcher matcher = VERSION_PATTERN.matcher(version);
		if(!matcher.find()) {
			throw new IllegalArgumentException("Invalid version format: " + version);
		}
		
		return new Version(matcher.group(0));
	}

	@Override 
	public int compareTo(@NotNull Version that) {
		int length = Math.max(components.length, that.components.length);
		
		for(int i = 0; i < length; i++) {
			int thisPart = i < components.length ? Integer.parseInt(components[i]) : 0;
			int thatPart = i < that.components.length ? Integer.parseInt(that.components[i]) : 0;
			
			if (thisPart < thatPart) {
				return -1;
			}
			
			if (thisPart > thatPart) {
				return 1;
			}
		}
		return 0;
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (!(o instanceof Version)) return false;
		Version version1 = (Version) o;
		return Objects.equals(version, version1.version);
	}

	@Override
	public int hashCode() {
		return Objects.hash(version);
	}

	@Override
	public String toString() {
		return version;
	}

	/**
	 * Returns the components of the version separated by .'s
	 * 
	 * @return A string array of the components.
	 */
	@NotNull
	public String[] getComponents() {
		return components;
	}
	
	/**
	 * @return Whether the last component is not equal to 0
	 */
	public boolean isPreRelease() {
		try {
			return Integer.parseInt(this.components[components.length-1]) != 0;
		} catch (NumberFormatException e) {
			return false;
		}
	}
	
	/**
	 * @since 0.99.0.8
	 */
	public boolean isNewerThanOrEquals(@NotNull Version other) {
		return this.compareTo(other) >= 0;
	}
	
	/**
	 * @since 0.99.0.8
	 */
	public boolean isNewerThan(@NotNull Version other) {
		return this.compareTo(other) > 0;
	}
	
	/**
	 * @since 0.99.0.8
	 */
	public boolean isOlderThanOrEquals(@NotNull Version other) {
		return this.compareTo(other) <= 0;
	}
	
	/**
	 * @since 0.99.0.8
	 */
	public boolean isOlderThan(@NotNull Version other) {
		return this.compareTo(other) < 0;
	}
}
