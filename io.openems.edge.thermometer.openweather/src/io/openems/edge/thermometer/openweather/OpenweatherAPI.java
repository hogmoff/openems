package io.openems.edge.thermometer.openweather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OpenweatherAPI {
	
	private final String Url;
	private int cycle;
	private int cycles;
	private JsonObject jsonData;

	public OpenweatherAPI(String URL, int Cycles) {
		this.Url = URL;
		this.cycle = 0;
		this.cycles = Cycles;
		this.jsonData = null;
	}

	/**
	 * Gets the current weather data from Openweather
	 * 
	 * See https://openweathermap.org/current#parameter
	 * 
	 * @return the weather data
	 * @throws OpenemsNamedException on error
	 */
	public JsonObject getCurrentWeatherData() throws OpenemsNamedException {

		if (this.cycle == this.cycles-1 || this.jsonData == null) {
			JsonObject json = new JsonObject();
			json = this.sendGetRequest(this.Url);		
		
			JsonObject main = json.get("main").getAsJsonObject();
			JsonObject clouds = json.get("clouds").getAsJsonObject();
			
			JsonObject weatherData = new JsonObject();
			weatherData.add("temperature", main.getAsJsonObject().get("temp"));
			weatherData.add("clouds", clouds.getAsJsonObject().get("all"));			
				
			this.cycle = 0;
			this.jsonData = weatherData;
			return weatherData;
		}
		else {
			this.cycle++;
			return this.jsonData;
		}
	}

	/**
	 * Sends a get request to the Openweather API.
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
						"Error while reading from Openweather API. Response code: " + status + ". " + body);
			}
		} catch (OpenemsException | IOException e) {
			throw new OpenemsException(
					"Unable to read from Openweather API, check API-Key");
		}
	}
	
}
