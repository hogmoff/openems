package io.openems.edge.io.edimax.sp2101W;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.channel.Level;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;

public enum EdimaxChannelID implements io.openems.edge.common.channel.ChannelId {
	/**
	 * Holds writes to Relay Output 1 for debugging
	 * 
	 * <ul>
	 * <li>Interface: EdimaxRelayOutput
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	DEBUG_RELAY_1(Doc.of(OpenemsType.BOOLEAN)), //
	/**
	 * Relay Output 1
	 * 
	 * <ul>
	 * <li>Interface: EdimaxRelayOutput
	 * <li>Type: Boolean
	 * <li>Range: On/Off
	 * </ul>
	 */
	RELAY_1(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(EdimaxChannelID.DEBUG_RELAY_1))), //
	
	/**
	 * Current.
	 */
	CURRENT(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.READ_ONLY) //
			.unit(Unit.MILLIAMPERE) //
	),
	
	/**
	 * Power.
	 */
	ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.READ_ONLY) //
			.unit(Unit.WATT) //
	),

	SLAVE_COMMUNICATION_FAILED(Doc.of(Level.FAULT)); //

	private final Doc doc;

	private EdimaxChannelID(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
} 