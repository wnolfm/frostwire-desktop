package org.gudy.azureus2.core3.torrent.impl;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.gudy.azureus2.core3.config.COConfigurationManager;
import org.gudy.azureus2.core3.torrent.TOTorrent;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLGroup;
import org.gudy.azureus2.core3.torrent.TOTorrentAnnounceURLSet;
import org.gudy.azureus2.core3.torrent.TOTorrentException;
import org.gudy.azureus2.core3.torrent.TOTorrentFile;
import org.gudy.azureus2.core3.torrent.TOTorrentListener;
import org.gudy.azureus2.core3.util.AEMonitor;
import org.gudy.azureus2.core3.util.ByteFormatter;
import org.gudy.azureus2.core3.util.HashWrapper;

// ut_metadata
public class TOTorrentMetadata implements TOTorrent {

    private final byte[] hash;
    private final String displayName;
    private List<URL> trackers = new ArrayList<URL>();
    private final String saveLocation;

    private URL announceURL;
    private TOTorrentAnnounceURLGroup announceURLGroup;
    private List<TOTorrentAnnounceURLSet> announceURLSet;

    public TOTorrentMetadata(byte[] hash, String displayName, URL[] trackers) {
        this.hash = hash;
        if (displayName != null) {
            this.displayName = displayName;
        } else {
            this.displayName = ByteFormatter.encodeString(hash);
        }
        String filename = "metadata_" + ByteFormatter.encodeString(hash) + ".torrent";
        this.saveLocation = new File(COConfigurationManager.getStringParameter("General_sDefaultTorrent_Directory"), filename).getAbsolutePath();

        this.announceURL = trackers[0];
        this.announceURLSet = new ArrayList<TOTorrentAnnounceURLSet>();
        this.announceURLGroup = new TOTorrentAnnounceURLGroup() {

            @Override
            public TOTorrentAnnounceURLSet[] getAnnounceURLSets() {
                return announceURLSet.toArray(new TOTorrentAnnounceURLSet[0]);
            }

            @Override
            public void setAnnounceURLSets(TOTorrentAnnounceURLSet[] sets) {
                announceURLSet = Arrays.asList(sets);
            }

            @Override
            public TOTorrentAnnounceURLSet createAnnounceURLSet(final URL[] urls) {
                TOTorrentAnnounceURLSet s = new TOTorrentAnnounceURLSet() {
                    @Override
                    public URL[] getAnnounceURLs() {
                        return urls;
                    }

                    @Override
                    public void setAnnounceURLs(URL[] urls) {
                    }
                };
                announceURLSet.add(s);
                return s;
            }
        };
        this.announceURLGroup.createAnnounceURLSet(trackers);
    }

    @Override
    public void setPrivate(boolean _private) throws TOTorrentException {
    }

    @Override
    public void setPieces(byte[][] pieces) throws TOTorrentException {
    }

    @Override
    public void setHashOverride(byte[] hash) throws TOTorrentException {
    }

    @Override
    public void setCreationDate(long date) {
    }

    @Override
    public void setCreatedBy(byte[] cb) {
    }

    @Override
    public void setComment(String comment) {
    }

    @Override
    public boolean setAnnounceURL(URL url) {
        this.announceURL = url;
        return true;
    }

    @Override
    public void setAdditionalStringProperty(String name, String value) {
    }

    @Override
    public void setAdditionalProperty(String name, Object value) {
    }

    @Override
    public void setAdditionalMapProperty(String name, Map value) {
    }

    @Override
    public void setAdditionalLongProperty(String name, Long value) {
    }

    @Override
    public void setAdditionalListProperty(String name, List value) {
    }

    @Override
    public void setAdditionalByteArrayProperty(String name, byte[] value) {
    }

    @Override
    public void serialiseToXMLFile(File file) throws TOTorrentException {
    }

    @Override
    public Map serialiseToMap() throws TOTorrentException {
        return null;
    }

    @Override
    public void serialiseToBEncodedFile(File file) throws TOTorrentException {
    }

    @Override
    public void removeListener(TOTorrentListener l) {
    }

    @Override
    public void removeAdditionalProperty(String name) {
    }

    @Override
    public void removeAdditionalProperties() {
    }

    @Override
    public void print() {
    }

    @Override
    public boolean isSimpleTorrent() {
        return false;
    }

    @Override
    public boolean isCreated() {
        return false;
    }

    @Override
    public boolean hasSameHashAs(TOTorrent other) {
        try {
            return Arrays.equals(hash, other.getHash());
        } catch (Throwable e) {
            return false;
        }
    }

    @Override
    public String getUTF8Name() {
        return displayName;
    }

    @Override
    public long getSize() {
        return 0;
    }

    @Override
    public boolean getPrivate() {
        return false;
    }

    @Override
    public byte[][] getPieces() throws TOTorrentException {
        return null;
    }

    @Override
    public long getPieceLength() {
        return 0;
    }

    @Override
    public int getNumberOfPieces() {
        return 0;
    }

    @Override
    public byte[] getName() {
        return getUTF8Name().getBytes();
    }

    @Override
    public AEMonitor getMonitor() {
        return null;
    }

    @Override
    public HashWrapper getHashWrapper() throws TOTorrentException {
        return new HashWrapper(hash);
    }

    @Override
    public byte[] getHash() throws TOTorrentException {
        return hash;
    }

    @Override
    public TOTorrentFile[] getFiles() {
        return null;
    }

    @Override
    public long getCreationDate() {
        return 0;
    }

    @Override
    public byte[] getCreatedBy() {
        return null;
    }

    @Override
    public byte[] getComment() {
        return null;
    }

    @Override
    public TOTorrentAnnounceURLGroup getAnnounceURLGroup() {
        return announceURLGroup;
    }

    @Override
    public URL getAnnounceURL() {
        return announceURL;
    }

    @Override
    public String getAdditionalStringProperty(String name) {
        return null;
    }

    @Override
    public Object getAdditionalProperty(String name) {
        return null;
    }

    @Override
    public Map getAdditionalMapProperty(String name) {
        return null;
    }

    @Override
    public Long getAdditionalLongProperty(String name) {
        return null;
    }

    @Override
    public List getAdditionalListProperty(String name) {
        return null;
    }

    @Override
    public byte[] getAdditionalByteArrayProperty(String name) {
        return null;
    }

    @Override
    public void addListener(TOTorrentListener l) {
    }

    public String getSaveLocation() {
        return saveLocation;
    }

    public void notifySaved() {
    }
}
