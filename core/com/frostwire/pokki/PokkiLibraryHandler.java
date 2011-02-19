package com.frostwire.pokki;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;

import org.limewire.io.IOUtils;
import org.limewire.setting.FileSetting;

import com.frostwire.CoreFrostWireUtils;
import com.frostwire.json.JsonEngine;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.library.DirectoryHolder;
import com.limegroup.gnutella.gui.library.MediaTypeSavedFilesDirectoryHolder;
import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.settings.SharingSettings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PokkiLibraryHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream os = null;

        try {

            exchange.sendResponseHeaders(Code.HTTP_OK, 0);

            os = exchange.getResponseBody();
            
            NamedMediaType nm = NamedMediaType.getFromMediaType(MediaType.getAudioMediaType());
            
            FileSetting fs = SharingSettings.getFileSettingForMediaType(nm.getMediaType());
            DirectoryHolder dh = new MediaTypeSavedFilesDirectoryHolder(fs, nm.getName(), nm.getMediaType());
            
            File[] files = dh.getFiles();
            
            String prefix = "[";
            String postfix = "]";

            os.write(prefix.getBytes());
            
            for (int i = 1; i <= files.length; i++) {
                
                String json = toJSON(files[i-1]);
                
                os.write(json.getBytes());
                
                if (i < files.length) {
                    os.write(",".getBytes());
                }
            }

            os.write(postfix.getBytes());

        } catch (IOException e) {
            System.out.println("Error handling the library");
            e.printStackTrace();
            throw e;
        } finally {
            IOUtils.close(os);
            exchange.close();
        }
    }
    
    public String toJSON(File f) {
        JSONLibraryFile file = new JSONLibraryFile();
        try {
            file.urn = CoreFrostWireUtils.getMD5(f.getName());
            file.name = f.getName().replaceAll("_", " ").replace("  ", " ");
        } catch (Exception e) {
            return null;
        }
        
        return new JsonEngine().toJson(file);
    }
    
    class JSONLibraryFile {
        public String name;
        public String urn;
    }
}
