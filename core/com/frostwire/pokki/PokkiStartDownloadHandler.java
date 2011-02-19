package com.frostwire.pokki;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.GUID;
import com.limegroup.gnutella.URN;
import com.limegroup.gnutella.gui.search.GnutellaSearchResult;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PokkiStartDownloadHandler implements HttpHandler {

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
            
            GnutellaSearchResult[] results = PokkiSearchHandler.searchResults.toArray(new GnutellaSearchResult[0]);
            GnutellaSearchResult sr = null;
            
            URN urn = URN.createSHA1Urn(urnParam);
            
            for (int i = 0; i < results.length; i++) {
                if (results[i].getSHA1Urn().equals(urn)) {
                    sr = results[i];
                }
            }
            
            byte[] guid = PokkiSearchHandler.searchGUID;
            SearchInformation searchInfo = PokkiSearchHandler.searchInformation;
            
            SearchMediator.downloadGnutellaLine(sr, new GUID(guid), searchInfo);

        } catch (IOException e) {
            System.out.println("Error handling the start download");
            e.printStackTrace();
            throw e;
        } finally {
            IOUtils.close(os);
            exchange.close();
        }
    }
}
