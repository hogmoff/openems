package io.openems.edge.weather.weewx;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class WeewxAPI {
	
	private final String currentUrl;

	public WeewxAPI(String currentURL) {
		this.currentUrl = currentURL;
	}

	/**
	 * Gets the current weather data from Weewx json-file
	 * 
	 * https://github.com/teeks99/weewx-json
	 * 
	 * @return the weather data
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getCurrentWeatherData() throws OpenemsNamedException {

		JsonObject json = new JsonObject();
		json = this.sendGetRequest(this.currentUrl);	
		return json;
	}
	

	/**
	 * Sends a get request to the Weewx API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject or JsonArray
	 * @throws OpenemsNamedException on error
	 */
	private JsonObject sendGetRequest(String URL) throws OpenemsNamedException {
		try {
			URL url = new URL(URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setRequestMethod("GET");
			con.setConnectTimeout(5000);
			con.setReadTimeout(5000);
			int status = con.getResponseCode();
			String body;
			try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
				// Read HTTP response
				StringBuilder content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				body = content.toString();
			}
			if (status < 300) {
				// Parse response to JSON
				return JsonUtils.parseToJsonObject(body);
			} else {
				throw new OpenemsException(
						"Error while reading from Weewx API. Response code: " + status + ". " + body);
			}
		} catch (OpenemsException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Weewx API. Check IP-Adress, Port, Path and File");
		}
	}
	
}
