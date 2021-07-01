package io.openems.edge.controller.roller.shelly;

import io.openems.common.types.OptionsEnum;

public enum State implements OptionsEnum {
	UNDEFINED(-1, "Undefined"), //

	SET_RUNNING_DOWN(10, "Set Running Down"), //
	SET_RUNNING_UP(11, "Set Running Up"), //
	
	RUNNING_DOWN(15, "Running Down"), //
	RUNNING_UP(16, "Running Up"), //
	
	GO_TO_SLATE_POS(30, "Go to for open Slats"), //
	SET_SLATE_TO_OPENPOS(31, "Set Open Slats"), //
	SLATE_TO_OPENPOS(32, "Open Slats"), //
	
	ROLLER_OPEN(40, "Roller Open"), //
	ROLLER_CLOSED(41, "Roller Closed"), //
	ROLLER_CLOSED_WITH_OPEN_SLATS(42, "Roller Closed with open Slats"), //
	
	ERROR(50, "Error") //
	;

	private final int value;
	private final String name;

	private State(int value, String name) {
		this.value = value;
		this.name = name;
	}

	@Override
	public int getValue() {
		return value;
	}

	@Override
	public String getName() {
		return name;
	}

	@Override
	public OptionsEnum getUndefined() {
		return UNDEFINED;
	}
}