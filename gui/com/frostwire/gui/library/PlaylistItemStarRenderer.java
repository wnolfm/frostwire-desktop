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

package com.frostwire.gui.library;

import java.awt.Component;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JTable;

import org.pushingpixels.substance.api.renderers.SubstanceDefaultTableCellRenderer;

import com.limegroup.gnutella.gui.GUIMediator;

/**
 *  Creates both a renderer and an editor for cells in the playlist table that display the name
 *  of the file being played.
 *  
 *  @author gubatron
 *  @author aldenml
 */
class PlaylistItemStarRenderer extends SubstanceDefaultTableCellRenderer {

    private static final long serialVersionUID = 6800146830099830381L;

    private static final Icon starOn;
    private static final Icon starOff;
    private static final Icon speaker;
    private static final Icon exclamation;

    static {
        starOn = GUIMediator.getThemeImage("star_on");
        starOff = GUIMediator.getThemeImage("star_off");
        speaker = GUIMediator.getThemeImage("speaker");
        exclamation = GUIMediator.getThemeImage("exclamation");
    }

    public PlaylistItemStarRenderer() {
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        final LibraryPlaylistsTableDataLine line = ((PlaylistItemStar) value).getLine();
        final PlaylistItemStar cell = (PlaylistItemStar) value;

        setIcon(cell.isPlaying(), line.getPlayListItem().isStarred(), cell.exists());

        final JLabel component = (JLabel) super.getTableCellRendererComponent(table, null, isSelected, hasFocus, row, column);
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                setIcon(false, line.getPlayListItem().isStarred(), cell.exists());
            }

            @Override
            public void mouseExited(MouseEvent e) {
                setIcon(cell.isPlaying(), line.getPlayListItem().isStarred(), cell.exists());
            }
        });

        return component;
    }

    private void setIcon(boolean playing, boolean starred, boolean exists) {
        if (!exists) {
            setIcon(exclamation);
        } else if (playing) {
            setIcon(speaker);
        } else if (starred) {
            setIcon(starOn);
        } else {
            setIcon(starOff);
        }
    }
}