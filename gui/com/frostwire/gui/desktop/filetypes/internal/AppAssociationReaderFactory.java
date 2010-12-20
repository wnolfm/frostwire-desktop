package com.frostwire.gui.desktop.filetypes.internal;

public class AppAssociationReaderFactory {
    public static AppAssociationReader newInstance() {
        return new GnomeAppAssociationReader();
    }
}
