package com.frostwire.pokki;

import java.io.IOException;
import java.io.OutputStream;

import org.limewire.io.IOUtils;

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

            String prefix = "{\"files\":[";
            String postfix = "]}";

            os.write(prefix.getBytes());

            for (CoreDownloader d : it) {

                long size = d.getContentLength();
                long read = d.getAmountRead();

                double div = (double) read / (double) size;
                int progress = (int) (div * 100);

                String json = d.getFile().getName() + ":" + d.getSha1Urn() + ":" + progress;
                
                os.write(json.getBytes());
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
}
