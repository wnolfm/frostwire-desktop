/*
 * Created by Angel Leon (@gubatron), Alden Torres (aldenml)
 * Copyright (c) 2011, 2012, FrostWire(R). All rights reserved.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.frostwire.gui.player;

import java.awt.Dimension;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import javax.swing.JCheckBox;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;

import org.limewire.concurrent.ExecutorsHelper;
import org.limewire.util.FileUtils;
import org.limewire.util.FilenameUtils;
import org.limewire.util.OSUtils;

import com.frostwire.alexandria.Playlist;
import com.frostwire.alexandria.PlaylistItem;
import com.frostwire.gui.library.LibraryMediator;
import com.frostwire.gui.mplayer.MPlayer;
import com.frostwire.mp3.Mp3File;
import com.frostwire.mp4.IsoFile;
import com.frostwire.mplayer.IcyInfoListener;
import com.frostwire.mplayer.MediaPlaybackState;
import com.frostwire.mplayer.PositionListener;
import com.frostwire.mplayer.StateListener;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.gui.GUIMediator;
import com.limegroup.gnutella.gui.I18n;
import com.limegroup.gnutella.gui.MPlayerMediator;
import com.limegroup.gnutella.gui.RefreshListener;
import com.limegroup.gnutella.settings.PlayerSettings;

/**
 * An media player to play compressed and uncompressed media.
 * 
 * @author gubatron
 * @author aldenml
 * 
 */
public class MediaPlayer implements RefreshListener, MPlayerUIEventListener {

    private static final String MPLAYER_DEFAULT_LINUX_PATH = "/usr/bin/mplayer";

	private static final String[] PLAYABLE_EXTENSIONS = new String[] { "mp3", "ogg", "wav", "wma", "m4a", "aac", "flac", "mp4", "flv", "avi", "mov", "mkv", "mpg", "mpeg", "3gp", "m4v", "webm" };

    /**
     * Our list of MediaPlayerListeners that are currently listening for events
     * from this player
     */
    private List<MediaPlayerListener> listenerList = new CopyOnWriteArrayList<MediaPlayerListener>();

    private MPlayer mplayer;
    private AudioSource currentSong;
    private Playlist currentPlaylist;
    private List<AudioSource> playlistFilesView;
    private RepeatMode repeatMode;
    private boolean shuffle;
    private boolean playNextMedia;

    private double volume;
    private float volumeGainFactor;

    private Queue<AudioSource> lastRandomFiles;

    private final ExecutorService playExecutor;

    private static MediaPlayer instance;

    private long durationInSeconds;
    private boolean isPlayPausedForSliding = false;
    //private boolean isSliding = false; //not used.
    
    private boolean stateNotificationsEnabled = true;
    
    public static MediaPlayer instance() {
        if (instance == null) {
        	if ( OSUtils.isWindows() ) {
        		instance = new MediaPlayerWindows();
        	} else if (OSUtils.isMacOSX()) {
        		instance = new MediaPlayerOSX();
        	} else {
        		instance = new MediaPlayer();
        	}
        }
        return instance;
    }

    protected MediaPlayer() {
        lastRandomFiles = new LinkedList<AudioSource>();
        playExecutor = ExecutorsHelper.newProcessingQueue("AudioPlayer-PlayExecutor");
        
        initVolumeGainFactor();

        String playerPath;
        playerPath = getPlayerPath();
        
        MPlayer.initialise(new File(playerPath));
        mplayer = new MPlayer();
        mplayer.addPositionListener(new PositionListener() {
            public void positionChanged(float currentTimeInSecs) {
                notifyProgress(currentTimeInSecs);
            }
        });
        mplayer.addStateListener(new StateListener() {
            public void stateChanged(MediaPlaybackState newState) {
                if (newState == MediaPlaybackState.Closed) { // This is the case
                                                             // mplayer is
                                                             // done with the
                                                             // current file
                    handleNextSong();
                }
                if (newState == MediaPlaybackState.Playing || newState == MediaPlaybackState.Paused) {
                    setVolume(volume);
                }
            }
        });
        mplayer.addIcyInfoListener(new IcyInfoListener() {
            public void newIcyInfoData(String data) {
                notifyIcyInfo(data);
            }
        });

        repeatMode = PlayerSettings.LOOP_PLAYLIST.getValue() ? RepeatMode.All : RepeatMode.None;
        shuffle = PlayerSettings.SHUFFLE_PLAYLIST.getValue();
        playNextMedia = true;
        volume = PlayerSettings.PLAYER_VOLUME.getValue();
        notifyVolumeChanged();

        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(new KeyEventDispatcher() {

            @Override
            public boolean dispatchKeyEvent(KeyEvent e) {
                if (e.getID() == KeyEvent.KEY_PRESSED && e.getKeyCode() == KeyEvent.VK_SPACE) {
                    Object s = e.getComponent();
                    if (!(s instanceof JTextField) && !(s instanceof JTable && ((JTable) s).isEditing() && !(s instanceof JCheckBox))) {
                        togglePause();
                        return true;
                    }
                }
                return false;
            }
        });
        
        // prepare to receive UI events
        MPlayerUIEventHandler.instance().addListener(this);
    }
    
    private void initVolumeGainFactor() {
    	volumeGainFactor = 30;
    	
		if (OSUtils.isLinux()) {
			volumeGainFactor = 100;
		}
		
	}

    protected String getPlayerPath() {
        if (OSUtils.isLinux()) {
            File f = new File(MPLAYER_DEFAULT_LINUX_PATH);
            if (!f.exists()) {
                
                GUIMediator.safeInvokeLater(new Runnable() {
                    @Override
                    public void run() {
                        
                        String instructions = I18n
                                .tr("<br><br>To Install <b>mplayer</b> in Ubuntu open a terminal window and type \"<b>sudo apt-get install mplayer</b>\".<br><br>If you have installed mplayer already at a custom location, <b>make sure to have a symlink pointing to your mplayer executable</b> at <b><font color=\"blue\">"
                                        + MPLAYER_DEFAULT_LINUX_PATH + "</font></b>");
                        
                        if (!OSUtils.isUbuntu()) {
                            instructions = "";
                        }
                        
                        GUIMediator.showError(I18n.tr("<html><b>FrostWire requires Mplayer to play your media</b> but I could not find it in your computer.<br><br>If you want to use FrostWire as a media player <b>Please install mplayer and restart FrostWire.</b>")
                                + I18n.tr(instructions));
                                
                    }
                });

            }
        }
        return MPLAYER_DEFAULT_LINUX_PATH;
    }
    
    protected float getVolumeGainFactor() {
    	return volumeGainFactor;
    }
    
    public Dimension getCurrentVideoSize() {
    	if ( mplayer != null ) {
    		return mplayer.getVideoSize();
    	} else {
    		return null;
    	}
    }
    
    public AudioSource getCurrentSong() {
        return currentSong;
    }

    public Playlist getCurrentPlaylist() {
        return currentPlaylist;
    }

    public List<AudioSource> getPlaylistFilesView() {
        return playlistFilesView;
    }

    public RepeatMode getRepeatMode() {
        return repeatMode;
    }

    public void setRepeatMode(RepeatMode repeatMode) {
        this.repeatMode = repeatMode;
        PlayerSettings.LOOP_PLAYLIST.setValue(repeatMode == RepeatMode.All);
    }

    public boolean isShuffle() {
        return shuffle;
    }

    public void setShuffle(boolean shuffle) {
        this.shuffle = shuffle;
        PlayerSettings.SHUFFLE_PLAYLIST.setValue(shuffle);
    }

    /**
     * Adds the specified MediaPlayer listener to the list
     */
    public void addMediaPlayerListener(MediaPlayerListener listener) {
        listenerList.add(listener);
    }

    /**
     * Removes the specified MediaPlayer listener from the list
     */
    public void removeMediaPlayerListener(MediaPlayerListener listener) {
        listenerList.remove(listener);
    }

    public MediaPlaybackState getState() {
        return mplayer.getCurrentState();
    }

    /**
     * Loads a AudioSource into the player to play next
     */
    public void loadMedia(AudioSource source, boolean play, boolean playNextSong, Playlist currentPlaylist, List<AudioSource> playlistFilesView) {
        currentSong = source;
        this.playNextMedia = playNextSong;
        this.currentPlaylist = currentPlaylist;
        this.playlistFilesView = playlistFilesView;
        notifyOpened(source);
        if (play) {
            durationInSeconds = -1;

            if (currentSong.getFile() != null) {
                LibraryMediator.instance().getLibraryCoverArt().setFile(currentSong.getFile());
                calculateDurationInSecs(currentSong.getFile());
                playMedia();
            } else if (currentSong.getPlaylistItem() != null) {
                LibraryMediator.instance().getLibraryCoverArt().setFile(new File(currentSong.getPlaylistItem().getFilePath()));
                playMedia();
                durationInSeconds = (long) currentSong.getPlaylistItem().getTrackDurationInSecs();
            } else if (currentSong instanceof InternetRadioAudioSource) {
                LibraryMediator.instance().getLibraryCoverArt().setDefault();
                playMedia(false);
            } else if (currentSong instanceof StreamAudioSource) {
                LibraryMediator.instance().getLibraryCoverArt().setDefault();
                playMedia(((StreamAudioSource) currentSong).showPlayerWindow());
            } else if (currentSong instanceof DeviceAudioSource) {
                LibraryMediator.instance().getLibraryCoverArt().setDefault();
                playMedia(((DeviceAudioSource) currentSong).showPlayerWindow());
            }
        }
    }

    private void calculateDurationInSecs(File f) {
        if (FileUtils.getFileExtension(f) == null || !FileUtils.getFileExtension(f).toLowerCase().endsWith("mp3") || !FileUtils.getFileExtension(f).toLowerCase().endsWith("m4a")) {
            durationInSeconds = -1;
            return;
        }

        if (FileUtils.getFileExtension(f).toLowerCase().endsWith("mp3")) {
            durationInSeconds = getDurationFromMP3(f);
        } else if (FileUtils.getFileExtension(f).toLowerCase().endsWith("m4a")) {
            durationInSeconds = getDurationFromM4A(f);
        }
    }

    private long getDurationFromMP3(File f) {
        try {
            Mp3File mp3 = new Mp3File(f.getAbsolutePath());
            return mp3.getLengthInSeconds();
        } catch (Throwable e) {
            return -1;
        }
    }

    private long getDurationFromM4A(File f) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(f);
            FileChannel inFC = fis.getChannel();
            IsoFile isoFile = new IsoFile(inFC);

            return isoFile.getMovieBox().getMovieHeaderBox().getDuration() / isoFile.getMovieBox().getMovieHeaderBox().getTimescale();
        } catch (Throwable e) {
            return -1;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (Throwable e) {
                    // ignore
                }
            }
        }
    }

    public void asyncLoadMedia(final AudioSource source, final boolean play, final boolean playNextSong, final Playlist currentPlaylist, final List<AudioSource> playlistFilesView) {
        playExecutor.execute(new Runnable() {
            public void run() {
                loadMedia(source, play, playNextSong, currentPlaylist, playlistFilesView);
            }
        });
    }

    public void loadMedia(AudioSource source, boolean play, boolean playNextSong) {
        loadMedia(source, play, playNextSong, currentPlaylist, playlistFilesView);
    }

    public void asyncLoadMedia(final AudioSource source, final boolean play, final boolean playNextSong) {
        playExecutor.execute(new Runnable() {
            public void run() {
                loadMedia(source, play, playNextSong);
            }
        });
    }

    public void loadMedia(AudioSource audioSource) {
        loadMedia(audioSource, false, false, null, null);
    }

	private String stopAndPrepareFilename() {
		mplayer.stop();
        setVolume(volume);

        String filename = "";
        if (currentSong.getFile() != null) {
            filename = currentSong.getFile().getAbsolutePath();
        } else if (currentSong.getURL() != null) {
            filename = currentSong.getURL().toString();
        } else if (currentSong.getPlaylistItem() != null) {
            filename = currentSong.getPlaylistItem().getFilePath();
        }
		return filename;
	}
    
    /** Force showing or not the media player window */
    public void playMedia(boolean showPlayerWindow) {
      	String filename = stopAndPrepareFilename();
        
        if (filename.length() > 0) {
	        MPlayerMediator mplayerMediator = MPlayerMediator.instance();
	        
	        if (mplayerMediator != null) {
	        	mplayerMediator.showPlayerWindow(showPlayerWindow);
	        }
	        
	        mplayer.open(filename);
        }
        
        notifyState(getState());
    }
    
    /**
     * Plays a file and determines wether or not to show the player window based on the MediaType of the file.
     */
    public void playMedia() {
    	
    	String filename = stopAndPrepareFilename();
        
        if (filename.length() > 0) {
	        boolean isVideoFile = MediaType.getVideoMediaType().matches(filename);
	        MPlayerMediator mplayerMediator = MPlayerMediator.instance();
	        
	        
	        
	        if (mplayerMediator != null) {
	        	mplayerMediator.showPlayerWindow(isVideoFile);
	        }
	        
	        mplayer.open(filename);
        }
        
        notifyState(getState());
    }

    /**
     * Toggle pause the current song
     */
    public void togglePause() {
    	mplayer.togglePause();
    	notifyState(getState());
    }
    
    /**
     * Stops the current song
     */
    public void stop() {
        mplayer.stop();
        currentSong = null;
        notifyState(getState());
    }
    
    
    public void fastForward() {
    	mplayer.fastForward();
    }
    
    public void rewind() {
    	mplayer.rewind();
    }

    /**
     * Seeks to a new location in the current song
     */
    public void seek(float timeInSecs) {
    	mplayer.seek(timeInSecs);
    	notifyState(getState());
    }
    
    /**
     * Sets the gain(volume) for the outputline
     * 
     * @param gain
     *            - [0.0 <-> 1.0]
     * @throws IOException
     *             - thrown when the soundcard does not support this operation
     */
    public void setVolume(double fGain) {
    	
        volume = Math.max(Math.min(fGain, 1.0), 0.0);
        mplayer.setVolume((int) (volume * getVolumeGainFactor()));
        PlayerSettings.PLAYER_VOLUME.setValue((float) volume);
        notifyVolumeChanged();
    }
    
    public double getVolume() {
    	return volume;
    }
    
    public void incrementVolume() {
    	setVolume(getVolume() + 0.1);
    }
    
    public void decrementVolume() {
    	setVolume(getVolume() - 0.1);
    }

    protected void notifyVolumeChanged() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireVolumeChanged(volume);
            }
        });
    }

    public static boolean isPlayableFile(File file) {
        return file.exists() && !file.isDirectory() && isPlayableFile(file.getAbsolutePath());
    }

    public static boolean isPlayableFile(String filename) {
        return FilenameUtils.hasExtension(filename, getPlayableExtensions());
    }

    public static String[] getPlayableExtensions() {
        return PLAYABLE_EXTENSIONS;
    }

    public static boolean isPlayableFile(AudioSource audioSource) {
        if (audioSource.getFile() != null) {
            return audioSource.getFile().exists() && isPlayableFile(audioSource.getFile());
        } else if (audioSource.getPlaylistItem() != null) {
            return new File(audioSource.getPlaylistItem().getFilePath()).exists() && isPlayableFile(audioSource.getPlaylistItem().getFilePath());
        } else if (audioSource instanceof InternetRadioAudioSource) {
            return true;
        } else if (audioSource instanceof StreamAudioSource) {
            return true;
        } else if (audioSource instanceof DeviceAudioSource) {
            return isPlayableFile(((DeviceAudioSource) audioSource).getFileDescriptor().filePath);
        } else {
            return false;
        }
    }

    /**
     * Notify listeners when a new audio source has been opened.
     * 
     * @param properties
     *            - any properties about the source that we extracted
     */
    protected void notifyOpened(final AudioSource audioSource) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireOpened(audioSource);
            }
        });
    }

    /**
     * Notify listeners about an AudioPlayerEvent. This creates general state
     * modifications to the player such as the transition from opened to playing
     * to paused to end of song.
     * 
     * @param code
     *            - the type of player event.
     * @param position
     *            in the stream when the event occurs.
     * @param value
     *            if the event was a modification such as a volume update, list
     *            the new value
     */
    protected void notifyState(final MediaPlaybackState state) {
        
    	if ( stateNotificationsEnabled ) {
	    	SwingUtilities.invokeLater(new Runnable() {
	            public void run() {
	                fireState(state);
	            }
	        });
    	}
    }

    /**
     * fires a progress event off a new thread. This lets us safely fire events
     * off of the player thread while using a lock on the input stream
     */
    protected void notifyProgress(final float currentTimeInSecs) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireProgress(currentTimeInSecs);
            }
        });
    }

    protected void notifyIcyInfo(final String data) {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                fireIcyInfo(data);
            }
        });
    }

    /**
     * This is fired every time a new song is loaded and ready to play. The
     * properties map contains information about the type of song such as bit
     * rate, sample rate, media type(MPEG, Streaming,etc..), etc..
     */
    protected void fireOpened(AudioSource audioSource) {
        for (MediaPlayerListener listener : listenerList) {
            listener.mediaOpened(this, audioSource);
        }
    }

    /**
     * Fired every time a byte stream is written to the sound card. This lets
     * listeners be aware of what point in the entire file is song is currently
     * playing. This also returns a copy of the written byte[] so it can get
     * passed along to objects such as a FFT for visual feedback of the song
     */
    protected void fireProgress(float currentTimeInSecs) {
        for (MediaPlayerListener listener : listenerList) {
            listener.progressChange(this, currentTimeInSecs);
        }
    }

    protected void fireVolumeChanged(double currentVolume) {
        for (MediaPlayerListener listener : listenerList) {
            listener.volumeChange(this, currentVolume);
        }
    }

    /**
     * Fired every time the state of the player changes. This allows a listener
     * to be aware of state transitions such as from OPENED -> PLAYING ->
     * STOPPED -> EOF
     */
    protected void fireState(MediaPlaybackState state) {
        for (MediaPlayerListener listener : listenerList) {
            listener.stateChange(this, state);
        }
    }

    protected void fireIcyInfo(String data) {
        for (MediaPlayerListener listener : listenerList) {
            listener.icyInfo(this, data);
        }
    }

    /**
     * returns the current state of the player and position of the song being
     * played
     */
    public void refresh() {
        notifyState(getState());
    }

    private void handleNextSong() {
        if (!playNextMedia) {
            return;
        }

        if (currentPlaylist != null && currentPlaylist.isDeleted()) {
            return;
        }

        AudioSource song = null;

        if (getRepeatMode() == RepeatMode.Song) {
            song = currentSong;
        } else if (isShuffle()) {
            song = getNextRandomSong(currentSong);
        } else if (getRepeatMode() == RepeatMode.All) {
            song = getNextContinuousSong(currentSong);
        } else {
            song = getNextSong(currentSong);
        }

        if (song != null) {
            //System.out.println(song.getFile());
            asyncLoadMedia(song, true, true, currentPlaylist, playlistFilesView);
        }
    }

    public boolean isThisBeingPlayed(File file) {
        if (getState() == MediaPlaybackState.Stopped) {
            return false;
        }

        AudioSource currentSong = getCurrentSong();
        if (currentSong == null) {
            return false;
        }

        File currentSongFile = currentSong.getFile();

        if (currentSongFile != null && file.equals(currentSongFile))
            return true;

        PlaylistItem playlistItem = currentSong.getPlaylistItem();
        if (playlistItem != null && new File(playlistItem.getFilePath()).equals(file)) {
            return true;
        }

        return false;
    }

    public boolean isThisBeingPlayed(String file) {
        if (getState() == MediaPlaybackState.Stopped) {
            return false;
        }

        AudioSource currentSong = getCurrentSong();
        if (currentSong == null) {
            return false;
        }

        String currentSongUrl = currentSong.getURL();

        if (currentSongUrl != null && file.toLowerCase().equals(currentSongUrl.toString().toLowerCase())) {
            return true;
        }

        return false;
    }

    public boolean isThisBeingPlayed(PlaylistItem playlistItem) {
        if (getState() == MediaPlaybackState.Stopped) {
            return false;
        }

        AudioSource currentSong = getCurrentSong();
        if (currentSong == null) {
            return false;
        }

        PlaylistItem currentSongFile = currentSong.getPlaylistItem();

        if (currentSongFile != null && playlistItem.equals(currentSongFile))
            return true;

        return false;
    }

    public synchronized void setPlaylistFilesView(List<AudioSource> playlistFilesView) {
        this.playlistFilesView = playlistFilesView;
    }

    public AudioSource getNextRandomSong(AudioSource currentSong) {
        if (playlistFilesView == null) {
            return null;
        }

        AudioSource songFile;
        int count = 4;
        while ((songFile = findRandomSongFile(currentSong)) == null && count-- > 0)
            ;

        if (count > 0) {
            lastRandomFiles.add(songFile);
            if (lastRandomFiles.size() > 3) {
                lastRandomFiles.poll();
            }
        } else {
            songFile = currentSong;
            lastRandomFiles.clear();
            lastRandomFiles.add(songFile);
        }

        return songFile;
    }

    public AudioSource getNextContinuousSong(AudioSource currentSong) {
        if (playlistFilesView == null) {
            return null;
        }

        int n = playlistFilesView.size();
        if (n == 1) {
            return playlistFilesView.get(0);
        }
        for (int i = 0; i < n; i++) {
            try {
                AudioSource f1 = playlistFilesView.get(i);
                if (currentSong.equals(f1)) {
                    for (int j = 1; j < n; j++) {
                        AudioSource file = playlistFilesView.get((j + i) % n);
                        if (isPlayableFile(file) || file instanceof DeviceAudioSource) {
                            return file;
                        }
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    public AudioSource getNextSong(AudioSource currentSong) {
        if (playlistFilesView == null) {
            return null;
        }

        int n = playlistFilesView.size();
        if (n == 1) {
            return playlistFilesView.get(0);
        }

        //PlaylistFilesView should probably have a HashTable<AudioSource,Integer>
        //Where the integer is the index of the AudioSource on playlistFilesView.
        //This way we could easily find the current song and know the index of the
        //next or previous song.
        //When you have lots of files, I think the search below might
        //be too slow.

        for (int i = 0; i < n; i++) {
            try {
                AudioSource f1 = playlistFilesView.get(i);
                if (currentSong.equals(f1)) {
                    for (int j = i + 1; j < n; j++) {
                        AudioSource file = playlistFilesView.get(j);
                        if (isPlayableFile(file)) {
                            return file;
                        }
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    public AudioSource getPreviousSong(AudioSource currentSong) {
        if (playlistFilesView == null) {
            return null;
        }

        int n = playlistFilesView.size();
        for (int i = 0; i < n; i++) {
            try {
                AudioSource f1 = playlistFilesView.get(i);
                if (currentSong.equals(f1)) {
                    for (int j = i - 1; j >= 0; j--) {
                        AudioSource file = playlistFilesView.get(j);
                        if (isPlayableFile(file)) {
                            return file;
                        }
                    }
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    private AudioSource findRandomSongFile(AudioSource excludeFile) {
        if (playlistFilesView == null) {
            return null;
        }
        int n = playlistFilesView.size();
        if (n == 0 ) {
        	return null;
        } else if (n == 1) {
            return playlistFilesView.get(0);
        }
        int index = new Random(System.currentTimeMillis()).nextInt(n);

        for (int i = index; i < n; i++) {
            try {
                AudioSource file = playlistFilesView.get(i);

                if (!lastRandomFiles.contains(file) && !file.equals(excludeFile) && isPlayableFile(file)) {
                    return file;
                }
            } catch (Exception e) {
                return null;
            }
        }

        return null;
    }

    public boolean canSeek() {
        if (durationInSeconds != -1) {
            return durationInSeconds > 0;
        }

        return mplayer.getDurationInSecs() > 0;
    }

    public float getDurationInSecs() {
        if (durationInSeconds != -1) {
            return durationInSeconds;
        }

        return mplayer.getDurationInSecs();
    }

	@Override
	public void onUIVolumeChanged(float volume) {
		setVolume(volume);
	}

	@Override
	public void onUISeekToTime(float seconds) {
		seek(seconds);
	}

	@Override
	public void onUIPlayPressed() {
		MediaPlaybackState curState = mplayer.getCurrentState();
    	
    	if (curState == MediaPlaybackState.Playing ||
    		curState == MediaPlaybackState.Paused) {
    		togglePause();
    	} else if (curState == MediaPlaybackState.Closed) {
    		playMedia();
    	}
	}

	@Override
	public void onUIPausePressed() {
		togglePause();
	}

	@Override
	public void onUIFastForwardPressed() {
		fastForward();
	}

	@Override
	public void onUIRewindPressed() {
		rewind();
	}

	@Override
	public void onUIToggleFullscreenPressed() {
		MPlayerMediator.instance().toggleFullScreen();
	}

	@Override
	public void onUIProgressSlideStart() {
		stateNotificationsEnabled = false;
		
		if ( mplayer.getCurrentState() == MediaPlaybackState.Playing ) {
			isPlayPausedForSliding = true;
			mplayer.pause();
		}
	}

	@Override
	public void onUIProgressSlideEnd() {
		if (isPlayPausedForSliding) {
			isPlayPausedForSliding = false;
			mplayer.play();
		}
		
		stateNotificationsEnabled = true;
	}

	@Override
	public void onUIVolumeIncremented() {
		incrementVolume();
	}

	@Override
	public void onUIVolumeDecremented() {
		decrementVolume();
	}

	@Override
	public void onUITogglePlayPausePressed() {
		togglePause();
	}


}