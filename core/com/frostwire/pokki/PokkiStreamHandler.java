package com.frostwire.pokki;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

public class PokkiStreamHandler implements HttpHandler {

    public void handle(HttpExchange exchange) throws IOException {

        OutputStream os = null;
        FileInputStream fis = null;

        try {

            List<NameValuePair> query = URLEncodedUtils.parse(exchange.getRequestURI(), "UTF-8");

            for (NameValuePair item : query) {
                if (item.getName().equals("id")) {
                    //id = Byte.parseByte(item.getValue());
                }
            }
//            exchange.getResponseHeaders().add("Content-Type", fileDescriptor.mime);
//            exchange.sendResponseHeaders(Code.HTTP_OK, fileDescriptor.fileSize);
//
//            os = exchange.getResponseBody();
//
//            fis = new FileInputStream(path);

            byte[] buffer = new byte[4 * 1024];
            int n;

            while ((n = fis.read(buffer, 0, buffer.length)) != -1) {
                os.write(buffer, 0, n);
            }
        
        } catch (IOException e) {
            //System.out.println("Error downloading file id=" + id);
            throw e;
        } finally {
            //UIUtils.close(os);
            //UIUtils.close(fis);
            exchange.close();
        }
    }
}
