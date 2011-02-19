package com.frostwire.pokki;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.limewire.io.IOUtils;

import com.limegroup.gnutella.MediaType;
import com.limegroup.gnutella.gui.search.GnutellaSearchResult;
import com.limegroup.gnutella.gui.search.SearchInformation;
import com.limegroup.gnutella.gui.search.SearchMediator;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PokkiSearchHandler implements HttpHandler {
    
    public static byte[] searchGUID;
    public static SearchInformation searchInformation;
    public static ArrayList<GnutellaSearchResult> searchResults = new ArrayList<GnutellaSearchResult>();

    @Override
    public void handle(HttpExchange exchange) throws IOException {
        
        searchGUID = null;
        searchInformation = null;
        searchResults.clear();
        
        OutputStream os = null; 
        
        String queryParam = null;
        
        try {
            
            List<NameValuePair> query = URLEncodedUtils.parse(exchange.getRequestURI(), "UTF-8");

            for (NameValuePair item : query) {
                if (item.getName().equals("query")) {
                    queryParam = item.getValue();
                }
            }
            
            exchange.sendResponseHeaders(Code.HTTP_OK, 0);
            
            os = exchange.getResponseBody();
            
            //String xml = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio title="hello" licensetype="creativecommons.org/licenses/"/></audios>
            String xml = "<?xml version=\"1.0\"?><audios xsi:noNamespaceSchemaLocation=\"http://www.limewire.com/schemas/audio.xsd\"><audio title=\"" + queryParam + "\"/></audios>";
            
            searchInformation = SearchInformation.createTitledKeywordSearch(queryParam, xml, MediaType.getAudioMediaType(), queryParam);
            
            searchGUID = SearchMediator.triggerSearch(searchInformation, false);
            
            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                
                if (searchResults.size() > 30) {
                    break;
                }
            }
            
            String prefix = "{\"files\":[";
            String postfix = "]}";

            os.write(prefix.getBytes());

            int size = searchResults.size();

            for (int i = 1; i <= size; i++) {

                GnutellaSearchResult sr = searchResults.get(i);
                
                String json = sr.getFileName() + ":" + sr.getSHA1Urn() + ",     ";
                
                os.write(json.getBytes());

                if (i < size) {
                    os.write(",".getBytes());
                }
            }

            os.write(postfix.getBytes());
            
        } catch (IOException e) {
            System.out.println("Error handling the search");
            e.printStackTrace();
            throw e;
        } finally {
            IOUtils.close(os);
            exchange.close();
        }
    }
}
