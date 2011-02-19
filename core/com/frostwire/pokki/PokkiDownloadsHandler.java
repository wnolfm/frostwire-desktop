package com.frostwire.pokki;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;
import java.util.Iterator;

import org.limewire.io.IOUtils;

import com.frostwire.json.JsonEngine;
import com.limegroup.gnutella.downloader.CoreDownloader;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PokkiDownloadsHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream os = null;

        try {

            exchange.sendResponseHeaders(Code.HTTP_OK, 0);

            os = exchange.getResponseBody();

            Iterable<CoreDownloader> it = GuiCoreMediator.getDownloadManager().getAllDownloaders();

            String prefix = "[";
            String postfix = "]";

            os.write(prefix.getBytes());
            
            Iterator<CoreDownloader> itt = it.iterator();

            while (itt.hasNext()) {
                
                CoreDownloader d = itt.next();
                
                String json = getJson(d);

                os.write(json.getBytes());
                
                if (itt.hasNext()) {
                    os.write(",".getBytes());
                }
            }

            os.write(postfix.getBytes());

        } catch (IOException e) {
            System.out.println("Error handling the downloads");
            e.printStackTrace();
            throw e;
        } finally {
            IOUtils.close(os);
            exchange.close();
        }
    }
    
    private String getJson(CoreDownloader d) {
        
        JSONDownloadStatus json = new JSONDownloadStatus();
        
        json.urn = d.getSha1Urn().toString().replace("urn:sha1:", "");
        json.name = d.getFile().getName();
        json.status = d.getState().toString();
        
        long size = d.getContentLength();
        long read = d.getAmountRead();

        double div = (double) read / (double) size;
        int progress = (int) (div * 100);
        
        json.progress = progress + "%";

        return new JsonEngine().toJson(json);
    }

    public class JSONDownloadStatus {
        public String urn;
        public String name;
        public String status;
        public String progress;
    }
}
