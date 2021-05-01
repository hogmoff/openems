package io.openems.edge.predictor.api.manager;

import java.util.concurrent.CompletableFuture;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.edge.common.user.User;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

public interface PredictorManager extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;
		
		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		public Doc doc() {
			return this.doc;
		}
	}
	
	public static final String METHOD = "get24HoursPrediction";

	/**
	 * Gets the {@link Prediction24Hours} by the best matching
	 * {@link Predictor24Hours} for the given {@link ChannelAddress}.
	 * 
	 * @param channelAddress the {@link ChannelAddress}
	 * @return the {@link Prediction24Hours} - all values null if no Predictor
	 *         matches the Channel-Address
	 */
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress);	
		
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request) throws OpenemsNamedException;
	
}
