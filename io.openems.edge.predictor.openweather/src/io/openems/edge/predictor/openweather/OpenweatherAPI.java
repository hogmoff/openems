package io.openems.edge.predictor.openweather;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.JsonUtils;

public class OpenweatherAPI {
	
	private final String currentUrl;
	private final String hourlyUrl;
	private int cycle;
	private int cycles;
	private JsonObject currentjsonData;

	public OpenweatherAPI(String currentURL, String hourlyURL, int Cycles) {
		this.currentUrl = currentURL;
		this.hourlyUrl = hourlyURL;
		this.cycle = 0;
		this.cycles = Cycles;
		this.currentjsonData = null;
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

		if (this.cycle == this.cycles-1 || this.currentjsonData == null) {
			JsonObject json = new JsonObject();
			json = this.sendGetRequest(this.currentUrl);		
		
			JsonObject current = json.get("current").getAsJsonObject();
			
			JsonObject weatherData = new JsonObject();
			weatherData.add("time", current.getAsJsonObject().get("dt"));
			weatherData.add("temperature", current.getAsJsonObject().get("temp"));
			weatherData.add("clouds", current.getAsJsonObject().get("clouds"));	
			weatherData.add("uvi", current.getAsJsonObject().get("uvi"));	
			weatherData.add("pressure", current.getAsJsonObject().get("pressure"));	
			weatherData.add("humidity", current.getAsJsonObject().get("humidity"));	
			weatherData.add("wind_speed", current.getAsJsonObject().get("wind_speed"));	
			weatherData.add("wind_deg", current.getAsJsonObject().get("wind_deg"));	
			weatherData.add("visibility", current.getAsJsonObject().get("visibility"));	
				
			this.cycle = 0;
			this.currentjsonData = weatherData;
			return weatherData;
		}
		else {
			this.cycle++;
			return this.currentjsonData;
		}
	}
	
	/**
	 * Gets the current weather data from Openweather
	 * 
	 * See https://openweathermap.org/api/one-call-api#current
	 * 
	 * @return the weather data
	 * @throws OpenemsNamedException on error
	 */
	public JsonArray getWeatherPrediction(int hours) throws OpenemsNamedException {
		
		try {
			JsonObject json = new JsonObject();
			json = this.sendGetRequest(this.hourlyUrl);		
		
			JsonArray forecasts = (JsonArray) json.get("hourly");			
			JsonArray weatherPrediction = new JsonArray();
			for (Integer i = 0; i < 2*hours; i++) {	
				JsonObject predictData = new JsonObject();
				predictData.add("time", forecasts.get(i).getAsJsonObject().get("dt"));
				predictData.add("temperature", forecasts.get(i).getAsJsonObject().get("temp"));
				predictData.add("clouds", forecasts.get(i).getAsJsonObject().get("clouds"));	
				predictData.add("uvi", forecasts.get(i).getAsJsonObject().get("uvi"));	
				predictData.add("pressure", forecasts.get(i).getAsJsonObject().get("pressure"));	
				predictData.add("humidity", forecasts.get(i).getAsJsonObject().get("humidity"));	
				predictData.add("wind_speed", forecasts.get(i).getAsJsonObject().get("wind_speed"));	
				predictData.add("wind_deg", forecasts.get(i).getAsJsonObject().get("wind_deg"));	
				predictData.add("visibility", forecasts.get(i).getAsJsonObject().get("visibility"));		
				weatherPrediction.add(predictData);
			}				
			return weatherPrediction;	
			
		} catch (Exception e) {
			e.printStackTrace();
			return null;
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
