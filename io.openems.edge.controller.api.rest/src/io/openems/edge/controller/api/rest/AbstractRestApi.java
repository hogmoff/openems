package io.openems.edge.controller.api.rest;

import java.util.concurrent.TimeUnit;

import org.eclipse.jetty.server.AcceptRateLimit;
import org.eclipse.jetty.server.ConnectionLimit;
import org.eclipse.jetty.server.Server;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Deactivate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.user.UserService;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.api.common.ApiWorker;
import io.openems.edge.controller.api.rest.readonly.RestApiReadOnly;
import io.openems.edge.timedata.api.Timedata;

public abstract class AbstractRestApi extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public static final boolean DEFAULT_DEBUG_MODE = true;

	protected final ApiWorker apiWorker = new ApiWorker();

	private final Logger log = LoggerFactory.getLogger(RestApiReadOnly.class);
	private final String implementationName;

	private Server server = null;
	private boolean isDebugModeEnabled = DEFAULT_DEBUG_MODE;

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		;
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public AbstractRestApi(String implementationName) {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ChannelId.values() //
		);
		this.implementationName = implementationName;
	}

	protected void activate(ComponentContext context, String id, String alias, boolean enabled,
			boolean isDebugModeEnabled, int apiTimeout, int port) throws OpenemsException {
		super.activate(context, id, alias, enabled);
		this.isDebugModeEnabled = isDebugModeEnabled;

		if (!this.isEnabled()) {
			// abort if disabled
			return;
		}

		this.apiWorker.setTimeoutSeconds(apiTimeout);

		/*
		 * Start RestApi-Server
		 */
		try {
			this.server = new Server(port);
			this.server.setHandler(new RestHandler(this));
			this.server.addBean(new AcceptRateLimit(10, 5, TimeUnit.SECONDS, this.server));
			this.server.addBean(new ConnectionLimit(5, this.server));
			this.server.start();
			this.logInfo(this.log, this.implementationName + " started on port [" + port + "].");
		} catch (Exception e) {
			throw new OpenemsException(this.implementationName + " failed on port [" + port + "].", e);
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
		if (this.server != null) {
			try {
				this.server.stop();
			} catch (Exception e) {
				this.logWarn(this.log, this.implementationName + " failed to stop: " + e.getMessage());
			}
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.apiWorker.run();
	}

	@Override
	protected void logInfo(Logger log, String message) {
		super.logInfo(log, message);
	}

	@Override
	protected void logWarn(Logger log, String message) {
		super.logWarn(log, message);
	}

	@Override
	protected void logError(Logger log, String message) {
		super.logError(log, message);
	}

	protected boolean isDebugModeEnabled() {
		return this.isDebugModeEnabled;
	}

	/**
	 * Gets the Timedata service.
	 * 
	 * @return the service
	 * @throws OpenemsException if the timeservice is not available
	 */
	protected abstract Timedata getTimedata() throws OpenemsException;

	/**
	 * Gets the UserService.
	 * 
	 * @return the service
	 */
	protected abstract UserService getUserService();

	/**
	 * Gets the ComponentManager.
	 * 
	 * @return the service
	 */
	protected abstract ComponentManager getComponentManager();

	/**
	 * Gets the AccessMode.
	 * 
	 * @return the {@link AccessMode}
	 */
	protected abstract AccessMode getAccessMode();
}
