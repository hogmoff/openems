package io.openems.edge.thermometer.openweather;

import java.time.LocalDateTime;

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

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Thermometer.Openweather", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class OpenweatherImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {
	
	private final Logger log = LoggerFactory.getLogger(OpenweatherImpl.class);

	private OpenweatherAPI openweatherAPI = null; 
	LocalDateTime prevHour = LocalDateTime.now();	
		
	@Reference
	private ComponentManager componentManager;

	public OpenweatherImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Openweather.ChannelId.values() //
		);
	}

	@Activate
	protected void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.openweatherAPI = new OpenweatherAPI("http://api.openweathermap.org/data/2.5/weather?lat=" + config.lat() + "&lon=" + config.lon() + "&appid=" + config.key(), config.cycles());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			try {
				JsonObject json = this.openweatherAPI.getCurrentWeatherData();
				this.channel(Openweather.ChannelId.TEMPERATURE).setNextValue(json.get("temperature").getAsInt()-273);
				this.channel(Openweather.ChannelId.CLOUDS).setNextValue(json.get("clouds").getAsInt());
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				this.channel(Openweather.ChannelId.TEMPERATURE).setNextValue(0);
				this.channel(Openweather.ChannelId.CLOUDS).setNextValue(0);
			}
		}
	}

	@Override
	public String debugLog() {
		return "Temperature: " + this.channel(Openweather.ChannelId.TEMPERATURE).value().toString() + " " +
				" - Clouds: " +	this.channel(Openweather.ChannelId.CLOUDS).value().toString();
	}
}
