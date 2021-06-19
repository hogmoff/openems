package io.openems.edge.predictor.api.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.openems.common.OpenemsConstants;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.jsonrpc.base.JsonrpcRequest;
import io.openems.common.jsonrpc.base.JsonrpcResponseSuccess;
import io.openems.common.types.ChannelAddress;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.User;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.predictor.api.oneday.Prediction24Hours;
import io.openems.edge.predictor.api.oneday.Predictor24Hours;

public class DummyPredictorManager extends AbstractOpenemsComponent implements PredictorManager, OpenemsComponent {

	private List<Predictor24Hours> predictors = new ArrayList<>();

	public DummyPredictorManager(Predictor24Hours... predictors) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				PredictorManager.ChannelId.values() //
		);
		for (Channel<?> channel : this.channels()) {
			channel.nextProcessImage();
		}
		for (Predictor24Hours predictor : predictors) {
			this.predictors.add(predictor);
		}
		super.activate(null, OpenemsConstants.PREDICTOR_MANAGER_ID, "", true);
	}

	public void addPredictor(Predictor24Hours predictor) {
		this.predictors.add(predictor);
	}

	@Override
	public Prediction24Hours get24HoursPrediction(ChannelAddress channelAddress) {
		for (Predictor24Hours predictor : this.predictors) {
			for (ChannelAddress pattern : predictor.getChannelAddresses()) {
				if (ChannelAddress.match(channelAddress, pattern) < 0) {
					// Predictor does not work for this ChannelAddress
					continue;
				}
				return predictor.get24HoursPrediction(channelAddress);
			}
		}
		// No matching Predictor found
		return Prediction24Hours.EMPTY;
	}

	@Override
	public CompletableFuture<? extends JsonrpcResponseSuccess> handleJsonrpcRequest(User user, JsonrpcRequest request)
			throws OpenemsNamedException {
		// TODO Auto-generated method stub
		return null;
	}
}
