package io.openems.edge.weather.weewx;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface WeewxWeatherStation extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		TEMPERATURE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.DEGREE_CELSIUS)),
		SOLARRADIATION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.NONE)),
		MAX_SOLARRADIATION(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.NONE));
		
		private final Doc doc;
				
		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	public enum WeatherstationType {
		// Weatherstation PRO from dnt
		// https://www.dnt.de/Produkte/WiFi-Wetterstation-WeatherScreen-PRO/
		DNT_WEATHERSTATION_PRO,
	}

}
