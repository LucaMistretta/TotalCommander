package com.totalcommander.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.totalcommander.models.FtpConnection;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Gestore per salvare e caricare connessioni FTP
 * Mantiene anche le connessioni attive
 */
public class FtpConnectionManager {
    
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".totalcommander";
    private static final String CONNECTIONS_FILE = CONFIG_DIR + File.separator + "ftp_connections.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    // Mappa per mantenere le connessioni FTP attive
    private static final Map<String, FtpService> activeConnections = new HashMap<>();

    static {
        // Crea directory config se non esiste
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveConnection(FtpConnection connection) {
        List<FtpConnection> connections = loadConnections();
        // Rimuovi connessioni duplicate
        connections.removeIf(c -> c.getName().equals(connection.getName()) && 
                                c.getHost().equals(connection.getHost()));
        connections.add(connection);
        saveConnections(connections);
    }

    public static List<FtpConnection> loadConnections() {
        File file = new File(CONNECTIONS_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }

        try (Reader reader = new FileReader(file)) {
            List<FtpConnection> connections = gson.fromJson(reader, new TypeToken<List<FtpConnection>>(){}.getType());
            return connections != null ? connections : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    public static void deleteConnection(FtpConnection connection) {
        List<FtpConnection> connections = loadConnections();
        connections.removeIf(c -> c.getName().equals(connection.getName()) && 
                                c.getHost().equals(connection.getHost()));
        saveConnections(connections);
        
        // Disconnetti se la connessione è attiva
        String key = getConnectionKey(connection);
        if (activeConnections.containsKey(key)) {
            FtpService service = activeConnections.remove(key);
            service.disconnect();
        }
    }

    private static void saveConnections(List<FtpConnection> connections) {
        try (Writer writer = new FileWriter(CONNECTIONS_FILE)) {
            gson.toJson(connections, writer);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Connetti a un server FTP e mantieni la connessione attiva
     */
    public static FtpService connect(FtpConnection connection) {
        String key = getConnectionKey(connection);
        
        // Se già connesso, ritorna il servizio esistente
        if (activeConnections.containsKey(key)) {
            FtpService service = activeConnections.get(key);
            if (service.isConnected()) {
                return service;
            } else {
                // Rimuovi connessione non più valida
                activeConnections.remove(key);
            }
        }
        
        // Crea nuova connessione
        FtpService service = new FtpService();
        if (service.connect(connection.getHost(), connection.getPort(), 
                           connection.getUsername(), connection.getPassword(),
                           connection.isUsePassiveMode())) {
            activeConnections.put(key, service);
            return service;
        }
        
        return null;
    }
    
    /**
     * Disconnetti da un server FTP
     */
    public static void disconnect(FtpConnection connection) {
        String key = getConnectionKey(connection);
        FtpService service = activeConnections.remove(key);
        if (service != null) {
            service.disconnect();
        }
    }
    
    /**
     * Ottieni una connessione attiva
     */
    public static FtpService getActiveConnection(FtpConnection connection) {
        String key = getConnectionKey(connection);
        return activeConnections.get(key);
    }
    
    /**
     * Genera una chiave univoca per la connessione
     */
    private static String getConnectionKey(FtpConnection connection) {
        return connection.getHost() + ":" + connection.getPort() + "@" + connection.getUsername();
    }
    
    /**
     * Disconnetti tutte le connessioni attive
     */
    public static void disconnectAll() {
        for (FtpService service : activeConnections.values()) {
            service.disconnect();
        }
        activeConnections.clear();
    }
}
