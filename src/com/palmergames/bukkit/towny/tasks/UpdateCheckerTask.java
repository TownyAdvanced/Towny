package com.palmergames.bukkit.towny.tasks;

import com.google.gson.JsonParser;
import com.palmergames.bukkit.towny.Towny;
import com.palmergames.bukkit.towny.TownyUpdateChecker;
import com.palmergames.bukkit.towny.object.Translation;
import com.palmergames.bukkit.util.Colors;
import com.palmergames.bukkit.util.Version;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class UpdateCheckerTask implements Runnable {
	private final Towny towny;
	
	public UpdateCheckerTask(Towny towny) {
		this.towny = towny;
	}
	
	@Override
	public void run() {
		towny.getLogger().info(Translation.of("msg_checking_for_updates"));

		try {
			URL url = new URL("https://api.github.com/repos/TownyAdvanced/Towny/releases");
			HttpURLConnection connection = (HttpURLConnection) url.openConnection();
			
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
				try {
					// TODO: Replace this when support for MC 1.16.* is dropped.
					@SuppressWarnings("deprecation")
					Version latestVersion = Version.fromString(new JsonParser().parse(reader).getAsJsonArray().get(0).getAsJsonObject().get("tag_name").getAsString());
					boolean upToDate = Version.fromString(towny.getVersion()).compareTo(latestVersion) >= 0;
					
					if (!upToDate) {
						TownyUpdateChecker.setUpdate(true);
						TownyUpdateChecker.setNewVersion(latestVersion);
						
						towny.getLogger().info(Colors.strip(Translation.of("msg_new_update_available", latestVersion, towny.getVersion())));
						towny.getLogger().info(Translation.of("msg_download_here", "https://github.com/TownyAdvanced/Towny/releases/tag/" + latestVersion));
					} else {
						towny.getLogger().info(Translation.of("msg_no_new_updates"));
						TownyUpdateChecker.setCheckedSuccessfully(true);
					}
				} catch (Exception ignored) {}
			}
		} catch (IOException e) {
			towny.getLogger().info(Translation.of("msg_no_new_updates"));
		}
	}
}
