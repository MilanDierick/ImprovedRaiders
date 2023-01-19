/*
 * Copyright (c) 2022 Milan Dierick | This source file is licensed under a modified version of Apache 2.0
 */

package org.sos.ir;

import script.SCRIPT;

public class Script implements SCRIPT {
    @Override
    public CharSequence name() {
        return "Improved Raiders";
    }

    @Override
    public CharSequence desc() {
        return "Improved Raiders";
    }

    @Override
    public void initBeforeGameCreated() {
    }

    @Override
    public SCRIPT_INSTANCE initAfterGameCreated() {
        return new Instance();
    }
}
