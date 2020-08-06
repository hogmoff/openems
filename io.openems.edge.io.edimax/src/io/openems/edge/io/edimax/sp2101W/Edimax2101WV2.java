package io.openems.edge.io.edimax.sp2101W;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

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
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.io.api.DigitalOutput;
import io.openems.edge.meter.api.MeterType;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "edimax2101WV2", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
		} //
)
public class Edimax2101WV2 extends AbstractOpenemsComponent implements DigitalOutput, SymmetricMeter, OpenemsComponent, EventHandler {

	private Edimax2101WV2Api edimax2101W_V2_api = null;
	private final Logger log = LoggerFactory.getLogger(Edimax2101WV2.class);
	private final BooleanWriteChannel[] digitalOutputChannels;
	private final MeterType meterType = MeterType.CONSUMPTION_METERED;

	public Edimax2101WV2() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				DigitalOutput.ChannelId.values(), //
				EdimaxChannelID.values() //
		);
		this.digitalOutputChannels = new BooleanWriteChannel[] { //
				this.channel(EdimaxChannelID.RELAY_1) //
		};
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.edimax2101W_V2_api = new Edimax2101WV2Api(config.ip(), config.port(), config.realm(), config.user(), config.password());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public BooleanWriteChannel[] digitalOutputChannels() {
		return this.digitalOutputChannels;
	}
	
	@Override
	public MeterType getMeterType() {
		return meterType;
	}

	@Override
	public String debugLog() {
		StringBuilder b = new StringBuilder();
		String stateText;
		Optional<Boolean> valueOpt = this.getRelay1Channel().value().asOptional();
		if (valueOpt.isPresent()) {
			stateText = valueOpt.get() ? "ON" : "OFF";
		} else {
			stateText = "?";
		}		
		b.append(stateText);	
		b.append(" " + this.getActivePower().toString());
		return b.toString();
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			this.eventBeforeProcessImage();
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			this.eventExecuteWrite();
			break;
		}
	}

	private BooleanWriteChannel getRelay1Channel() {
		return this.channel(EdimaxChannelID.RELAY_1);
	}
	
	private void setCurrent(Integer current) {
		this.channel(EdimaxChannelID.CURRENT).setNextValue(current);
	}
	
	private void setActivePower(Integer power) {
		this.channel(EdimaxChannelID.ACTIVE_POWER).setNextValue(power);
	}

	private StateChannel getSlaveCommunicationFailedChannel() {
		return this.channel(EdimaxChannelID.SLAVE_COMMUNICATION_FAILED);
	}

	private void setSlaveCommunicationFailed(boolean value) {
		this.getSlaveCommunicationFailedChannel().setNextValue(value);
	}

	/**
	 * Execute on Cycle Event "Before Process Image".
	 * @throws IOException 
	 */
	private void eventBeforeProcessImage() {
		Boolean relay1ison;
		Integer relay1current;
		Integer relay1power;
		try {
			JsonObject json = this.edimax2101W_V2_api.getStatus();
			relay1ison = JsonUtils.getAsBoolean(json, "ison");
			relay1current = JsonUtils.getAsInt(json, "current");
			relay1power = JsonUtils.getAsInt(json, "power");

			this.setSlaveCommunicationFailed(false);

		} catch (OpenemsNamedException | IndexOutOfBoundsException e) {
			relay1ison = null;
			relay1current = null;
			relay1power = null;
			this.logError(this.log, "Unable to read from Edimax API: " + e.getMessage());
			this.setSlaveCommunicationFailed(true);
		}
		// set new values
		this.getRelay1Channel().setNextValue(relay1ison);
		this.setCurrent(relay1current);
		this.setActivePower(relay1power);
		
	}

	/**
	 * Execute on Cycle Event "Execute Write".
	 */
	private void eventExecuteWrite() {
		try {
			// write values
			this.executeWrite(this.getRelay1Channel(), 0);
			
			this.setSlaveCommunicationFailed(false);
		} catch (OpenemsNamedException e) {
			this.setSlaveCommunicationFailed(true);
		}
	}
	
	private void executeWrite(BooleanWriteChannel channel, int index) throws OpenemsNamedException {
		Boolean readValue = channel.value().get();
		Optional<Boolean> writeValue = channel.getNextWriteValueAndReset();		
		if (!writeValue.isPresent()) {
			// no write value
			return;
		}
		if (Objects.equals(readValue, writeValue.get())) {
			// read value = write value
			return;
		}
		
		this.edimax2101W_V2_api.setRelayTurn(writeValue.get());
	}


} 

