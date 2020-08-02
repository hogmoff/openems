package io.openems.edge.predictor.solarradiationmodel.production;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Predictor Production Solarradiation-Model", //
		description = "Predicts the Production using the Solcast Forecast.")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "predictorSolarradiation0";
	
	@AttributeDefinition(name = "Latitude", description = "Latitude for Forecast Position")
	String lat() default "49.545985";
	
	@AttributeDefinition(name = "Longitude", description = "Longitude for Forecast Position")
	String lon() default "9.735270";
	
	@AttributeDefinition(name = "API Key", description = "API-Key for Solcast")
	String key() default "I_sU5ZEX-9l5Jv0ItQrmfvLYUUOhQrSl";
	
	@AttributeDefinition(name = "Resource-ID", description = "Resource-ID for Solcast")
	String resource_id() default "6ed0-663f-860f-969c";
	
	@AttributeDefinition(name = "API-requests are limited?", description = "API-Requests are limited and time limit is enabled")
	boolean limitedAPI() default true;
	
	@AttributeDefinition(name = "Starttime", description = "Allowed Starttime for API-Requests, will be ignored if disabled")
	String starttime() default "06:00";
	
	@AttributeDefinition(name = "Endtime", description = "Allowed Endtime for API-Requests, will be ignored if disabled")
	String endtime() default "16:00";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	String webconsole_configurationFactory_nameHint() default "Predictor Production Solarradiation-Model [{id}]";
}
