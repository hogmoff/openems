package io.openems.edge.summary;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "io.openems.edge.summary", //
		description = "")
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

	String webconsole_configurationFactory_nameHint() default "Summary [{id}]";

}