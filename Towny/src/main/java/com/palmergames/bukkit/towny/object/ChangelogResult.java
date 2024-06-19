package com.palmergames.bukkit.towny.object;

import java.util.Collection;

/**
 * Represents a read changelog from a {@link com.palmergames.bukkit.towny.utils.ChangelogReader}.
 * 
 * @param lines The changelog lines, will be empty if the changelog could not be read successfully.
 * @param successful Whether the changelog file was read successfully, currently only false if the last version could not be found.
 * @param limitReached Whether the specified line limit was reached during reading.
 * @param nextVersionIndex The line index of where the first version changelog for the first version after the last version starts.
 * @param totalSize The total line count of the changelog.
 */
public record ChangelogResult(Collection<String> lines, boolean successful, boolean limitReached, int nextVersionIndex, int totalSize) {}
