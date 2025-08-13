package com.palmergames.bukkit.towny.object;

import com.palmergames.bukkit.towny.Towny;
import org.jetbrains.annotations.ApiStatus;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

/**
 * Represents git build information for a Towny jar file.
 * 
 * @param commit The full SHA-1 hash of the most recent commit.
 * @param commitShort The shortened hash of the most recent commit.
 * @param branch The branch this jar was built from.
 * @param repositoryUrl The url for the remote git repository.
 * @param message The subject of the most recent commit.
 */
@ApiStatus.Internal
public record BuildInfo(String commit, String commitShort, String branch, String repositoryUrl, String message) {
	public static BuildInfo retrieveBuildInfo(Towny plugin) throws IOException {
		try (final InputStream is = plugin.getResource("version.properties")) {
			if (is == null) {
				throw new IOException("Could not find version.properties in the Towny jar file.");
			}

			final Properties properties = new Properties();
			try (final InputStreamReader reader = new InputStreamReader(is)) {
				properties.load(reader);
			}

			return new BuildInfo(
				properties.getProperty("commit"),
				properties.getProperty("commit-short"),
				properties.getProperty("branch"),
				properties.getProperty("repository-url"),
				properties.getProperty("commit-message")
			);
		}
	}
}
