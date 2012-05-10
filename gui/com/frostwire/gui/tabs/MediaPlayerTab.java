package com.frostwire.gui.tabs;

import java.awt.BorderLayout;

import javax.swing.JComponent;
import javax.swing.JPanel;

import uk.co.caprica.vlcj.component.EmbeddedMediaPlayerComponent;

import com.frostwire.gui.player.VlcPlayer;
import com.limegroup.gnutella.gui.I18n;
import com.sun.jna.NativeLibrary;

/**
 * This class contains access to the chat tab properties.
 */
public final class MediaPlayerTab extends AbstractTab {

    private static JPanel PANEL = new JPanel(new BorderLayout());

    private final EmbeddedMediaPlayerComponent mediaPlayerComponent;

    public MediaPlayerTab() {
        super(I18n.tr("Player"), I18n.tr("Media Player"), "chat_tab");

        NativeLibrary.addSearchPath("vlc", "./lib/native/vlc/mac/lib");

        mediaPlayerComponent = new EmbeddedMediaPlayerComponent();
        PANEL.add(mediaPlayerComponent);

        VlcPlayer.instance().setInnerPlayer(mediaPlayerComponent.getMediaPlayer());
    }

    public JComponent getComponent() {
        return PANEL;
    }
}
