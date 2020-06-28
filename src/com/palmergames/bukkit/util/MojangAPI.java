package com.palmergames.bukkit.util;

import org.json.simple.JSONObject;
import org.json.simple.JSONValue;

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

	static JSONObject send(String url) throws IOException {
		return (JSONObject) JSONValue.parse(sendGetRequest(url));
	}

	static String dashUUID(String undashed) {
		return undashed.replaceAll(uuidCreator.pattern(), "$1-$2-$3-$4-$5");
	}

	private static String sendGetRequest(String URL) throws IOException {
		StringBuilder builder = new StringBuilder();

		try {
			HttpURLConnection connection = (HttpURLConnection) new URL(URL).openConnection();
			connection.setRequestMethod("GET");

			if (connection.getResponseCode() == 204)
				throw new IOException();
		
			BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
			String response;
			while ((response = reader.readLine()) != null) {
				builder.append(response);
			}
			reader.close();
		} catch (IOException e1) {
			throw new IOException();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return builder.toString();
	}
}