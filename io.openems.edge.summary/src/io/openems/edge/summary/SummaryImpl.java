package io.openems.edge.summary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

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
	private Future<Object> futureResult;   
	private Object API_Result = null;
	int cycle = 0;
	int executeEveryCycle = 0;
	public static String url = "";
	public static String query = "";
	String Today = "";
	String StartMonth = "";
	String StartYear = "";
	String SumYear = "";

	public SummaryImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Summary.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		url = "http://" + config.ip() + ":" + config.port() + "/api/v2/query";
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
				Today = LocalDateTime.now().format(formatter);
				StartMonth = LocalDateTime.now().getYear() + "-" + String.format("%02d", LocalDateTime.now().getMonthValue()) + "-01"; 
				StartYear = LocalDateTime.now().getYear() + "-01-01"; 
				SumYear = "2000-01-01";
				
				query = "day=" + Today + "\nmonth=" + StartMonth + "\nyear=" + StartYear + "\nwhole=" + SumYear +"\n" + config.FluxApi();						
				
				try {
					futureResult = getObjectAsync();
					API_Result = futureResult.get(3000, TimeUnit.MILLISECONDS);
				} catch (InterruptedException e) {
					throw new OpenemsException(e.getMessage());
				} catch (ExecutionException e) {
					throw new OpenemsException(e.getMessage());
				} catch (TimeoutException e) {
					throw new OpenemsException(e.getMessage());
				}
				
			}
			
			if (API_Result != null && futureResult.isDone()) {
	
				int dailyProduction = 0;
				int dailyConsumption = 0;
				int dailySell = 0;
				int dailyBuy = 0;	
				String[] arr = (String[]) API_Result;
				if (arr != null && arr.length > 4) {
					String[] col = arr[3].split(",");
					int indexTime = Arrays.asList(col).indexOf("_time");
					int indexProd = Arrays.asList(col).indexOf("production");
					int indexCons = Arrays.asList(col).indexOf("consumption");
					int indexSell = Arrays.asList(col).indexOf("sell");
					int indexBuy = Arrays.asList(col).indexOf("buy");
									
					// First find daily values
					for (int i=4; i < arr.length; i++) {
						String[] values = arr[i].split(",");
						if (values[indexTime].startsWith(Today)) {
							if (values.length > indexProd && !values[indexProd].isEmpty()) {
								dailyProduction = (int)Double.parseDouble(values[indexProd]);	
								this.channel(Summary.ChannelId.DAILY_PRODUCTION).setNextValue(dailyProduction);
							}
							if (values.length > indexCons && !values[indexCons].isEmpty()) {
								dailyConsumption = (int)Double.parseDouble(values[indexCons]);		
								this.channel(Summary.ChannelId.DAILY_CONSUMPTION).setNextValue(dailyConsumption);
							}
							if (values.length > indexSell && !values[indexSell].isEmpty()) {
								dailySell = Math.abs((int)Double.parseDouble(values[indexSell]));	
								this.channel(Summary.ChannelId.DAILY_SELL).setNextValue(dailySell);
							}
							if (values.length > indexBuy && !values[indexBuy].isEmpty()) {
								dailyBuy = (int)Double.parseDouble(values[indexBuy]);
								this.channel(Summary.ChannelId.DAILY_BUY).setNextValue(dailyBuy);
							}
						}
					}
					
					boolean MonthlyValue = false;
					boolean YearlyValue = false;
					
					// Find other values
					for (int i=4; i < arr.length; i++) {
						String[] values = arr[i].split(",");
						if (values.length > indexProd && !values[indexProd].isEmpty()) {
							if (values[indexTime].startsWith(StartMonth)) {
								int Production = (int)Double.parseDouble(values[indexProd]) + dailyProduction;	
								this.channel(Summary.ChannelId.MONTHLY_PRODUCTION).setNextValue(Production);
								MonthlyValue = true;
							}		
							else if (values[indexTime].startsWith(StartYear)) {
								int Production = (int)Double.parseDouble(values[indexProd]) + dailyProduction;	
								this.channel(Summary.ChannelId.YEARLY_PRODUCTION).setNextValue(Production);
								YearlyValue = true;
							}
							else if (values[indexTime].startsWith(SumYear)) {
								int Production = (int)Double.parseDouble(values[indexProd]) + dailyProduction;	
								this.channel(Summary.ChannelId.SUM_PRODUCTION).setNextValue(Production);
							}
						}
						else if (values.length > indexCons && !values[indexCons].isEmpty()) {
							if (values[indexTime].startsWith(StartMonth)) {
								int Consumption = (int)Double.parseDouble(values[indexCons]) + dailyConsumption;	
								this.channel(Summary.ChannelId.MONTHLY_CONSUMPTION).setNextValue(Consumption);
								MonthlyValue = true;
							}		
							else if (values[indexTime].startsWith(StartYear)) {
								int Consumption = (int)Double.parseDouble(values[indexCons]) + dailyConsumption;	
								this.channel(Summary.ChannelId.YEARLY_CONSUMPTION).setNextValue(Consumption);
								YearlyValue = true;
							}
							else if (values[indexTime].startsWith(SumYear)) {
								int Consumption = (int)Double.parseDouble(values[indexCons]) + dailyConsumption;	
								this.channel(Summary.ChannelId.SUM_CONSUMPTION).setNextValue(Consumption);
							}
						}
						else if (values.length > indexSell && !values[indexSell].isEmpty()) {
							if (values[indexTime].startsWith(StartMonth)) {
								int Sell = Math.abs((int)Double.parseDouble(values[indexSell])) + dailySell;	
								this.channel(Summary.ChannelId.MONTHLY_SELL).setNextValue(Sell);
								MonthlyValue = true;
							}		
							else if (values[indexTime].startsWith(StartYear)) {
								int Sell = Math.abs((int)Double.parseDouble(values[indexSell])) + dailySell;	
								this.channel(Summary.ChannelId.YEARLY_SELL).setNextValue(Sell);
								YearlyValue = true;
							}
							else if (values[indexTime].startsWith(SumYear)) {
								int Sell = Math.abs((int)Double.parseDouble(values[indexSell])) + dailySell;	
								this.channel(Summary.ChannelId.SUM_SELL).setNextValue(Sell);
							}
						}
						else if (values.length > indexBuy && values[indexBuy].isEmpty()) {
							if (values[indexTime].startsWith(StartMonth)) {
								int Buy = (int)Double.parseDouble(values[indexBuy]) + dailyBuy;	
								this.channel(Summary.ChannelId.MONTHLY_BUY).setNextValue(Buy);
								MonthlyValue = true;
							}		
							else if (values[indexTime].startsWith(StartYear)) {
								int Buy = (int)Double.parseDouble(values[indexBuy]) + dailyBuy;	
								this.channel(Summary.ChannelId.YEARLY_BUY).setNextValue(Buy);
								YearlyValue = true;
							}
							else if (values[indexTime].startsWith(SumYear)) {
								int Buy = (int)Double.parseDouble(values[indexBuy]) + dailyBuy;	
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
					API_Result = null;
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
	
	/* Sends a asynchron POST request to the Influxdb API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a StringArray as Object
	 */
	public static Future<Object> getObjectAsync() {
	    return CompletableFuture.supplyAsync(() -> doHttpCall());
	}

	static Object doHttpCall() {
	    try {
	        HttpURLConnection urlConnection = 
	            (HttpURLConnection) new URL(url).openConnection();
	            urlConnection.setDoOutput(true);
	            urlConnection.setRequestMethod("POST");
	            urlConnection.addRequestProperty("Accept", "application/csv");
	            urlConnection.addRequestProperty("Content-Type", "application/vnd.flux");
	            urlConnection.setRequestProperty("Content-Length", Integer.toString(query.length()));
	            urlConnection.getOutputStream().write(query.getBytes("UTF8"));
	        
	        try (BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()))) {
	        	// Read HTTP response
				StringBuilder content = new StringBuilder();
				String line;
				while ((line = in.readLine()) != null) {
					content.append(line);
					content.append(System.lineSeparator());
				}
				String body = content.toString();
	            return body.split("\n");
	        }
	    } catch (IOException e ) {
	        throw new RuntimeException(e);
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
