package io.openems.backend.metadata.odoo.postgres.task;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.TreeMultimap;

import io.openems.backend.metadata.odoo.Field;
import io.openems.backend.metadata.odoo.Field.EdgeDevice;
import io.openems.backend.metadata.odoo.Field.EdgeDeviceStatus;
import io.openems.backend.metadata.odoo.postgres.PgUtils;
import io.openems.common.channel.Level;
import io.openems.common.exceptions.OpenemsException;

public class UpdateEdgeStatesSum extends DatabaseTask {

	private final Logger log = LoggerFactory.getLogger(UpdateEdgeStatesSum.class);

	private final Set<Integer> odooIds;

	public UpdateEdgeStatesSum(Set<Integer> odooIds) {
		this.odooIds = odooIds;
	}

	@Override
	protected void _execute(Connection connection) throws SQLException {
		PreparedStatement psQueryNotAcknowledgedDeviceStates = this.psQueryNotAcknowledgedDeviceStates(connection);
		PreparedStatement psUpdateEdgeState = this.psUpdateEdgeState(connection);

		for (int odooId : this.odooIds) {
			/*
			 * Query non-acknowledged states
			 */
			Level highestLevel = Level.OK;
			String stateChannels;
			psQueryNotAcknowledgedDeviceStates.setInt(1, odooId);
			ResultSet rs = psQueryNotAcknowledgedDeviceStates.executeQuery();
			TreeMap<Level, TreeMultimap<String, String>> levels = new TreeMap<>(
					(l1, l2) -> Integer.compare(l1.getValue(), l2.getValue() * -1));
			while (rs.next()) {
				try {
					// Parse ResultSet
					Level level = Level.valueOf(PgUtils.getAsString(rs, EdgeDeviceStatus.LEVEL).toUpperCase());

					if (level == Level.OK) {
						// ignore OK-Channels; no need to acknowledge them
						continue;
					}

					String componentId = PgUtils.getAsString(rs, EdgeDeviceStatus.COMPONENT_ID);
					String channelName = PgUtils.getAsString(rs, EdgeDeviceStatus.CHANNEL_NAME);

					// Update highest level
					if (level.getValue() > highestLevel.getValue()) {
						highestLevel = level;
					}

					// Add StateChannel to Map
					TreeMultimap<String, String> componentIds = levels.get(level);
					if (componentIds == null) {
						componentIds = TreeMultimap.create();
						levels.put(level, componentIds);
					}
					componentIds.put(componentId, channelName);
				} catch (OpenemsException e) {
					this.log.warn("While updating Edge States: " + e.getMessage());
				}
			}

			// Generate State-Channels-String
			stateChannels = levels.entrySet().stream().map(levelsEntry -> {
				return levelsEntry.getKey().name().toUpperCase() + " " //
						+ levelsEntry.getValue().asMap().entrySet().stream().map(componentIdsEntry -> {
							return componentIdsEntry.getKey() + ": " + String.join(", ", componentIdsEntry.getValue());
						}).collect(Collectors.joining(";"));
			}).collect(Collectors.joining(" "));
			/*
			 * Update Edge openems_sum_state_level and openems_sum_state_text
			 */
			psUpdateEdgeState.setString(1, highestLevel.name().toLowerCase());
			psUpdateEdgeState.setString(2, stateChannels);
			psUpdateEdgeState.setInt(3, odooId);
			psUpdateEdgeState.execute();
		}
	}

	/**
	 * SELECT level, component_id, channel_name FROM {} WHERE device_id = {} AND ...
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psQueryNotAcknowledgedDeviceStates(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"SELECT " + Field.getSqlQueryFields(EdgeDeviceStatus.values()) //
						+ " FROM " + EdgeDeviceStatus.ODOO_TABLE //
						+ " WHERE device_id = ?" //
						+ " AND (" //
						+ " last_acknowledge IS NULL"
						+ " OR (acknowledge_days > 0 AND last_appearance > last_acknowledge + interval '1 day' * acknowledge_days)" //
						+ ")");
	}

	/**
	 * UPDATE {} SET openems_sum_state_level = {}, openems_sum_state_text = {} WHERE
	 * id = {};.
	 * 
	 * @return the PreparedStatement
	 * @throws SQLException on error
	 */
	private PreparedStatement psUpdateEdgeState(Connection connection) throws SQLException {
		return connection.prepareStatement(//
				"UPDATE " + EdgeDevice.ODOO_TABLE //
						+ " SET" //
						+ " " + EdgeDevice.OPENEMS_SUM_STATE.id() + " = ?," //
						+ " " + EdgeDevice.OPENEMS_SUM_STATE_TEXT.id() + " = ?" //
						+ " WHERE id = ?");
	}

	@Override
	public String toString() {
		return "UpdateEdgeStatesSum []";
	}

}