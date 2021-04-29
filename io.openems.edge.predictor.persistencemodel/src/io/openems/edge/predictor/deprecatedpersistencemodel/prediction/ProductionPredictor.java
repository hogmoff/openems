package io.openems.edge.predictor.deprecatedpersistencemodel.prediction;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.predictor.api.hourly.ProductionHourlyPredictor;
import io.openems.edge.predictor.deprecatedpersistencemodel.AbstractPersistenceModelPredictor;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Predictor.Production.PersistenceModel", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE)

public class ProductionPredictor extends AbstractPersistenceModelPredictor
		implements ProductionHourlyPredictor, OpenemsComponent, EventHandler {

	@Reference
	protected ComponentManager componentManager;

	public ProductionPredictor() {
		super(OpenemsConstants.SUM_ID, Sum.ChannelId.PRODUCTION_ACTIVE_ENERGY);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		super.activate(context, config.id(), config.alias(), config.enabled());
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected ComponentManager getComponentManager() {
		return this.componentManager;
	}
}