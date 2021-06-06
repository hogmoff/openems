package io.openems.edge.summary;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Summary of energyflow", //
		description = "Summary of energyflow for display in Live-UI")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "summary0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Debug Mode", description = "Activates the debug mode")
	boolean debugMode() default false;
	
	@AttributeDefinition(name = "IP-Address", description = "The IP address of the InfluxDB.")
	String ip() default "192.168.1.100";
	
	@AttributeDefinition(name = "Port", description = "The Port of the InfluxDB API.")
	String port() default "8086";
	
	@AttributeDefinition(name = "Refresh Cycles", description = "Refresh status every x cycle", required = true)
	int StatusAfterCycles() default 60;
	
	@AttributeDefinition(name = "Flux String", description = "Flux-string with variables for Dates: day, month, year, whole", required = true)
	String FluxApi() default "dProd = from(bucket: \"openems/autogen\")\n"
			+ "  |> range(start: day)  \n"
			+ "  |> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/ProductionActivePower\")\n"
			+ "  |> integral(unit: 1h, column: \"_value\")\n"
			+ "  |> map(fn: (r) => ({ _time: day, production: r._value}))\n"
			+ "dCons = from(bucket: \"openems/autogen\")\n"
			+ "  |> range(start: day)  \n"
			+ "  |> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/ConsumptionActivePower\")\n"
			+ "  |> integral(unit: 1h, column: \"_value\")\n"
			+ "  |> map(fn: (r) => ({ _time: day, consumption: r._value}))\n"
			+ "p = from(bucket: \"openems/autogen\")\n"
			+ "  |> range(start: day)  \n"
			+ "  |> filter(fn: (r) => r._measurement == \"data\" and r._field == \"_sum/GridActivePower\")\n"
			+ "  |> map(fn: (r) => ({ _time: r._time, sell: if r._value < 0 then r._value else 0, buy: if r._value >= 0 then r._value else 0 }))\n"
			+ "dSell = p\n"
			+ "  |> integral(unit: 1h, column: \"sell\")\n"
			+ "  |> map(fn: (r) => ({ _time: day, sell: r.sell})) \n"
			+ "dBuy = p\n"
			+ "  |> integral(unit: 1h, column: \"buy\")\n"
			+ "  |> map(fn: (r) => ({ _time: day, buy: r.buy}))\n"
			+ "mProd = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: month)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyProduction\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: month, production: r._value }))\n"
			+ "mCons = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: month)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyConsumption\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: month, consumption: r._value }))\n"
			+ "mSell = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: month)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailySell\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: month, sell: r._value }))\n"
			+ "mBuy = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: month)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyBuy\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: month, buy: r._value }))\n"
			+ "yProd = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: year)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyProduction\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: year, production: r._value }))\n"
			+ "yCons = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: year)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyConsumption\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: year, consumption: r._value }))\n"
			+ "ySell = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: year)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailySell\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: year, sell: r._value }))\n"
			+ "yBuy = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: year)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyBuy\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: year, buy: r._value }))\n"
			+ "aProd = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: whole)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyProduction\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: whole, production: r._value }))\n"
			+ "aCons = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: whole)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyConsumption\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: whole, consumption: r._value }))\n"
			+ "aSell = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: whole)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailySell\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: whole, sell: r._value }))\n"
			+ "aBuy = from(bucket: \"auswertung/autogen\")\n"
			+ "  |> range(start: whole)\n"
			+ "  |> filter(fn: (r) => r._measurement == \"dailyBuy\")\n"
			+ "  |> sum()\n"
			+ "  |> map(fn: (r) => ({ _time: whole, buy: r._value }))\n"
			+ "union(tables: [dProd, dCons, dSell, dBuy, mProd, mCons, mSell, mBuy, yProd, yCons, ySell, yBuy, aProd, aCons, aSell, aBuy])";

	String webconsole_configurationFactory_nameHint() default "Summary [{id}]";

}