package io.openems.edge.predictor.openweather;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.TimeZone;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.component.ClockProvider;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.predictor.api.oneday.AbstractPredictor24Hours;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Predictor.Openweather", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class OpenweatherImpl extends AbstractPredictor24Hours implements Predictor24Hours, OpenemsComponent, EventHandler {
	
	private final Logger log = LoggerFactory.getLogger(OpenweatherImpl.class);

	private OpenweatherAPI openweatherAPI = null; 
	private boolean executed;	
	LocalDateTime prevHour = LocalDateTime.now();
	private TreeMap<LocalDateTime, Integer> hourlyTemp = new TreeMap<LocalDateTime, Integer>();
	private TreeMap<LocalDateTime, Integer> hourlyClouds = new TreeMap<LocalDateTime, Integer>();
		
	@Reference
	private ComponentManager componentManager;

	public OpenweatherImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				Openweather.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.channelAddresses());
		String currentUrl = "http://api.openweathermap.org/data/2.5/onecall?lat=" + config.lat() + "&lon=" + config.lon() + 
				"&units=metric&exclude=minutely,hourly,daily,alerts&appid=" + config.key();
		String predictUrl = "http://api.openweathermap.org/data/2.5/onecall?lat=" + config.lat() + "&lon=" + config.lon() + 
				"&units=metric&exclude=current,minutely,daily,alerts&appid=" + config.key();
		this.openweatherAPI = new OpenweatherAPI(currentUrl, predictUrl, config.cycles());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	@Override
	protected Prediction24Hours createNewPrediction(ChannelAddress channelAddress) {
		
		Integer[] result = new Integer[96];		
		if (channelAddress.getChannelId().equals("Predict_Temperature")) {
			int i = Math.max(0, 24 - this.hourlyTemp.size());
			for (Entry<LocalDateTime, Integer> entry : this.hourlyTemp.entrySet()) {
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
			}
		}
		else if (channelAddress.getChannelId().equals("Predict_Clouds")) {
			int i = Math.max(0, 24 - this.hourlyClouds.size());
			for (Entry<LocalDateTime, Integer> entry : this.hourlyClouds.entrySet()) {
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
			}
		}
		else {
			return null;
		}
		return new Prediction24Hours(result);
	}
	
	@Override
	protected ClockProvider getClockProvider() {
		return this.componentManager;
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			try {
				this.calculatePrediction();
				JsonObject json = this.openweatherAPI.getCurrentWeatherData();
				this.channel(Openweather.ChannelId.TEMPERATURE).setNextValue(json.get("temperature").getAsInt());
				this.channel(Openweather.ChannelId.CLOUDS).setNextValue(json.get("clouds").getAsInt());
				this.channel(Openweather.ChannelId.UNABLE_TO_PREDICT).setNextValue(false);				
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				this.channel(Openweather.ChannelId.UNABLE_TO_PREDICT).setNextValue(true);
			}
		}
	}
	
	/*
	 * This method gets the value from the Channel every one hour and updates the TreeMap.
	 */
	private void calculatePrediction() throws OpenemsNamedException {
		LocalDateTime currentHour = LocalDateTime.now(this.componentManager.getClock()).withNano(0).withMinute(0).withSecond(0);
		JsonArray js = null;

		if (!executed) {
			// First time execution - Map is still empty	
			js = this.openweatherAPI.getWeatherPrediction(24);			
			this.prevHour = currentHour;
			this.executed = true;
			
		} else if (currentHour.isAfter(this.prevHour)) {
			// hour changed -> get new forecast
			js = this.openweatherAPI.getWeatherPrediction(24);

			this.prevHour = currentHour;
		} else {
			// hour did not change -> return
			return;
		}
		
		if (js != null) {
			hourlyTemp.clear();
			hourlyClouds.clear();
			for (Integer i = 0; i < js.size(); i++) {			
				int time = js.get(i).getAsJsonObject().get("time").getAsInt();
				LocalDateTime t = LocalDateTime.ofInstant(Instant.ofEpochSecond(time),
                        TimeZone.getDefault().toZoneId());
				JsonElement temp = js.get(i).getAsJsonObject().get("temperature");
				JsonElement clouds = js.get(i).getAsJsonObject().get("clouds");
				hourlyTemp.put(t, temp.getAsInt());
				hourlyClouds.put(t, clouds.getAsInt());
			}	
			this.channel(Openweather.ChannelId.PREDICT_ENABLED).setNextValue(true);
			
		}
		else {			
			// remove first element and add new elements with null
			hourlyTemp.pollFirstEntry();
			hourlyClouds.pollFirstEntry();
			
			LocalDateTime DateEnd;
			if (hourlyTemp.isEmpty()) {
				DateEnd = currentHour;
			}
			else {
				DateEnd = hourlyTemp.lastKey().plusHours(1);
			}
			hourlyTemp.put(DateEnd, null);
			hourlyClouds.put(DateEnd, null);
			this.channel(Openweather.ChannelId.PREDICT_ENABLED).setNextValue(false);			
			
		}		
	}

	@Override
	public String debugLog() {
		return "Temperature: " + this.channel(Openweather.ChannelId.TEMPERATURE).value().toString() + " " +
				" - Clouds: " +	this.channel(Openweather.ChannelId.CLOUDS).value().toString();
	}
}
