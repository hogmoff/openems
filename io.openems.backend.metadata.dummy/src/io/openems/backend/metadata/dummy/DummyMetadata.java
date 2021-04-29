package io.openems.backend.metadata.dummy;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.backend.common.metadata.AbstractMetadata;
import io.openems.backend.common.metadata.BackendUser;
import io.openems.backend.common.metadata.Edge;
import io.openems.backend.common.metadata.Edge.State;
import io.openems.backend.common.metadata.Metadata;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.session.Role;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfigDiff;
import io.openems.common.utils.StringUtils;

@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.Dummy", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class DummyMetadata extends AbstractMetadata implements Metadata {

	private static final Pattern NAME_NUMBER_PATTERN = Pattern.compile("[^0-9]+([0-9]+)$");

	private final Logger log = LoggerFactory.getLogger(DummyMetadata.class);

	private final AtomicInteger nextUserId = new AtomicInteger(-1);
	private final AtomicInteger nextEdgeId = new AtomicInteger(-1);

	private final Map<String, BackendUser> users = new HashMap<>();
	private final Map<String, MyEdge> edges = new HashMap<>();

	public DummyMetadata() {
		super("Metadata.Dummy");
		this.setInitialized();
	}

	@Activate
	void activate() {
		this.logInfo(this.log, "Activate");
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public BackendUser authenticate() throws OpenemsException {
		int id = this.nextUserId.incrementAndGet();
		String userId = "user" + id;
		BackendUser user = new BackendUser(userId, "User #" + id);
		for (String edgeId : this.edges.keySet()) {
			user.addEdgeRole(edgeId, Role.ADMIN);
		}
		this.users.put(userId, user);
		return user;
	}

	@Override
	public BackendUser authenticate(String username, String password) throws OpenemsNamedException {
		return this.authenticate();
	}

	@Override
	public BackendUser authenticate(String sessionId) throws OpenemsException {
		return this.authenticate();
	}

	@Override
	public Optional<String> getEdgeIdForApikey(String apikey) {
		Optional<MyEdge> edgeOpt = this.edges.values().stream() //
				.filter(edge -> apikey.equals(edge.getApikey())) //
				.findFirst();
		if (edgeOpt.isPresent()) {
			return Optional.ofNullable(edgeOpt.get().getId());
		}
		// not found. Is apikey a valid Edge-ID?
		Optional<Integer> idOpt = DummyMetadata.parseNumberFromName(apikey);
		int id;
		String edgeId;
		if (idOpt.isPresent()) {
			edgeId = apikey;
			id = idOpt.get();
		} else {
			// create new ID
			id = this.nextEdgeId.incrementAndGet();
			edgeId = "edge" + id;
		}
		MyEdge edge = new MyEdge(edgeId, apikey, "OpenEMS Edge #" + id, State.ACTIVE, "", "", Level.OK,
				new EdgeConfig());
		edge.onSetConfig(config -> {
			this.logInfo(this.log, "Edge [" + edgeId + "]. Update config: "
					+ StringUtils.toShortString(EdgeConfigDiff.diff(config, edge.getConfig()).getAsHtml(), 100));
		});
		this.edges.put(edgeId, edge);
		return Optional.ofNullable(edgeId);

	}

	@Override
	public Optional<Edge> getEdge(String edgeId) {
		Edge edge = this.edges.get(edgeId);
		return Optional.ofNullable(edge);
	}

	@Override
	public Optional<BackendUser> getUser(String userId) {
		return Optional.ofNullable(this.users.get(userId));
	}

	@Override
	public Collection<Edge> getAllEdges() {
		return Collections.unmodifiableCollection(this.edges.values());
	}

	public static Optional<Integer> parseNumberFromName(String name) {
		try {
			Matcher matcher = NAME_NUMBER_PATTERN.matcher(name);
			if (matcher.find()) {
				String nameNumberString = matcher.group(1);
				return Optional.ofNullable(Integer.parseInt(nameNumberString));
			}
		} catch (NullPointerException e) {
			/* ignore */
		}
		return Optional.empty();
	}
}
