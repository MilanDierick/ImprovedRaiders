/*
 * Copyright (c) 2022 Milan Dierick | This source file is licensed under a modified version of Apache 2.0
 */

package org.sos.ir;

import script.SCRIPT;
import snake2d.Renderer;
import snake2d.util.file.FileGetter;
import snake2d.util.file.FilePutter;

public class Instance implements SCRIPT.SCRIPT_INSTANCE {
	private final RaidEvent event;
	
	public Instance() {
		event = new RaidEvent();
	}
	
	@Override
	public void update(double ds) {
		event.onTick(ds);
	}
	
	@Override
	public void render(Renderer r, float ds) {

	}
	
	@Override
	public void save(FilePutter file) {
		event.onSerialize(file);
	}
	
	@Override
	public void load(FileGetter file) {
		event.onDeserialize(file);
	}
}
