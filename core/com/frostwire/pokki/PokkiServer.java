package com.frostwire.pokki;

import java.net.InetSocketAddress;

import com.sun.net.httpserver.HttpServer;

public class PokkiServer {

    private HttpServer _httpServer;

    public void start() {

        try {
            
            _httpServer = HttpServer.create(new InetSocketAddress(8787), 10);
            
            _httpServer.createContext("/search", new PokkiSearchHandler());
            _httpServer.createContext("/library", new PokkiLibraryHandler());
            _httpServer.createContext("/downloads", new PokkiDownloadsHandler());
            _httpServer.createContext("/startDownload", new PokkiStartDownloadHandler());
            _httpServer.createContext("/cancelDownload", new PokkiCancelDownloadHandler());
            _httpServer.createContext("/stream", new PokkiStreamHandler());
            
            _httpServer.start();
            
            System.out.println("Pokki server started");
            
        } catch (Exception e) {
            System.out.println("Failed to start Pokki server");
            e.printStackTrace();
        }
    }
}
