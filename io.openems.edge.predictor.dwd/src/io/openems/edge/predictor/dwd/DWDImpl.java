package io.openems.edge.predictor.dwd;

import java.io.IOException;
import java.io.InputStream;
import java.io.BufferedInputStream;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;
import java.util.TreeMap;
import java.util.Map.Entry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.events.XMLEvent;

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
		name = "Predictor.DWD", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class DWDImpl extends AbstractPredictor24Hours implements Predictor24Hours, OpenemsComponent, EventHandler {

	private final Logger log = LoggerFactory.getLogger(DWDImpl.class);
	private boolean executed;	
	private String File;
	List<LocalDateTime> TimeSteps = new ArrayList<>();
	List<Integer> Temperature = new ArrayList<>();
	List<Integer> Clouds = new ArrayList<>();
	LocalDateTime prevHour = LocalDateTime.now();
	private TreeMap<LocalDateTime, Integer> hourlyTemp = new TreeMap<LocalDateTime, Integer>();
	private TreeMap<LocalDateTime, Integer> hourlyClouds = new TreeMap<LocalDateTime, Integer>();
	
	@Reference
	private ComponentManager componentManager;

	public DWDImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				DWD.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled(), config.channelAddresses());
		this.File = config.kmzFile();
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
				if (i >= 96) {
					break;
				}
			}
		}
		else if (channelAddress.getChannelId().equals("Predict_Clouds")) {
			int i = Math.max(0, 24 - this.hourlyClouds.size());
			for (Entry<LocalDateTime, Integer> entry : this.hourlyClouds.entrySet()) {
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
				result[i++] = entry.getValue();
				if (i >= 96) {
					break;
				}
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
				this.channel(DWD.ChannelId.TEMPERATURE).setNextValue(hourlyTemp.firstEntry().getValue());
				this.channel(DWD.ChannelId.CLOUDS).setNextValue(hourlyClouds.firstEntry().getValue());
				this.channel(DWD.ChannelId.UNABLE_TO_PREDICT).setNextValue(false);				
			} catch (OpenemsNamedException e) {
				this.logError(this.log, e.getMessage());
				this.channel(DWD.ChannelId.UNABLE_TO_PREDICT).setNextValue(true);
			}
		}
	}
	
	/*
	 * This method gets the value from the Channel every one hour and updates the TreeMap.
	 */
	private void calculatePrediction() throws OpenemsNamedException {
		LocalDateTime currentHour = LocalDateTime.now(this.componentManager.getClock()).withNano(0).withMinute(0).withSecond(0);

		if (!executed) {
			// First time execution - Map is still empty	
			this.getPrediction();			
			this.prevHour = currentHour;
			this.executed = true;
			
		} else if (currentHour.isAfter(this.prevHour)) {
			// hour changed -> get new forecast
			this.getPrediction();
			this.prevHour = currentHour;
		} else {
			// hour did not change -> return			
			return;
		}
		
		if (this.TimeSteps.size() > 0 && this.Temperature.size() > 0 && this.Clouds.size() > 0) {
			hourlyTemp.clear();
			hourlyClouds.clear();
			for (Integer i = 0; i < this.TimeSteps.size(); i++) {			
				hourlyTemp.put(this.TimeSteps.get(i), this.Temperature.get(i));
				hourlyClouds.put(this.TimeSteps.get(i), this.Clouds.get(i));
			}	
			this.channel(DWD.ChannelId.PREDICT_ENABLED).setNextValue(true);
			
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
			this.channel(DWD.ChannelId.PREDICT_ENABLED).setNextValue(false);	
			
		}		
	}
	
	private void getPrediction() {
		this.TimeSteps.clear();
		this.Temperature.clear();
		this.Clouds.clear();
	    try {	    	
	    	URL url = new URL(this.File); 	
			InputStream in = new BufferedInputStream(url.openStream(), 524288);
			ZipInputStream stream = new ZipInputStream(in);						
			ZipEntry ze = stream.getNextEntry();
			XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(stream);			
		    String name = ze.getName();
		    if (name.endsWith(".kml")) {				  
			  while(reader.hasNext())
			  {
				  int event = reader.next();
			      if (event == XMLEvent.START_ELEMENT && "ForecastTimeSteps".equals(reader.getLocalName())) {
			    	  this.TimeSteps = this.readTimeSteps(reader);
			      }
			      // Temperature 2m over surface
			      else if (event == XMLEvent.START_ELEMENT && "Forecast".equals(reader.getLocalName())
			    		  && "TTT".equals(reader.getAttributeValue(0))) {
			    	  this.Temperature = this.readValues(reader, -273.15);
			      }
			      // Clouds
			      else if (event == XMLEvent.START_ELEMENT && "Forecast".equals(reader.getLocalName())
			    		  && "Neff".equals(reader.getAttributeValue(0))) {
			    	  this.Clouds = this.readValues(reader, 0.0);
			      }
			  }
		    }
		    else {
		    	this.logError(this.log, "No *.kml file found");
		    }
		    in.close();
			stream.close();                
	    } catch (XMLStreamException | IOException e) {
	    	this.logError(this.log, e.getMessage());
	    	this.TimeSteps.clear();
			this.Temperature.clear();
			this.Clouds.clear();
	    }
	}
	
	private List<LocalDateTime> readTimeSteps(XMLStreamReader reader) {
		List<LocalDateTime> timeSteps = new ArrayList<LocalDateTime>();
		try {
			while(reader.hasNext())
			  {
				  int event = reader.next();
				  if (XMLEvent.START_ELEMENT == event
				          && "TimeStep".equals(reader.getLocalName())) {
				      String str = reader.getElementText();
				      SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.000X");
				      formatter.setTimeZone(TimeZone.getDefault());				     
				      Date date = formatter.parse(str);
				      LocalDateTime ldate = LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault());
				      timeSteps.add(ldate);
				  }
				  else if (XMLEvent.END_ELEMENT == event) {
					  break;
				  }
			  }		
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return timeSteps;		
	}
	
	private List<Integer> readValues(XMLStreamReader reader, double Offset) {
		List<Integer> values = new ArrayList<Integer>();
		try {
			while(reader.hasNext())
			  {					
				  int event = reader.next();
				  
				  if (XMLEvent.START_ELEMENT == event
				          && "value".equals(reader.getLocalName())) {
					  String s = reader.getElementText();
					  String[] arr = s.trim().split("    ");
					  for (Integer i = 0; i < arr.length; i++) {
						  double v = Double.parseDouble(arr[i]) + Offset;						  
						  values.add((int)Math.round(v));
					  }					  
				  }
				  else if (XMLEvent.END_ELEMENT == event) {
					  break;
				  }
			  }		
		} catch (IllegalArgumentException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (XMLStreamException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return values;		
	}
	
	
	@Override
	public String debugLog() {
		return "Temperature: " + this.channel(DWD.ChannelId.TEMPERATURE).value().toString() + " " +
				" | Clouds: " +	this.channel(DWD.ChannelId.CLOUDS).value().toString();
	}

}
