package io.openems.edge.predictor.openweather;

import org.osgi.service.event.EventHandler;
import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface Openweather extends OpenemsComponent, EventHandler {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_PREDICT(Doc.of(Level.FAULT)),
		PREDICT_ENABLED(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_ONLY)),
		TEMPERATURE(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.DEGREE_CELSIUS)),
		CLOUDS(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY).unit(Unit.PERCENT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

}
