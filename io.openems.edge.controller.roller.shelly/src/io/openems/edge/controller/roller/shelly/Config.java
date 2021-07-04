package io.openems.edge.controller.roller.shelly;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.osgi.service.metatype.annotations.ObjectClassDefinition;

@ObjectClassDefinition(//
		name = "Controller Roller Shelly", //
		description = "")
public @interface Config {

	@AttributeDefinition(name = "Component-ID", description = "Unique ID of this Component")
	String id() default "rollerGroup0";

	@AttributeDefinition(name = "Alias", description = "Human-readable name of this Component; defaults to Component-ID")
	String alias() default "";

	@AttributeDefinition(name = "Is enabled?", description = "Is this Component enabled?")
	boolean enabled() default true;
	
	@AttributeDefinition(name = "Debug mode?", description = "Debug mode enabled?")
	boolean debugMode() default true;
	
	@AttributeDefinition(name = "IP-Addresses", description = "IP addresses of the Roller that should be controlled")
	String[] ipAddresses() default {"192.168.1.210","192.168.1.211","192.168.1.212"};
	
	@AttributeDefinition(name = "Position open", description = "Position Open in Percent (not for summer mode)")
	int openPos() default 99;
	
	@AttributeDefinition(name = "Position close", description = "Position Close in Percent (not for summer mode)")
	int closePos() default 1;
	
	@AttributeDefinition(name = "Summer mode?", description = "Summer mode enabled? If yes than only slats will be opened")
	boolean summerMode() default true;
	
	@AttributeDefinition(name = "Min Temperature Automatic 1h", description = "Min Temperature where slats are opened (=summerMode)")
	int minTempForSummerMode1h() default 15;
	
	@AttributeDefinition(name = "Min Temperature Automatic Today", description = "Min Temperature where slats are opened (=summerMode)")
	int minTempForSummerModeToday() default 20;
	
	@AttributeDefinition(name = "ChannelAdrdess Predict Temperature", description = "ChannelAddress for Predict Temperature in Automatic Mode")
	String predictorChannelAddress() default "weather0/Predict_Temperature";
	
	@AttributeDefinition(name = "Time to open slats", description = "Duration-time to open slats for every roller in seconds")
	String[] durationTime() default {"0.7", "0.7", "0.7"};
	
	@AttributeDefinition(name = "Use Almanac?", description = "Use Almanac to open/close roller")
	boolean useAlmanac() default true;
	
	@AttributeDefinition(name = "Controlmode", description = "Use Time for Sunrise/Sunset, Twilight or External Controller")
	ShellyRoller.openMode almanacMode() default ShellyRoller.openMode.EXTERNAL;
	
	@AttributeDefinition(name = "NeoWX-Almanac", description = "Link to NeoWX-Almanac site (optional)")
	String httpAlmanac() default "https:\\";

	String webconsole_configurationFactory_nameHint() default "Controller Shelly Roller [{id}]";

}