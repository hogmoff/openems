package io.openems.edge.summary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "io.openems.edge.summary", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = { //
				EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
		} //
)
public class SummaryImpl extends AbstractOpenemsComponent implements Summary, OpenemsComponent, EventHandler {

	private Config config = null;
	private final Logger log = LoggerFactory.getLogger(SummaryImpl.class);
	int cycle = 0;
	int executeEveryCycle = 0;
	String url = "";

	public SummaryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Summary.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.url = "http://" + config.ip() + ":" + config.port() + "/api/v2/query";
		this.executeEveryCycle = config.StatusAfterCycles();
		this.config = config;
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
			this.readSummaries();
			break;
		}
	}
	
	private void readSummaries() {
		try {
			// Execute every x-Cycle
			if (this.cycle == 0 || this.cycle % this.executeEveryCycle == 0) {
				DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
				String Today = LocalDateTime.now().format(formatter);
				String StartMonth = LocalDateTime.now().getYear() + "-" + String.format("%02d", LocalDateTime.now().getMonthValue()) + "-01"; 
				String StartYear = LocalDateTime.now().getYear() + "-01-01"; 
				String SumYear = "2000-01-01";
				
				String Body = "t1 = from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + Today + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/ProductionActivePower\")\n"
						  + "|> integral(unit: 1h, column: \"_value\")\n"
						  + "t2 = from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + Today + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/ConsumptionActivePower\")\n"
						  + "|> integral(unit: 1h, column: \"_value\")\n"
						  + "t3 = from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + Today + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/GridActivePower\" and r._value < 0)\n"
						  + "|> integral(unit: 1h, column: \"_value\")\n"
						  + "t4 = from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + Today + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/GridActivePower\" and r._value > 0)\n"
						  + "|> integral(unit: 1h, column: \"_value\")\n"
						  + "t5 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartMonth + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"dailyProduction\")\n"
						  + "|> sum()\n"
						  + "t6 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartMonth + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"dailyConsumption\")\n"
						  + "|> sum()\n"
						  + "t7 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartMonth + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"dailySell\")\n"
						  + "|> sum()\n"
						  + "t8 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartMonth + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"dailyBuy\")\n"
						  + "|> sum()\n"
						  + "t9 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartYear + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"dailyProduction\")\n"
						  + "|> sum()\n"
						  + "t10 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartYear + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"dailyConsumption\")\n"
						  + "|> sum()\n"
						  + "t11 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartYear + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"dailySell\")\n"
						  + "|> sum()\n"
						  + "t12 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + StartYear + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"dailyBuy\")\n"
						  + "|> sum()\n"
						  + "t13 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + SumYear + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"dailyProduction\")\n"
						  + "|> sum()\n"
						  + "t14 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + SumYear + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"dailyConsumption\")\n"
						  + "|> sum()\n"
						  + "t15 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + SumYear + ")\n"   
						  + "|> filter(fn: (r) => r._measurement == \"dailySell\")\n"
						  + "|> sum()\n"
						  + "t16 = from(bucket: \"auswertung/autogen\")\n"
						  + "|> range(start: " + SumYear + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"dailyBuy\")\n"
						  + "|> sum()\n"
						  + "union(tables: [t1, t2, t3, t4, t5, t6, t7, t8, t9, t10, t11, t12, t13, t14, t15, t16])";	
				
				int dailyProduction = 0;
				int dailyConsumption = 0;
				int dailySell = 0;
				int dailyBuy = 0;	
				String[] arr = this.sendPostRequest(this.url, Body);
				if (arr != null && arr.length > 4) {
					String[] col = arr[3].split(",");
					int indexStart = Arrays.asList(col).indexOf("_start");
					int indexField = Arrays.asList(col).indexOf("_field");
					int indexMeasurement = Arrays.asList(col).indexOf("_measurement");
					int indexValue = Arrays.asList(col).indexOf("_value");
									
					// First find daily values
					for (int i=4; i < arr.length; i++) {
						String[] values = arr[i].split(",");
						if (values[indexField].equals("_sum/ProductionActivePower") && values[indexStart].startsWith(Today)) {
							dailyProduction = (int)Double.parseDouble(values[indexValue]);	
							this.channel(Summary.ChannelId.DAILY_PRODUCTION).setNextValue(dailyProduction);
						}
						else if (values[indexField].equals("_sum/ConsumptionActivePower") && values[indexStart].startsWith(Today)) {
							dailyConsumption = (int)Double.parseDouble(values[indexValue]);		
							this.channel(Summary.ChannelId.DAILY_CONSUMPTION).setNextValue(dailyConsumption);
						}
						else if (values[indexField].equals("_sum/GridActivePower") && Double.parseDouble(values[indexValue]) < 0 && values[indexStart].startsWith(Today)) {
							dailySell = Math.abs((int)Double.parseDouble(values[indexValue]));	
							this.channel(Summary.ChannelId.DAILY_SELL).setNextValue(dailySell);
						}
						else if (values[indexField].equals("_sum/GridActivePower") && Double.parseDouble(values[indexValue]) > 0 && values[indexStart].startsWith(Today)) {
							dailyBuy = (int)Double.parseDouble(values[indexValue]);
							this.channel(Summary.ChannelId.DAILY_BUY).setNextValue(dailyBuy);
						}
					}
					
					boolean MonthlyValue = false;
					boolean YearlyValue = false;
					
					// Find other values
					for (int i=4; i < arr.length; i++) {
						String[] values = arr[i].split(",");
						if (values[indexMeasurement].equals("dailyProduction")) {
							if (values[indexStart].startsWith(StartMonth)) {
								int Production = (int)Double.parseDouble(values[indexValue]) + dailyProduction;	
								this.channel(Summary.ChannelId.MONTHLY_PRODUCTION).setNextValue(Production);
								MonthlyValue = true;
							}		
							else if (values[indexStart].startsWith(StartYear)) {
								int Production = (int)Double.parseDouble(values[indexValue]) + dailyProduction;	
								this.channel(Summary.ChannelId.YEARLY_PRODUCTION).setNextValue(Production);
								YearlyValue = true;
							}
							else if (values[indexStart].startsWith(SumYear)) {
								int Production = (int)Double.parseDouble(values[indexValue]) + dailyProduction;	
								this.channel(Summary.ChannelId.SUM_PRODUCTION).setNextValue(Production);
							}
						}
						else if (values[indexMeasurement].equals("dailyConsumption")) {
							if (values[indexStart].startsWith(StartMonth)) {
								int Consumption = (int)Double.parseDouble(values[indexValue]) + dailyConsumption;	
								this.channel(Summary.ChannelId.MONTHLY_CONSUMPTION).setNextValue(Consumption);
								MonthlyValue = true;
							}		
							else if (values[indexStart].startsWith(StartYear)) {
								int Consumption = (int)Double.parseDouble(values[indexValue]) + dailyConsumption;	
								this.channel(Summary.ChannelId.YEARLY_CONSUMPTION).setNextValue(Consumption);
								YearlyValue = true;
							}
							else if (values[indexStart].startsWith(SumYear)) {
								int Consumption = (int)Double.parseDouble(values[indexValue]) + dailyConsumption;	
								this.channel(Summary.ChannelId.SUM_CONSUMPTION).setNextValue(Consumption);
							}
						}
						else if (values[indexMeasurement].equals("dailySell")) {
							if (values[indexStart].startsWith(StartMonth)) {
								int Sell = Math.abs((int)Double.parseDouble(values[indexValue])) + dailySell;	
								this.channel(Summary.ChannelId.MONTHLY_SELL).setNextValue(Sell);
								MonthlyValue = true;
							}		
							else if (values[indexStart].startsWith(StartYear)) {
								int Sell = Math.abs((int)Double.parseDouble(values[indexValue])) + dailySell;	
								this.channel(Summary.ChannelId.YEARLY_SELL).setNextValue(Sell);
								YearlyValue = true;
							}
							else if (values[indexStart].startsWith(SumYear)) {
								int Sell = Math.abs((int)Double.parseDouble(values[indexValue])) + dailySell;	
								this.channel(Summary.ChannelId.SUM_SELL).setNextValue(Sell);
							}
						}
						else if (values[indexMeasurement].equals("dailyBuy")) {
							if (values[indexStart].startsWith(StartMonth)) {
								int Buy = (int)Double.parseDouble(values[indexValue]) + dailyBuy;	
								this.channel(Summary.ChannelId.MONTHLY_BUY).setNextValue(Buy);
								MonthlyValue = true;
							}		
							else if (values[indexStart].startsWith(StartYear)) {
								int Buy = (int)Double.parseDouble(values[indexValue]) + dailyBuy;	
								this.channel(Summary.ChannelId.YEARLY_BUY).setNextValue(Buy);
								YearlyValue = true;
							}
							else if (values[indexStart].startsWith(SumYear)) {
								int Buy = (int)Double.parseDouble(values[indexValue]) + dailyBuy;	
								this.channel(Summary.ChannelId.SUM_BUY).setNextValue(Buy);
							}
						}
					}
					
					if (!MonthlyValue) {
						this.channel(Summary.ChannelId.MONTHLY_PRODUCTION).setNextValue(dailyProduction);
						this.channel(Summary.ChannelId.MONTHLY_CONSUMPTION).setNextValue(dailyConsumption);
						this.channel(Summary.ChannelId.MONTHLY_SELL).setNextValue(dailySell);
						this.channel(Summary.ChannelId.MONTHLY_BUY).setNextValue(dailyBuy);
					}
					if (!YearlyValue) {
						this.channel(Summary.ChannelId.YEARLY_PRODUCTION).setNextValue(dailyProduction);
						this.channel(Summary.ChannelId.YEARLY_CONSUMPTION).setNextValue(dailyConsumption);
						this.channel(Summary.ChannelId.YEARLY_SELL).setNextValue(dailySell);
						this.channel(Summary.ChannelId.YEARLY_BUY).setNextValue(dailyBuy);
					}
				}
				
				this.cycle = 1;
			}
			else {
				this.cycle++;
			}
		}
		catch (OpenemsNamedException e)
		{
			this.logError(this.log, e.getMessage());
		}
	}
	
	/**
	 * Sends a POST request to the Influxdb API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a StringArray
	 * @throws OpenemsNamedException on error
	 */
	private String[] sendPostRequest(String URL, String query) throws OpenemsNamedException {
		try {
			URL url = new URL(URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.addRequestProperty("Accept", "application/csv");
			con.addRequestProperty("Content-Type", "application/vnd.flux");
			con.setRequestProperty("Content-Length", Integer.toString(query.length()));			
			con.getOutputStream().write(query.getBytes("UTF8"));
			con.setConnectTimeout(1000);
			con.setReadTimeout(1000);
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
				String[] arr = body.split("\n");				
				return arr;
			} else {
				throw new OpenemsException(
						"Error while reading from InfluxDB API. Response code: " + status + ". " + body);
			}
		} catch (OpenemsException | IOException e) {
			throw new OpenemsException(
					"Unable to read from InfluxDB API. Check IP-Adress, Port");
		}
	}

	@Override
	public String debugLog() {
		if (config.debugMode()) {
			return "DailyProd:" + this.channel(Summary.ChannelId.DAILY_PRODUCTION).getNextValue() +
					"|MontlyProd:" + this.channel(Summary.ChannelId.MONTHLY_PRODUCTION).getNextValue() +
					"|YearlyProd:" +  this.channel(Summary.ChannelId.YEARLY_PRODUCTION).getNextValue() +
					"|SumProd:" +  this.channel(Summary.ChannelId.SUM_PRODUCTION).getNextValue();
		}
		else {
			return "";
		}
	}
}
