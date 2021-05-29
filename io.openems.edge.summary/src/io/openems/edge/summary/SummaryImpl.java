package io.openems.edge.summary;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import com.google.gson.JsonObject;

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
				String dailyBody = "from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + LocalDateTime.now().format(formatter) + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/ProductionActivePower\")\n"
						  + "|> integral(unit: 1h, column: \"_value\")";	 

				String StartMonth = LocalDateTime.now().getYear() + "-" + String.format("%02d", LocalDateTime.now().getMonthValue()) + "-01"; 
				String weeklyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartMonth + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyProduction\" and r._field == \"integral\")\n"
						+ "  |> sum()";			
				
				String StartYear = LocalDateTime.now().getYear() + "-01-01"; 
				String yearlyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartYear + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyProduction\" and r._field == \"integral\")\n"
						+ "  |> sum()";	
				
				String sumBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: 2000-01-01)\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyProduction\" and r._field == \"integral\")\n"
						+ "  |> sum()";	
				
				JsonObject dailyProd = this.sendPostRequest(this.url, dailyBody);
				JsonObject monthlyProd = this.sendPostRequest(this.url, weeklyBody);
				JsonObject yearlyProd = this.sendPostRequest(this.url, yearlyBody);
				JsonObject sumProd = this.sendPostRequest(this.url, sumBody);
				
				int dailyProduction = dailyProd.getAsJsonObject().get("_value").getAsInt();
				this.channel(Summary.ChannelId.DAILY_PRODUCTION).setNextValue(dailyProduction);
				this.channel(Summary.ChannelId.MONTHLY_PRODUCTION).setNextValue(monthlyProd.getAsJsonObject().get("_value").getAsInt() + dailyProduction);
				this.channel(Summary.ChannelId.YEARLY_PRODUCTION).setNextValue(yearlyProd.getAsJsonObject().get("_value").getAsInt() + dailyProduction);
				this.channel(Summary.ChannelId.SUM_PRODUCTION).setNextValue(sumProd.getAsJsonObject().get("_value").getAsInt() + dailyProduction);
				
				dailyBody = "from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + LocalDateTime.now().format(formatter) + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/ConsumptionActivePower\")\n"
						  + "|> integral(unit: 1h, column: \"_value\")";	 

				weeklyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartMonth + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyConsumption\" and r._field == \"integral\")\n"
						+ "  |> sum()";			
				
				yearlyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartYear + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyConsumption\" and r._field == \"integral\")\n"
						+ "  |> sum()";	
				
				sumBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: 2000-01-01)\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyConsumption\" and r._field == \"integral\")\n"
						+ "  |> sum()";	
				
				JsonObject dailyCons = this.sendPostRequest(this.url, dailyBody);
				JsonObject monthlyCons = this.sendPostRequest(this.url, weeklyBody);
				JsonObject yearlyCons = this.sendPostRequest(this.url, yearlyBody);
				JsonObject sumCons = this.sendPostRequest(this.url, sumBody);
				
				int dailyConsumption = dailyCons.getAsJsonObject().get("_value").getAsInt();
				this.channel(Summary.ChannelId.DAILY_CONSUMPTION).setNextValue(dailyConsumption);
				this.channel(Summary.ChannelId.MONTHLY_CONSUMPTION).setNextValue(monthlyCons.getAsJsonObject().get("_value").getAsInt() + dailyConsumption);
				this.channel(Summary.ChannelId.YEARLY_CONSUMPTION).setNextValue(yearlyCons.getAsJsonObject().get("_value").getAsInt() + dailyConsumption);
				this.channel(Summary.ChannelId.SUM_CONSUMPTION).setNextValue(sumCons.getAsJsonObject().get("_value").getAsInt() + dailyConsumption);

				dailyBody = "from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + LocalDateTime.now().format(formatter) + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/GridActivePower\" and r._value < 0)\n"
						  + "|> integral(unit: 1h, column: \"_value\")";	 

				weeklyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartMonth + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailySell\" and r._field == \"abs\")\n"
						+ "  |> sum()";			
				
				yearlyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartYear + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailySell\" and r._field == \"abs\")\n"
						+ "  |> sum()";	
				
				sumBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: 2000-01-01)\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailySell\" and r._field == \"abs\")\n"
						+ "  |> sum()";	
				
				JsonObject dailySell = this.sendPostRequest(this.url, dailyBody);
				JsonObject monthlySell = this.sendPostRequest(this.url, weeklyBody);
				JsonObject yearlySell = this.sendPostRequest(this.url, yearlyBody);
				JsonObject sumSell = this.sendPostRequest(this.url, sumBody);
				
				int idailySell = Math.abs(dailySell.getAsJsonObject().get("_value").getAsInt());
				this.channel(Summary.ChannelId.DAILY_SELL).setNextValue(idailySell);
				this.channel(Summary.ChannelId.MONTHLY_SELL).setNextValue(Math.abs(monthlySell.getAsJsonObject().get("_value").getAsInt()) + idailySell);
				this.channel(Summary.ChannelId.YEARLY_SELL).setNextValue(Math.abs(yearlySell.getAsJsonObject().get("_value").getAsInt() + idailySell));
				this.channel(Summary.ChannelId.SUM_SELL).setNextValue(Math.abs(sumSell.getAsJsonObject().get("_value").getAsInt() + idailySell));
				
				dailyBody = "from(bucket: \"openems/autogen\")\n"
						  + "|> range(start: " + LocalDateTime.now().format(formatter) + ")\n"  
						  + "|> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/GridActivePower\" and r._value > 0)\n"
						  + "|> integral(unit: 1h, column: \"_value\")";	 

				weeklyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartMonth + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyBuy\" and r._field == \"abs\")\n"
						+ "  |> sum()";			
				
				yearlyBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: " + StartYear + ")\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyBuy\" and r._field == \"abs\")\n"
						+ "  |> sum()";	
				
				sumBody = "from(bucket: \"auswertung/autogen\")\n"
						+ "  |> range(start: 2000-01-01)\n"
						+ "  |> filter(fn: (r) => r._measurement == \"dailyBuy\" and r._field == \"abs\")\n"
						+ "  |> sum()";	
				
				JsonObject dailyBuy = this.sendPostRequest(this.url, dailyBody);
				JsonObject monthlyBuy = this.sendPostRequest(this.url, weeklyBody);
				JsonObject yearlyBuy = this.sendPostRequest(this.url, yearlyBody);
				JsonObject sumBuy = this.sendPostRequest(this.url, sumBody);
				
				int idailyBuy = dailyBuy.getAsJsonObject().get("_value").getAsInt();
				this.channel(Summary.ChannelId.DAILY_BUY).setNextValue(idailyBuy);
				this.channel(Summary.ChannelId.MONTHLY_BUY).setNextValue(monthlyBuy.getAsJsonObject().get("_value").getAsInt() + idailyBuy);
				this.channel(Summary.ChannelId.YEARLY_BUY).setNextValue(yearlyBuy.getAsJsonObject().get("_value").getAsInt() + idailyBuy);
				this.channel(Summary.ChannelId.SUM_BUY).setNextValue(sumBuy.getAsJsonObject().get("_value").getAsInt() + idailyBuy);
				
				this.cycle = 1;
			}
			else {
				this.cycle++;
			}
		}
		catch (OpenemsNamedException e)
		{
			
		}
	}
	
	/**
	 * Sends a POST request to the Influxdb API.
	 * 
	 * @param endpoint the REST Api endpoint
	 * @return a JsonObject
	 * @throws OpenemsNamedException on error
	 */
	private JsonObject sendPostRequest(String URL, String query) throws OpenemsNamedException {
		try {
			URL url = new URL(URL);
			HttpURLConnection con = (HttpURLConnection) url.openConnection();
			con.setDoOutput(true);
			con.setRequestMethod("POST");
			con.addRequestProperty("Accept", "application/csv");
			con.addRequestProperty("Content-Type", "application/vnd.flux");
			con.setRequestProperty("Content-Length", Integer.toString(query.length()));			
			con.getOutputStream().write(query.getBytes("UTF8"));
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
				// Parse csv-response to JSON
				JsonObject json = new JsonObject();	
				String[] arr = body.split("\n");
				if (arr.length > 4) {
					String[] col = arr[3].split(",");		
					for (int i=4; i<arr.length; i++) {
						String[] val = arr[i].split(",");			
						json.addProperty(col[col.length-1], Double.valueOf(val[val.length-1]));
					}				
				}
				return json;
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
