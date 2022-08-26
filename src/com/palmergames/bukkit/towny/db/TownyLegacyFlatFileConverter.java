package com.palmergames.bukkit.towny.db;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Properties;
import java.util.UUID;

public class TownyLegacyFlatFileConverter {

	public static UUID getUUID(File file) {
		if (file.exists() && file.isFile()) {
			try (FileInputStream fis = new FileInputStream(file);
					InputStreamReader isr = new InputStreamReader(fis, StandardCharsets.UTF_8)) {
				Properties properties = new Properties();
				properties.load(isr);
				String uuidAsString = properties.getProperty("uuid");
				return UUID.fromString(uuidAsString); 
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return null;
	}

}
