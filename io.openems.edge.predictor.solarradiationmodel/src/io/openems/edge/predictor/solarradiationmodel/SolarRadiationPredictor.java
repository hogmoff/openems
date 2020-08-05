package io.openems.edge.predictor.solarradiationmodel;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.predictor.api.HourlyPrediction;
import io.openems.edge.predictor.api.HourlyPredictor;
import io.openems.edge.predictor.solarradiationmodel.SolarRadiationPredictor;

public abstract class SolarRadiationPredictor extends AbstractOpenemsComponent implements HourlyPredictor {
	
	public SolcastAPI solarforcastAPI = null; 
	//private final ChannelAddress channelAddress;
	private final Clock clock;
	private boolean executed;
	LocalDateTime prevHour = LocalDateTime.now();
	private final Logger log = LoggerFactory.getLogger(SolarRadiationPredictor.class);
	private final TreeMap<LocalDateTime, Integer> hourlySolarData = new TreeMap<LocalDateTime, Integer>();
	private final TreeMap<LocalDateTime, Integer> hourlySolarData_10 = new TreeMap<LocalDateTime, Integer>();
	private final TreeMap<LocalDateTime, Integer> hourlySolarData_90 = new TreeMap<LocalDateTime, Integer>();
	
	protected SolarRadiationPredictor(Clock clock, String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorChannelId.values() //
		);
		//this.channelAddress = new ChannelAddress(componentId, channelId.id());
		this.clock = clock;
}
	
	protected SolarRadiationPredictor(String componentId,
			io.openems.edge.common.channel.ChannelId channelId) {
		this(Clock.systemDefaultZone(), componentId, channelId);
	}

	protected abstract ComponentManager getComponentManager();
	
	/**
	 * Collects the solar model data on every cycle.
	 * 
	 * @param event the Event provided by {@link EventHandler}.
	 */
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE:
			try {
				this.calculateEnergyValue();

				this.channel(PredictorChannelId.UNABLE_TO_PREDICT).setNextValue(false);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				this.channel(PredictorChannelId.UNABLE_TO_PREDICT).setNextValue(true);
			}

		}
	}
	
	/*
	 * This method gets the value from the Channel every one hour and updates the
	 * TreeMap.
	 */
	private void calculateEnergyValue() throws OpenemsNamedException {
		//LongReadChannel channel = this.getComponentManager().getChannel(this.channelAddress);
		LocalDateTime currentHour = LocalDateTime.now(this.clock).withNano(0).withMinute(0).withSecond(0);
		JsonArray js = null;

		if (!executed) {
			// First time execution - Map is still empty	
			js = this.solarforcastAPI.getSolarForecast(24);			
			this.prevHour = currentHour;
			this.executed = true;
			
		} else if (currentHour.isAfter(this.prevHour)) {
			// hour changed -> get new forecast
			js = this.solarforcastAPI.getSolarForecast(24);

			this.prevHour = currentHour;
		} else {
			// hour did not change -> return
			return;
		}
		
		if (js != null) {
			this.hourlySolarData.clear();
			this.hourlySolarData_10.clear();
			this.hourlySolarData_90.clear();
			for (Integer i = 0; i < js.size(); i++) {			
				JsonElement time = js.get(i).getAsJsonObject().get("time");
				LocalDateTime t = OffsetDateTime.parse(time.getAsString()).toLocalDateTime();
				JsonElement solar = js.get(i).getAsJsonObject().get("solarradiation");
				JsonElement solar10 = js.get(i).getAsJsonObject().get("solarradiation10");
				JsonElement solar90 = js.get(i).getAsJsonObject().get("solarradiation90");
				this.hourlySolarData.put(t, solar.getAsInt());
				this.hourlySolarData_10.put(t, solar10.getAsInt());
				this.hourlySolarData_90.put(t, solar90.getAsInt());
			}	
			
		}
		else {
			// add new Element with null and remove first element
			this.hourlySolarData.remove(this.hourlySolarData.firstKey());			
			this.hourlySolarData_10.remove(this.hourlySolarData_10.firstKey());			
			this.hourlySolarData_90.remove(this.hourlySolarData_90.firstKey());
			this.hourlySolarData.put(LocalDateTime.now(), null);
			this.hourlySolarData_10.put(LocalDateTime.now(), null);
			this.hourlySolarData_90.put(LocalDateTime.now(), null);
		}
		
		HourlyPrediction hourlyPrediction = this.get24hPrediction();
		this.channel(PredictorChannelId.PREDICT_00H).setNextValue(hourlyPrediction.getValues()[0]);
		this.channel(PredictorChannelId.PREDICT_01H).setNextValue(hourlyPrediction.getValues()[1]);
		this.channel(PredictorChannelId.PREDICT_02H).setNextValue(hourlyPrediction.getValues()[2]);
		this.channel(PredictorChannelId.PREDICT_03H).setNextValue(hourlyPrediction.getValues()[3]);
		this.channel(PredictorChannelId.PREDICT_04H).setNextValue(hourlyPrediction.getValues()[4]);
		this.channel(PredictorChannelId.PREDICT_05H).setNextValue(hourlyPrediction.getValues()[5]);
		this.channel(PredictorChannelId.PREDICT_06H).setNextValue(hourlyPrediction.getValues()[6]);
		this.channel(PredictorChannelId.PREDICT_07H).setNextValue(hourlyPrediction.getValues()[7]);
		this.channel(PredictorChannelId.PREDICT_08H).setNextValue(hourlyPrediction.getValues()[8]);
		this.channel(PredictorChannelId.PREDICT_09H).setNextValue(hourlyPrediction.getValues()[9]);
		this.channel(PredictorChannelId.PREDICT_10H).setNextValue(hourlyPrediction.getValues()[10]);
		this.channel(PredictorChannelId.PREDICT_11H).setNextValue(hourlyPrediction.getValues()[11]);
		this.channel(PredictorChannelId.PREDICT_12H).setNextValue(hourlyPrediction.getValues()[12]);
		
		HourlyPrediction hourlyPrediction_10 = this.get24hPrediction_10();
		this.channel(PredictorChannelId.PREDICT10_00H).setNextValue(hourlyPrediction_10.getValues()[0]);
		this.channel(PredictorChannelId.PREDICT10_01H).setNextValue(hourlyPrediction_10.getValues()[1]);
		this.channel(PredictorChannelId.PREDICT10_02H).setNextValue(hourlyPrediction_10.getValues()[2]);
		this.channel(PredictorChannelId.PREDICT10_03H).setNextValue(hourlyPrediction_10.getValues()[3]);
		this.channel(PredictorChannelId.PREDICT10_04H).setNextValue(hourlyPrediction_10.getValues()[4]);
		this.channel(PredictorChannelId.PREDICT10_05H).setNextValue(hourlyPrediction_10.getValues()[5]);
		this.channel(PredictorChannelId.PREDICT10_06H).setNextValue(hourlyPrediction_10.getValues()[6]);
		this.channel(PredictorChannelId.PREDICT10_07H).setNextValue(hourlyPrediction_10.getValues()[7]);
		this.channel(PredictorChannelId.PREDICT10_08H).setNextValue(hourlyPrediction_10.getValues()[8]);
		this.channel(PredictorChannelId.PREDICT10_09H).setNextValue(hourlyPrediction_10.getValues()[9]);
		this.channel(PredictorChannelId.PREDICT10_10H).setNextValue(hourlyPrediction_10.getValues()[10]);
		this.channel(PredictorChannelId.PREDICT10_11H).setNextValue(hourlyPrediction_10.getValues()[11]);
		this.channel(PredictorChannelId.PREDICT10_12H).setNextValue(hourlyPrediction_10.getValues()[12]);
		
		HourlyPrediction hourlyPrediction_90 = this.get24hPrediction_90();
		this.channel(PredictorChannelId.PREDICT90_00H).setNextValue(hourlyPrediction_90.getValues()[0]);
		this.channel(PredictorChannelId.PREDICT90_01H).setNextValue(hourlyPrediction_90.getValues()[1]);
		this.channel(PredictorChannelId.PREDICT90_02H).setNextValue(hourlyPrediction_90.getValues()[2]);
		this.channel(PredictorChannelId.PREDICT90_03H).setNextValue(hourlyPrediction_90.getValues()[3]);
		this.channel(PredictorChannelId.PREDICT90_04H).setNextValue(hourlyPrediction_90.getValues()[4]);
		this.channel(PredictorChannelId.PREDICT90_05H).setNextValue(hourlyPrediction_90.getValues()[5]);
		this.channel(PredictorChannelId.PREDICT90_06H).setNextValue(hourlyPrediction_90.getValues()[6]);
		this.channel(PredictorChannelId.PREDICT90_07H).setNextValue(hourlyPrediction_90.getValues()[7]);
		this.channel(PredictorChannelId.PREDICT90_08H).setNextValue(hourlyPrediction_90.getValues()[8]);
		this.channel(PredictorChannelId.PREDICT90_09H).setNextValue(hourlyPrediction_90.getValues()[9]);
		this.channel(PredictorChannelId.PREDICT90_10H).setNextValue(hourlyPrediction_90.getValues()[10]);
		this.channel(PredictorChannelId.PREDICT90_11H).setNextValue(hourlyPrediction_90.getValues()[11]);
		this.channel(PredictorChannelId.PREDICT90_12H).setNextValue(hourlyPrediction_90.getValues()[12]);
		

	}

	@Override
	public HourlyPrediction get24hPrediction() {
		Integer[] values = new Integer[24];
		int i = Math.max(0, 24 - this.hourlySolarData.size());

		for (Entry<LocalDateTime, Integer> entry : this.hourlySolarData.entrySet()) {
			values[i++] = entry.getValue();
		}
		LocalDateTime currentHour = LocalDateTime.now().withNano(0).withMinute(0).withSecond(0);

		HourlyPrediction hourlyPrediction = new HourlyPrediction(values, currentHour);
		return hourlyPrediction;
	}
	
	private HourlyPrediction get24hPrediction_10() {
		Integer[] values = new Integer[24];
		int i = Math.max(0, 24 - this.hourlySolarData_10.size());

		for (Entry<LocalDateTime, Integer> entry : this.hourlySolarData_10.entrySet()) {
			values[i++] = entry.getValue();
		}
		LocalDateTime currentHour = LocalDateTime.now().withNano(0).withMinute(0).withSecond(0);

		HourlyPrediction hourlyPrediction = new HourlyPrediction(values, currentHour);
		return hourlyPrediction;
	}
	
	private HourlyPrediction get24hPrediction_90() {
		Integer[] values = new Integer[24];
		int i = Math.max(0, 24 - this.hourlySolarData_90.size());

		for (Entry<LocalDateTime, Integer> entry : this.hourlySolarData_90.entrySet()) {
			values[i++] = entry.getValue();
		}
		LocalDateTime currentHour = LocalDateTime.now().withNano(0).withMinute(0).withSecond(0);

		HourlyPrediction hourlyPrediction = new HourlyPrediction(values, currentHour);
		return hourlyPrediction;
	}
}
