package io.openems.edge.thermometer.openweather;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Openweather-Model", //
		description = "Get weather data using the Openweather-API")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "weather0";
	
	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Latitude", description = "Latitude for Forecast Position")
	String lat() default "52.516273";
	
	@AttributeDefinition(name = "Longitude", description = "Longitude for Forecast Position")
	String lon() default "13.381179";
	
	@AttributeDefinition(name = "API Key", description = "API-Key for Openweather")
	String key() default "";
	
	@AttributeDefinition(name = "read every x-cycle", description = "Read values every x cycles")
	int cycles() default 10;

	String webconsole_configurationFactory_nameHint() default "Weather data for Openweather [{id}]";
}