package com.palmergames.bukkit.util;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.palmergames.bukkit.towny.exceptions.MojangException;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.regex.Pattern;

/**
 * @author LlmDl with permission from creatorfromhell, using code snippets from
 *         TNELib (https://github.com/TheNewEconomy/TNELib)
 */
public class MojangAPI {

	static final Pattern uuidCreator = Pattern.compile("(\\w{8})(\\w{4})(\\w{4})(\\w{4})(\\w{12})");

	static JsonObject send(String url) throws IOException, MojangException {
		return sendGetRequest(url);
	}

	static String dashUUID(String undashed) {
		return undashed.replaceAll(uuidCreator.pattern(), "$1-$2-$3-$4-$5");
	}

	private static JsonObject sendGetRequest(String URL) throws IOException, MojangException {
		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
			connection.setRequestMethod("GET");

			if (connection.getResponseCode() == 204)
				throw new MojangException();
		
			try (BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
				return new JsonParser().parse(reader).getAsJsonObject();
			}
		} catch (MojangException e2) {
			throw new MojangException();
		} catch (IOException e1) {
			throw new IOException();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return new JsonObject();
	}
}