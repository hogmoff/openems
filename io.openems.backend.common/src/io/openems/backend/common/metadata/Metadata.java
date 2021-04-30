package io.openems.backend.common.metadata;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.stream.Collectors;

import org.osgi.annotation.versioning.ProviderType;

import com.google.common.collect.HashMultimap;

import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.EdgeConfig;
import io.openems.common.types.EdgeConfig.Component.Channel;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetail;
import io.openems.common.types.EdgeConfig.Component.Channel.ChannelDetailState;

@ProviderType
public interface Metadata {

	/**
	 * Was the Metadata service fully initialized?.
	 * 
	 * <p>
	 * The service might take some time in the beginning to establish a connection
	 * or to cache data from an external database.
	 * 
	 * @return true if it is initialized
	 */
	public boolean isInitialized();

	/**
	 * See {@link #isInitialized()}.
	 * 
	 * @param callback the callback on 'isInitialized'
	 */
	public void addOnIsInitializedListener(Runnable callback);

	/**
	 * See {@link #isInitialized()}.
	 * 
	 * @param callback the callback on 'isInitialized'
	 */
	public void removeOnIsInitializedListener(Runnable callback);

	/**
	 * Authenticates the User by username and password.
	 * 
	 * @param username the Username
	 * @param password the Password
	 * @return the {@link User}
	 * @throws OpenemsNamedException on error
	 */
	public User authenticate(String username, String password) throws OpenemsNamedException;

	/**
	 * Authenticates the User by a Token.
	 * 
	 * @param token the Token
	 * @return the {@link User}
	 * @throws OpenemsNamedException on error
	 */
	public User authenticate(String token) throws OpenemsNamedException;

	/**
	 * Closes a session for a User.
	 * 
	 * @param user the {@link User}
	 */
	public void logout(User user);

	/**
	 * Gets the Edge-ID for an API-Key, i.e. authenticates the API-Key.
	 * 
	 * @param apikey the API-Key
	 * @return the Edge-ID or Empty
	 */
	public abstract Optional<String> getEdgeIdForApikey(String apikey);

	/**
	 * Get an Edge by its unique Edge-ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the Edge as Optional
	 */
	public abstract Optional<Edge> getEdge(String edgeId);

	/**
	 * Get an Edge by its unique Edge-ID. Throws an Exception if there is no Edge
	 * with this ID.
	 * 
	 * @param edgeId the Edge-ID
	 * @return the Edge
	 * @throws OpenemsException on error
	 */
	public default Edge getEdgeOrError(String edgeId) throws OpenemsException {
		Optional<Edge> edgeOpt = this.getEdge(edgeId);
		if (edgeOpt.isPresent()) {
			return edgeOpt.get();
		} else {
			throw new OpenemsException("Unable to get Edge for id [" + edgeId + "]");
		}
	}

	/**
	 * Gets the User for the given User-ID.
	 * 
	 * @param userId the User-ID
	 * @return the {@link User}, or Empty
	 */
	public abstract Optional<User> getUser(String userId);

	/**
	 * Gets all Edges.
	 * 
	 * @return collection of Edges.
	 */
	public abstract Collection<Edge> getAllEdges();

	/**
	 * Helper method for creating a String of all active State-Channels by Level.
	 * 
	 * @param activeStateChannels Map of ChannelAddress and
	 *                            EdgeConfig.Component.Channel; as returned by
	 *                            Edge.onSetSumState()
	 * @return a string
	 */
	public static String activeStateChannelsToString(
			Map<ChannelAddress, EdgeConfig.Component.Channel> activeStateChannels) {
		// Sort active State-Channels by Level and Component-ID
		HashMap<Level, HashMultimap<String, Channel>> states = new HashMap<>();
		for (Entry<ChannelAddress, Channel> entry : activeStateChannels.entrySet()) {
			ChannelDetail detail = entry.getValue().getDetail();
			if (detail instanceof ChannelDetailState) {
				Level level = ((ChannelDetailState) detail).getLevel();
				HashMultimap<String, Channel> channelsByComponent = states.get(level);
				if (channelsByComponent == null) {
					channelsByComponent = HashMultimap.create();
					states.put(level, channelsByComponent);
				}
				channelsByComponent.put(//
						entry.getKey().getComponentId(), //
						entry.getValue());
			}
		}
		StringBuilder result = new StringBuilder();
		for (Level level : Level.values()) {
			HashMultimap<String, Channel> channelsByComponent = states.get(level);
			if (channelsByComponent != null) {
				if (result.length() > 0) {
					result.append("| ");
				}
				result.append(level.name() + ": ");
				StringBuilder subResult = new StringBuilder();
				for (Entry<String, Collection<Channel>> entry : channelsByComponent.asMap().entrySet()) {
					if (subResult.length() > 0) {
						subResult.append("; ");
					}
					subResult.append(entry.getKey() + ": ");
					subResult.append(entry.getValue().stream() //
							.map(channel -> {
								if (!channel.getText().isEmpty()) {
									return channel.getText();
								} else {
									return channel.getId();
								}
							}) //
							.collect(Collectors.joining(", ")));
				}
				result.append(subResult);
			}
		}
		return result.toString();
	}

}
