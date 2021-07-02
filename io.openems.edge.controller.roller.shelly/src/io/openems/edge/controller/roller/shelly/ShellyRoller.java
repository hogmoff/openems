package io.openems.edge.controller.roller.shelly;

import io.openems.common.channel.AccessMode;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;


public interface ShellyRoller extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		OPENROLLER(Doc.of(OpenemsType.BOOLEAN).accessMode(AccessMode.READ_WRITE));

		//OPENROLLER(new BooleanDoc() //
		//		.accessMode(AccessMode.READ_WRITE) //
		//		.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.OPENROLLERDEBUG))); //
		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	public enum openMode{
		EXTERNAL, SUNRISE_SUNSET, TWILIGHT
	}

}
