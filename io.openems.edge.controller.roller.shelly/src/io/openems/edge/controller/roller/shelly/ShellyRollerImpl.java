package io.openems.edge.controller.roller.shelly;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalTime;
import java.util.Objects;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.JsonUtils;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Roller.Shelly", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE //
		} //
)
public class ShellyRollerImpl extends AbstractOpenemsComponent implements ShellyRoller, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(ShellyRollerImpl.class);

	@Reference
	protected ComponentManager componentManager;
	
	private Config config;

	private String[] ipAddresses = null;
	private int openPos = 0;
	private int closePos = 0;
	private double[] duration = null;
	private JsonArray jsonStatus = null;
	private boolean summerMode = false;
	private boolean debugMode = false;
	private String httpAlmanac = "";
	private openMode almanacMode = null;
	private LocalTime openTime = null;
	private LocalTime closeTime = null;
	private int minTempForSummerMode1h;
	private int minTempForSummerModeToday;
	private String predictorChannelAddress;
	private PredictorManager predictorManager;

	public ShellyRollerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ShellyRoller.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (config.durationTime().length != config.ipAddresses().length) {
			throw new OpenemsException("ipAddresses and duration have to be the same length!");
		}	
		this.config = config;
		this.debugMode = config.debugMode();
		this.summerMode = config.summerMode();
		this.ipAddresses = config.ipAddresses();
		this.openPos = config.openPos();
		this.closePos = config.closePos();
		this.httpAlmanac = config.httpAlmanac();
		this.almanacMode = config.almanacMode();
		
		this.duration = new double[config.durationTime().length];
		for (int i=0; i<config.durationTime().length; i++) {
			this.duration[i] = Double.valueOf(config.durationTime()[i]);
		}
		this.predictorChannelAddress = this.config.predictorChannelAddress();
		this.minTempForSummerMode1h = this.config.minTempForSummerMode1h();
		this.minTempForSummerModeToday = this.config.minTempForSummerModeToday();
		
		this.predictorManager = this.componentManager.getComponent("_predictorManager");
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}
	
	/**
	 * The current state in the State Machine
	 */
	private State state = State.UNDEFINED;
	
	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:
			try {
				this.getOpenCloseTime();
				this.stateMachine();
			} catch (OpenemsNamedException e) {
				this.debugLog(e.getMessage());
			}
			break;
		case EdgeEventConstants.TOPIC_CYCLE_EXECUTE_WRITE:
			try {
				this.executeWrite(this.getOpenRollerChannel(), 0);
			} catch (OpenemsNamedException e) {
				this.debugLog(e.getMessage());
			}			
			break;
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
		channel.setNextValue(writeValue);
	}
	
	private BooleanWriteChannel getOpenRollerChannel() {
		return this.channel(ShellyRoller.ChannelId.OPENROLLER);
	}
	
	private void getOpenCloseTime() throws OpenemsNamedException {
		if (this.almanacMode != openMode.EXTERNAL) {
			String result = this.sendRequest_String(this.httpAlmanac, "GET");		
			try {
				String sunrise = "";
				String sunset = "";
				if (this.almanacMode == openMode.SUNRISE_SUNSET || this.almanacMode == openMode.AUTOMATIC_SUNRISE_SUNSET) {
					int indexSunrise = result.indexOf("<i class=\"wi wi-sunrise mr-1\" style=\"opacity: .75\"></i> ") + 56;
					int indexSunset= result.indexOf("<i class=\"wi wi-sunset mr-1\" style=\"opacity: .75\"></i> ") + 55;
					sunrise = result.substring(indexSunrise, indexSunrise + 8);
					sunset = result.substring(indexSunset, indexSunset + 8);
				}
				else if (this.almanacMode == openMode.TWILIGHT || this.almanacMode == openMode.AUTOMATIC_TWILIGHT) {
					int indexSunrise = result.indexOf("\"Start civil twilight\">") + 23;
					int indexSunset= result.indexOf("\"End civil twilight\">") + 21;
					sunrise = result.substring(indexSunrise, indexSunrise + 8);
					sunset = result.substring(indexSunset, indexSunset + 8);
				}
				this.openTime = LocalTime.parse(sunrise);
				this.closeTime = LocalTime.parse(sunset);	
				LocalTime now = LocalTime.now();
				Integer maxTemp1h = null;
				Integer maxTempToday = null;
				
				if (now.isBefore(this.openTime) && now.isBefore(this.closeTime)) {
					getOpenRollerChannel().setNextValue(false);
				}
				else if (now.isAfter(this.openTime) && now.isBefore(this.closeTime)) {
					if (this.almanacMode == openMode.AUTOMATIC_SUNRISE_SUNSET || this.almanacMode == openMode.AUTOMATIC_TWILIGHT) {
						summerModeResult r = setAutoSummerMode(now, this.closeTime);
						this.summerMode = r.summerMode;
						maxTemp1h = r.maxTemp1h;
						maxTempToday = r.maxTempToday;
						getOpenRollerChannel().setNextValue(true);						
					}
					else {
						getOpenRollerChannel().setNextValue(true);
					}
				}
				else if (now.isAfter(this.openTime) && now.isAfter(this.closeTime)) {
					getOpenRollerChannel().setNextValue(false);
				}		
				this.debugLog("OPENROLLER = " + getOpenRollerChannel().getNextValue().toString()
						+ " | summerMode = " + this.summerMode + " | predict max Temp 1h = " + maxTemp1h + "°C"
						+ " | predict max Temp Today = " + maxTempToday + "°C"
						+ " | openTime: " + sunrise + " | closeTime: " + sunset);
				
				
			} catch (Exception e) {
				this.openTime = null;
				this.closeTime = null;
			}			
		} 
			
			
	}
	
	private summerModeResult setAutoSummerMode(LocalTime now, LocalTime closeTime) throws OpenemsNamedException {
		ChannelAddress c = ChannelAddress.fromString(this.predictorChannelAddress);
		Prediction24Hours weatherPredict = this.predictorManager.get24HoursPrediction(c);
		boolean weatherPredictOk = true;	
		summerModeResult r = new summerModeResult(Integer.MIN_VALUE, Integer.MIN_VALUE, false);
		Integer[] weather = weatherPredict.getValues();
		LocalTime time = now;
		LocalTime time1h = time.plusHours(1);
		for (int i=0; i<weather.length; i++) {
			if (weather[i] == null) {
				weatherPredictOk = false;				
				break;
			}
			if ((time.isBefore(time1h) || time.equals(time1h)) && weather[i] > r.maxTemp1h) {
				r.maxTemp1h = weather[i];
			}
			if ((time.isBefore(closeTime) || time.equals(closeTime)) && weather[i] > r.maxTempToday) {
				r.maxTempToday = weather[i];
			}
			if (time.isAfter(closeTime)) {
				break;
			}
			time = time.plusMinutes(15);
		}
		
		if (weatherPredictOk) {
			if (r.maxTemp1h >= this.minTempForSummerMode1h 
					|| r.maxTempToday >= this.minTempForSummerModeToday) {
				r.summerMode = true;
				return r;
			}
			else {
				r.summerMode = false;
				r.maxTemp1h = null;
				r.maxTempToday = null;
				return r;
			}
		}
		else {
			// switch to sunrise_sunset or twilight mode
			r.summerMode = this.config.summerMode();
			return r;
		}
	}

	private void stateMachine() throws OpenemsNamedException {
		/*
		 * Check if all parameters are available
		 */
		
		String value = this.channel(ShellyRoller.ChannelId.OPENROLLER).getNextValue().toString();
				
		boolean openRoller = false;
		if (value.equals("true")) {
			openRoller = true;
		}
		else if (value.equals("UNDEFINED")) {
			return;
		}
		 
		/*
		 * State Machine
		 */
		switch (this.state) {
		case UNDEFINED:
			if (openRoller) {				
				this.state = State.SET_RUNNING_UP;
			} else {				
				this.state = State.SET_RUNNING_DOWN;
			} 
			break;
			
		case SET_RUNNING_UP:
			if (this.summerMode) {
				this.state = State.GO_TO_SLATE_POS;
			}
			else {
				this.setRollerUp();
				this.state = State.RUNNING_UP;
			}			
			break;
			
		case SET_RUNNING_DOWN:
			if (this.summerMode) {
				this.setRollerClose();
				this.state = State.RUNNING_DOWN;
			}
			else {
				this.setRollerDown();
				this.state = State.RUNNING_DOWN;
			}									
			break;
		
		case RUNNING_DOWN:			
			rollerStatus statusDown = this.getRollerStatus(this.closePos);
			String text = "posReached=" + Boolean.toString(statusDown.posReached) 
			  + "|stateStop=" + Boolean.toString(statusDown.stateStop)
			  + "|overtemp="  + Boolean.toString(statusDown.overtemp) 
			  + "|safety_switch=" + Boolean.toString(statusDown.safety_switch);
			this.debugLog(text);
			if (statusDown.overtemp || statusDown.safety_switch) {
				this.stopRoller();
				this.state = State.ERROR;
			}
			else if (statusDown.posReached) {
				this.state = State.ROLLER_CLOSED;				
			}
			else if (statusDown.stateStop && this.summerMode) {
				this.state = State.ROLLER_CLOSED;				
			}
			break;
			
		case RUNNING_UP:
			rollerStatus statusUp = this.getRollerStatus(this.openPos);
			String textUp = "posReached=" + Boolean.toString(statusUp.posReached) 
			  + "|stateStop=" + Boolean.toString(statusUp.stateStop)
			  + "|overtemp="  + Boolean.toString(statusUp.overtemp) 
			  + "|safety_switch=" + Boolean.toString(statusUp.safety_switch);
			this.debugLog(textUp);
			if (statusUp.overtemp || statusUp.safety_switch) {
				this.stopRoller();
				this.state = State.ERROR;
			}
			else if (statusUp.posReached) {
				this.state = State.ROLLER_OPEN;
			}
			break;
			
		case GO_TO_SLATE_POS:
			this.setRollerClose();
			this.state = State.SET_SLATE_TO_OPENPOS;
			break;
			
		case SET_SLATE_TO_OPENPOS:
			rollerStatus statusSlatesPos = this.getRollerStatus(0);			
			if (statusSlatesPos.overtemp || statusSlatesPos.safety_switch) {
				this.stopRoller();
				this.state = State.ERROR;
			}
			else if (statusSlatesPos.stateStop) {
				this.openSlats();
				this.state = State.SLATE_TO_OPENPOS;
			}
			break;
			
		case SLATE_TO_OPENPOS:
			rollerStatus statusSlates = this.getRollerStatus(0);
			if (statusSlates.overtemp || statusSlates.safety_switch) {
				this.stopRoller();
				this.state = State.ERROR;
			}
			else if (statusSlates.stateStop) {
				this.state = State.ROLLER_CLOSED_WITH_OPEN_SLATS;
			}
			break;
			
		case ROLLER_OPEN:
			if (!openRoller) {
				this.state = State.SET_RUNNING_DOWN;
			}
			break;
			
		case ROLLER_CLOSED:
			if (openRoller) {
				this.state = State.SET_RUNNING_UP;
			}
			break;
			
		case ROLLER_CLOSED_WITH_OPEN_SLATS:
			if (!openRoller) {
				this.state = State.SET_RUNNING_DOWN;
			}
			break;
			
		case ERROR:
			this.jsonStatus = this.getStatus();
			this.logError(this.log, "Error with one or more roller:\n" + this.jsonStatus.toString());
			break;
		
		}
		this.debugLog("State: " + this.state.getName());
	}
	
	private void setRollerUp() throws OpenemsNamedException {
		for (int i=0; i < this.ipAddresses.length; i++) {
			String url = "http://" + this.ipAddresses[i] + "/roller/0?go=to_pos&roller_pos=" + Integer.toString(this.openPos);
			this.sendRequest(url, "GET");			
		}		
	}
	
	private void setRollerDown() throws OpenemsNamedException {
		for (int i=0; i < this.ipAddresses.length; i++) {
			String url = "http://" + this.ipAddresses[i] + "/roller/0?go=to_pos&roller_pos=" + Integer.toString(this.closePos);
			this.sendRequest(url, "GET");			
		}		
	}
	
	private void setRollerClose() throws OpenemsNamedException {
		for (int i=0; i < this.ipAddresses.length; i++) {
			String url = "http://" + this.ipAddresses[i] + "/roller/0?go=close";
			this.sendRequest(url, "GET");			
		}		
	}
	
	private JsonArray getStatus() throws OpenemsNamedException {
		JsonArray Status = new JsonArray();
		for (int i=0; i < this.ipAddresses.length; i++) {
			String url = "http://" + this.ipAddresses[i] + "/roller/0";
			Status.add(this.sendRequest(url, "GET"));			
		}	
		return Status;
	}
	
	private rollerStatus getRollerStatus(int targetPos) throws OpenemsNamedException {
		this.jsonStatus = getStatus();
		rollerStatus r = new rollerStatus();
		int posReached = 0;
		int stateStop = 0;
		for (int i=0; i < this.jsonStatus.size(); i++) {
			int pos = this.jsonStatus.get(i).getAsJsonObject().get("current_pos").getAsInt();
			String state =  this.jsonStatus.get(i).getAsJsonObject().get("state").getAsString();
			boolean overtemp =  this.jsonStatus.get(i).getAsJsonObject().get("overtemperature").getAsBoolean();
			boolean safety_switch =  this.jsonStatus.get(i).getAsJsonObject().get("safety_switch").getAsBoolean();
			if (pos == targetPos) {
				posReached++;
			}
			if (state.equals("stop")) {
				stateStop++;
			}
			if (overtemp) {
				r.overtemp = true;
			}
			if (safety_switch) {
				r.safety_switch = true;
			}
		}
		if (posReached == this.ipAddresses.length) {
			r.posReached = true;
		} 
		if (stateStop == this.ipAddresses.length) {
			r.stateStop = true;
		} 
		return r;
	}
	
	private void stopRoller() throws OpenemsNamedException {
		for (int i=0; i < this.ipAddresses.length; i++) {
			String url = "http://" + this.ipAddresses[i] + "/roller/0?go=stop";
			this.sendRequest(url, "GET");			
		}		
	}
	
	private void openSlats() throws OpenemsNamedException {
		for (int i=0; i < this.ipAddresses.length; i++) {
			String url = "http://" + this.ipAddresses[i] + "/roller/0?go=open&duration=" + Double.toString(this.duration[i]);
			this.sendRequest(url, "GET");			
		}		
	}
		
	   /**
	   * Sends a get or set request to the shelly API.
	   *
	   * @param endpoint the REST Api endpoint
	   * @return a JsonObject or JsonArray
	   * @throws OpenemsNamedException on error
	   */
	  private JsonObject sendRequest(String urlString, String requestMethod) 
	      throws OpenemsNamedException {
	    try {
	      URL url = new URL(urlString);
	      HttpURLConnection con = (HttpURLConnection) url.openConnection();
	      con.setRequestMethod(requestMethod);
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
	        "Error while reading from shelly API. Response code: " + status + ". " + body);
	  }
	} catch (OpenemsNamedException | IOException e) {
	  throw new OpenemsException(
	      "Unable to read from shelly API. " + e.getClass().getSimpleName() + ": " + e.getMessage() + " | Check calibration of roller");
	   }
	  }
	  
	  /**
	   * Sends a get or set request to the shelly API.
	   *
	   * @param endpoint the REST Api endpoint
	   * @return a String
	   * @throws OpenemsNamedException on error
	   */
	  private String sendRequest_String(String urlString, String requestMethod) 
	      throws OpenemsNamedException {
	    try {
	      URL url = new URL(urlString);
	      HttpURLConnection con = (HttpURLConnection) url.openConnection();
	      con.setRequestMethod(requestMethod);
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
	    return body;
	  } else {
	    throw new OpenemsException(
	        "Error while reading from shelly API. Response code: " + status + ". " + body);
	  }
	} catch (OpenemsNamedException | IOException e) {
	  throw new OpenemsException(
	      "Unable to read from shelly API. " + e.getClass().getSimpleName() + ": " + e.getMessage() + " | Check calibration of roller");
	   }
	  }
	
	private class rollerStatus{
		boolean posReached = false;
		boolean stateStop = false;
		boolean overtemp = false;
		boolean safety_switch = false;
	}
	
	private class summerModeResult {
	   public Integer maxTemp1h;
	   public Integer maxTempToday;
	   public boolean summerMode;
	
	   public summerModeResult(Integer maxTemp1h, Integer maxTempToday, boolean summerMode) {
	      this.maxTemp1h = maxTemp1h;
	      this.maxTempToday = maxTempToday;
	      this.summerMode = summerMode;
	   }
	}
	

  /**
  * Debug Log.
  * 
  *<p>Logging only if the debug mode is enabled
  * 
  * @param message text that should be logged
  */
  public void debugLog(String message) {
    if (this.debugMode) {
      this.logInfo(this.log, message);
    }
  }
}
