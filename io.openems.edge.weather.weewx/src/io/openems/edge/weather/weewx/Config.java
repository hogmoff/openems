package io.openems.edge.weather.weewx;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Weather Weewx", //
		description = "Implements a weather station over Weewx Device")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "weewx0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Weatherstation", description = "Type of weatherstation connected to Weewx device.")
	WeewxWeatherStation.WeatherstationType weatherstation() default WeewxWeatherStation.WeatherstationType.DNT_WEATHERSTATION_PRO;
	
	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Weewx device.")
	String ip() default "192.168.1.100";
	
	@AttributeDefinition(name = "Port", description = "The Port of the Weewx device.")
	String port() default "80";
	
	@AttributeDefinition(name = "Path", description = "The Path to the Weewx statusfile.")
	String path() default "/weewx";
	
	@AttributeDefinition(name = "Json-File", description = "The JSON-File of the Weewx device.")
	String file() default "weewx.json";

	String webconsole_configurationFactory_nameHint() default "io.openems.edge.weather.weewx [{id}]";

}