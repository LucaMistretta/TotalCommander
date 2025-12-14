package com.totalcommander.services;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Servizio per salvare e caricare le impostazioni dell'applicazione
 */
public class SettingsService {
    
    private static SettingsService instance;
    private static final String CONFIG_DIR = System.getProperty("user.home") + File.separator + ".totalcommander";
    private static final String SETTINGS_FILE = CONFIG_DIR + File.separator + "settings.json";
    private static final Gson gson = new GsonBuilder()
        .setPrettyPrinting()
        .serializeSpecialFloatingPointValues() // Permette NaN e Infinity
        .create();
    
    private ApplicationSettings settings;
    
    static {
        // Crea directory config se non esiste
        try {
            Files.createDirectories(Paths.get(CONFIG_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    private SettingsService() {
        settings = loadSettings();
    }
    
    public static SettingsService getInstance() {
        if (instance == null) {
            instance = new SettingsService();
        }
        return instance;
    }
    
    /**
     * Classe per rappresentare tutte le impostazioni dell'applicazione
     */
    public static class ApplicationSettings {
        // Impostazioni lingua
        public String language = "ITA";
        
        // Impostazioni finestra
        public double windowX = -1; // -1 indica posizione di default
        public double windowY = -1;
        public double windowWidth = 1200;
        public double windowHeight = 700;
        public boolean windowMaximized = false;
        
        // Impostazioni pannelli
        public String leftPanelPath = "";
        public String rightPanelPath = "";
        
        // Impostazioni FTP
        public String ftpTransmissionMode = "Binaria (archivi, doc ecc.)";
        
        // Altre impostazioni future possono essere aggiunte qui
        
        public ApplicationSettings() {}
    }
    
    /**
     * Carica le impostazioni dal file
     */
    private ApplicationSettings loadSettings() {
        File file = new File(SETTINGS_FILE);
        if (!file.exists() || file.length() == 0) {
            return new ApplicationSettings();
        }
        
        try (Reader reader = new FileReader(file)) {
            ApplicationSettings loaded = gson.fromJson(reader, ApplicationSettings.class);
            return loaded != null ? loaded : new ApplicationSettings();
        } catch (IOException e) {
            System.err.println("Errore nel caricamento delle impostazioni: " + e.getMessage());
            e.printStackTrace();
            return new ApplicationSettings();
        }
    }
    
    /**
     * Salva le impostazioni su file
     */
    public void saveSettings() {
        synchronized (SettingsService.class) {
            try {
                // Scrive prima in un file temporaneo per evitare corruzioni
                File tempFile = new File(SETTINGS_FILE + ".tmp");
                try (Writer writer = new FileWriter(tempFile)) {
                    gson.toJson(settings, writer);
                }
                
                // Sostituisce il file originale con quello temporaneo
                File originalFile = new File(SETTINGS_FILE);
                if (originalFile.exists()) {
                    originalFile.delete();
                }
                tempFile.renameTo(originalFile);
                
            } catch (IOException e) {
                System.err.println("Errore nel salvataggio delle impostazioni: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Ottiene le impostazioni correnti
     */
    public ApplicationSettings getSettings() {
        return settings;
    }
    
    /**
     * Imposta la lingua
     */
    public void setLanguage(String languageCode) {
        settings.language = languageCode;
        saveSettings();
    }
    
    /**
     * Imposta la posizione e dimensione della finestra
     */
    public void setWindowBounds(double x, double y, double width, double height, boolean maximized) {
        // Valida i valori per evitare NaN o valori non validi
        // Permetti anche coordinate negative (alcuni sistemi possono avere finestre parzialmente fuori schermo)
        settings.windowX = Double.isNaN(x) ? -1 : x;
        settings.windowY = Double.isNaN(y) ? -1 : y;
        settings.windowWidth = (Double.isNaN(width) || width <= 0) ? 1200 : width;
        settings.windowHeight = (Double.isNaN(height) || height <= 0) ? 700 : height;
        settings.windowMaximized = maximized;
        saveSettings();
    }
    
    /**
     * Imposta i percorsi dei pannelli
     */
    public void setPanelPaths(String leftPath, String rightPath) {
        settings.leftPanelPath = leftPath;
        settings.rightPanelPath = rightPath;
        saveSettings();
    }
    
    /**
     * Imposta la modalità di trasmissione FTP
     */
    public void setFtpTransmissionMode(String mode) {
        settings.ftpTransmissionMode = mode;
        saveSettings();
    }
}

