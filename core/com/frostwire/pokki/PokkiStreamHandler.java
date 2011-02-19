package com.frostwire.pokki;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.limewire.io.IOUtils;
import org.limewire.setting.FileSetting;

import com.frostwire.CoreFrostWireUtils;
import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.gui.library.DirectoryHolder;
import com.limegroup.gnutella.gui.library.MediaTypeSavedFilesDirectoryHolder;
import com.limegroup.gnutella.gui.search.NamedMediaType;
import com.limegroup.gnutella.settings.SharingSettings;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PokkiStreamHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {

        OutputStream os = null;
        FileInputStream fis = null;
        
        String urnParam = null;

        try {

            List<NameValuePair> query = URLEncodedUtils.parse(exchange.getRequestURI(), "UTF-8");

            for (NameValuePair item : query) {
                if (item.getName().equals("urn")) {
                    urnParam = item.getValue();
                }
            }
            
            File file = getLibraryFile(urnParam);
            //exchange.getResponseHeaders().add("Content-Type", "audio/mpeg");
            //exchange.getResponseHeaders().add("Content-Transfer-Encoding", "binary");
            //exchange.getResponseHeaders().add("Connection", "close");
            exchange.sendResponseHeaders(Code.HTTP_OK, file.length());

            os = exchange.getResponseBody();

            fis = new FileInputStream(file.getAbsolutePath());

            byte[] buffer = new byte[4 * 1024];
            int n;

            while ((n = fis.read(buffer, 0, buffer.length)) != -1) {
                os.write(buffer, 0, n);
            }
        
        } catch (IOException e) {
            System.out.println("Error downloading file id=" + urnParam);
            throw e;
        } finally {
            IOUtils.close(os);
            IOUtils.close(fis);
            exchange.close();
        }
    }

    private File getLibraryFile(String urnParam) {
        NamedMediaType nm = NamedMediaType.getFromMediaType(MediaType.getAudioMediaType());
        
        FileSetting fs = SharingSettings.getFileSettingForMediaType(nm.getMediaType());
        DirectoryHolder dh = new MediaTypeSavedFilesDirectoryHolder(fs, nm.getName(), nm.getMediaType());
        
        File[] files = dh.getFiles();
        
        for (int i = 0; i < files.length; i++) {
            
            String urn = null;
            try {
                urn = CoreFrostWireUtils.getMD5(files[i].getName());
            } catch (Exception e) {
            }
            
            if (urnParam.equals(urn)) {
                return files[i];
            }
        }
        
        return null;
    }
}
