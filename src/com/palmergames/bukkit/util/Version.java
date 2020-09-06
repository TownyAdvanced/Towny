package com.palmergames.bukkit.util;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Version implements Comparable<Version> {

	private final String version;
	private static final Pattern SEPARATOR = Pattern.compile("\\.");
	private static final Pattern VERSION_PATTERN = Pattern.compile("[0-9]+(" + SEPARATOR + "[0-9]+)*");
	public final String get() {
		return this.version;
	}

	private Version(String version) {
		this.version = version;
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

	@Override 
	public int compareTo(@NotNull Version that) {
		String[] thisParts = this.get().split(SEPARATOR.pattern());
		String[] thatParts = that.get().split(SEPARATOR.pattern());
		
		int length = Math.max(thisParts.length, thatParts.length);
		for(int i = 0; i < length; i++) {
			int thisPart = i < thisParts.length ? Integer.parseInt(thisParts[i]) : 0;
			int thatPart = i < thatParts.length ? Integer.parseInt(thatParts[i]) : 0;
			
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
}
