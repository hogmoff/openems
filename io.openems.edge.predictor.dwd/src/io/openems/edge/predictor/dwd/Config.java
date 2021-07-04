package io.openems.edge.predictor.dwd;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Predictor Weather DWD", //
		description = "Get weather prediction and data using the DWD-opendata")
@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "weather0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Debug mode enabled?", description = "Show debug messages?")
	boolean debugMode() default true;
	
	@AttributeDefinition(name = "Link to MOXMIX_L-kmz-File", description = "Link to opendata.dwd.de")
	String kmzFile() default "";
	
	@AttributeDefinition(name = "Channel-Addresses", description = "List of Channel-Addresses this Predictor is used for, e.g. '*/Predict_Temperature', '*/Predict_Clouds'")
	String[] channelAddresses() default { "*/Predict_Temperature", "*/Predict_Clouds" };

	String webconsole_configurationFactory_nameHint() default "Weather data for DWD [{id}]";

}