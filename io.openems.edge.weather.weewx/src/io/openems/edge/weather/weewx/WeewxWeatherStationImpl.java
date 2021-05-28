package io.openems.edge.weather.weewx;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.weather.weewx", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class WeewxWeatherStationImpl extends AbstractOpenemsComponent implements OpenemsComponent, EventHandler {

	private Config config = null;
	private WeewxAPI weewx = null; 
	private final Logger log = LoggerFactory.getLogger(WeewxWeatherStationImpl.class);

	public WeewxWeatherStationImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				WeewxWeatherStation.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		String URL = "http://" + config.ip() + ":" + config.port() + config.path() + "/" + config.file();
		this.weewx = new WeewxAPI(URL);
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
			// fill channels
			this.getCurrentValues();
			break;
		}
	}
	
	private void getCurrentValues() {
		try {
			JsonObject json = this.weewx.getCurrentWeatherData();
			if (json != null) {		
				JsonObject current = json.get("current").getAsJsonObject();
				switch(this.config.weatherstation()) {
				case DNT_WEATHERSTATION_PRO:				
					this.channel(WeewxWeatherStation.ChannelId.TEMPERATURE).setNextValue(current.getAsJsonObject().get("temperature").getAsJsonObject().get("value").getAsInt());
					this.channel(WeewxWeatherStation.ChannelId.SOLARRADIATION).setNextValue(current.getAsJsonObject().get("solar radiation").getAsJsonObject().get("value").getAsInt());
					this.channel(WeewxWeatherStation.ChannelId.MAX_SOLARRADIATION).setNextValue(current.getAsJsonObject().get("max solar radiation").getAsJsonObject().get("value").getAsInt());				
				}
			}
		}
		catch (OpenemsNamedException e) {
			log.debug(e.getMessage());
		}
	}

	@Override
	public String debugLog() {
		return "Temperature: " + this.channel(WeewxWeatherStation.ChannelId.TEMPERATURE).getNextValue().toString() +
				" | Solarradiation: " + this.channel(WeewxWeatherStation.ChannelId.SOLARRADIATION).getNextValue().toString() +
				" | max Solarradiation: " + this.channel(WeewxWeatherStation.ChannelId.MAX_SOLARRADIATION).getNextValue().toString();
	}
}
