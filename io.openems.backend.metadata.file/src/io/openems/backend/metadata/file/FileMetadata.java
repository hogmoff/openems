package io.openems.backend.metadata.file;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

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
import io.openems.common.utils.JsonUtils;

/**
 * This implementation of MetadataService reads Edges configuration from a file.
 * The layout of the file is as follows:
 * 
 * <pre>
 * {
 *   edges: {
 *     [edgeId: string]: {
 *       comment: string,
 *       apikey: string
 *     } 
 *   }
 * }
 * </pre>
 * 
 * <p>
 * This implementation does not require any login. It always serves the same
 * user, which has 'ADMIN'-permissions on all given Edges.
 */
@Designate(ocd = Config.class, factory = false)
@Component(//
		name = "Metadata.File", //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class FileMetadata extends AbstractMetadata implements Metadata {

	private final Logger log = LoggerFactory.getLogger(FileMetadata.class);

	private final BackendUser user = new BackendUser("admin", "Administrator");
	private final Map<String, MyEdge> edges = new HashMap<>();

	private String path = "";

	public FileMetadata() {
		super("Metadata.File");
	}

	@Activate
	void activate(Config config) {
		log.info("Activate [path=" + config.path() + "]");
		this.path = config.path();

		// Read the data async
		CompletableFuture.runAsync(() -> {
			this.refreshData();
		});
	}

	@Deactivate
	void deactivate() {
		this.logInfo(this.log, "Deactivate");
	}

	@Override
	public BackendUser authenticate() throws OpenemsException {
		return this.user;
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
	public synchronized Optional<String> getEdgeIdForApikey(String apikey) {
		this.refreshData();
		for (Entry<String, MyEdge> entry : this.edges.entrySet()) {
			MyEdge edge = entry.getValue();
			if (edge.getApikey().equals(apikey)) {
				return Optional.of(edge.getId());
			}
		}
		return Optional.empty();
	}

	@Override
	public synchronized Optional<Edge> getEdge(String edgeId) {
		this.refreshData();
		Edge edge = this.edges.get(edgeId);
		return Optional.ofNullable(edge);
	}

	@Override
	public Optional<BackendUser> getUser(String userId) {
		return Optional.of(this.user);
	}

	@Override
	public synchronized Collection<Edge> getAllEdges() {
		this.refreshData();
		return Collections.unmodifiableCollection(this.edges.values());
	}

	private synchronized void refreshData() {
		if (this.edges.isEmpty()) {
			// read file
			StringBuilder sb = new StringBuilder();
			String line = null;
			try (BufferedReader br = new BufferedReader(new FileReader(this.path))) {
				while ((line = br.readLine()) != null) {
					sb.append(line);
				}
			} catch (IOException e) {
				this.logWarn(this.log, "Unable to read file [" + this.path + "]: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			List<MyEdge> edges = new ArrayList<>();

			// parse to JSON
			try {
				JsonElement config = JsonUtils.parse(sb.toString());
				JsonObject jEdges = JsonUtils.getAsJsonObject(config, "edges");
				for (Entry<String, JsonElement> entry : jEdges.entrySet()) {
					JsonObject edge = JsonUtils.getAsJsonObject(entry.getValue());
					edges.add(new MyEdge(//
							entry.getKey(), // Edge-ID
							JsonUtils.getAsString(edge, "apikey"), //
							JsonUtils.getAsString(edge, "comment"), //
							State.ACTIVE, // State
							"", // Version
							"", // Product-Type
							Level.OK, // Sum-State
							new EdgeConfig() // Config
					));
				}
			} catch (OpenemsNamedException e) {
				this.logWarn(this.log, "Unable to JSON-parse file [" + this.path + "]: " + e.getMessage());
				e.printStackTrace();
				return;
			}

			// Add Edges and configure User permissions
			for (MyEdge edge : edges) {
				this.edges.put(edge.getId(), edge);
				this.user.addEdgeRole(edge.getId(), Role.ADMIN);
			}
		}
		this.setInitialized();
	}

}
