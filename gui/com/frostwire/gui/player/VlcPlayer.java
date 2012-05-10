package com.frostwire.gui.player;

import uk.co.caprica.vlcj.player.embedded.EmbeddedMediaPlayer;

public final class VlcPlayer {

    private EmbeddedMediaPlayer player;

    private static VlcPlayer instance;

    public static VlcPlayer instance() {
        if (instance == null) {
            instance = new VlcPlayer();
        }
        return instance;
    }

    private VlcPlayer() {
    }

    public void setInnerPlayer(EmbeddedMediaPlayer player) {
        this.player = player;
    }

    public void open(String mrl) {
        player.playMedia(mrl);
    }
}
