package com.frostwire.pokki;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.Downloader;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.GuiCoreMediator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PokkiCancelDownloadHandler implements HttpHandler {

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        OutputStream os = null;
        
        String urnParam = null;

        try {
            
            List<NameValuePair> query = URLEncodedUtils.parse(exchange.getRequestURI(), "UTF-8");

            for (NameValuePair item : query) {
                if (item.getName().equals("urn")) {
                    urnParam = "urn:sha1:" + item.getValue();
                }
            }

            exchange.sendResponseHeaders(Code.HTTP_OK, 0);

            os = exchange.getResponseBody();
            
            URN urn = URN.createSHA1Urn(urnParam);
            Downloader d = GuiCoreMediator.getDownloadManager().getDownloaderForURN(urn);
            
            d.stop();
        } catch (IOException e) {
            System.out.println("Error handling the cancel download");
            e.printStackTrace();
            throw e;
        } finally {
            IOUtils.close(os);
            exchange.close();
        }
    }
}
