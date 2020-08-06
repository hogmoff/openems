package io.openems.edge.io.edimax.sp2101W;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "IO Edimax 2101W V2", //
		description = "Implements a switching socket with power meter")

@interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "out0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;

	@AttributeDefinition(name = "IP-Address", description = "The IP address of the Edimax device.")
	String ip() default "192.168.1.1";
	
	@AttributeDefinition(name = "Port", description = "The Port of the Edimax device.")
	String port() default "10000";
	
	@AttributeDefinition(name = "Realm", description = "The Realm-Name of the Edimax device.")
	String realm() default "SP2101W_V2";
	
	@AttributeDefinition(name = "User", description = "The Admin User of the Edimax device.")
	String user() default "admin";
	
	@AttributeDefinition(name = "Password", description = "The Password of the Edimax device.")
	String password() default "12345678";
	
	String webconsole_configurationFactory_nameHint() default "io.openems.edge.io.edimax_2101W_V2 [{id}]";

}
