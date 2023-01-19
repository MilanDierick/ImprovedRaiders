/*
 * Copyright (c) 2022 Milan Dierick | This source file is licensed under a modified version of Apache 2.0
 */

package org.sos.ir;

import settlement.main.SETT;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;
import view.sett.IDebugPanelSett;

public class RaidEvent {
	public RaidEvent() {
		IDebugPanelSett.add("Raid Event", this::triggerRaid);
	}
	
	public void onTick(double delta) {

	}

	public void onSerialize(FilePutter writer) {

	}

	public void onDeserialize(FileGetter reader) {

	}
	
	private void triggerRaid() {
		int soldierCount = 200;
		String raiderName = "Raiders";
		
		CouncilorMessage councilorMessage = new CouncilorMessage("Councilor", soldierCount);
		
		new RaidersMessage("Raiders!",
		                   SETT.FACTION().capitolRegion().name().toString(),
		                   raiderName,
		                   councilorMessage
		).send();
	}
}
