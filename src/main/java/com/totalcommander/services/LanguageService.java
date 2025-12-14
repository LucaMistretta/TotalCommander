package com.totalcommander.services;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

/**
 * Servizio per gestire le traduzioni e i file di lingua
 */
public class LanguageService {
    
    private static LanguageService instance;
    private Map<String, String> translations;
    private String currentLanguage;
    private static final String LANGUAGES_DIR = "languages";
    private static final String DEFAULT_LANGUAGE = "ITA";
    
    private LanguageService() {
        translations = new HashMap<>();
        currentLanguage = DEFAULT_LANGUAGE;
        loadLanguage(DEFAULT_LANGUAGE);
    }
    
    public static LanguageService getInstance() {
        if (instance == null) {
            instance = new LanguageService();
        }
        return instance;
    }
    
    /**
     * Carica un file di lingua
     */
    public boolean loadLanguage(String languageCode) {
        try {
            String fileName = "APP_" + languageCode.toUpperCase() + ".MNU";
            
            // Prova prima da resources
            InputStream resourceStream = getClass().getClassLoader()
                .getResourceAsStream(LANGUAGES_DIR + "/" + fileName);
            
            if (resourceStream != null) {
                loadFromStream(resourceStream);
                currentLanguage = languageCode.toUpperCase();
                return true;
            }
            
            // Se non trovato in resources, prova dalla directory corrente
            Path filePath = Paths.get(LANGUAGES_DIR, fileName);
            if (Files.exists(filePath)) {
                try (InputStream fileStream = Files.newInputStream(filePath)) {
                    loadFromStream(fileStream);
                    currentLanguage = languageCode.toUpperCase();
                    return true;
                }
            }
            
            // Se non trovato, usa il default
            if (!languageCode.equalsIgnoreCase(DEFAULT_LANGUAGE)) {
                return loadLanguage(DEFAULT_LANGUAGE);
            }
            
        } catch (Exception e) {
            System.err.println("Errore nel caricamento della lingua: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private void loadFromStream(InputStream stream) throws IOException {
        translations.clear();
        Properties props = new Properties();
        
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(stream, "UTF-8"))) {
            
            String line;
            while ((line = reader.readLine()) != null) {
                // Ignora commenti e righe vuote
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                // Parse chiave=valore
                int equalsIndex = line.indexOf('=');
                if (equalsIndex > 0) {
                    String key = line.substring(0, equalsIndex).trim();
                    String value = line.substring(equalsIndex + 1).trim();
                    translations.put(key, value);
                }
            }
        }
    }
    
    /**
     * Ottiene la traduzione per una chiave
     */
    public String getTranslation(String key) {
        return translations.getOrDefault(key, key);
    }
    
    /**
     * Ottiene la traduzione con fallback
     */
    public String getTranslation(String key, String defaultValue) {
        return translations.getOrDefault(key, defaultValue);
    }
    
    /**
     * Ottiene la lingua corrente
     */
    public String getCurrentLanguage() {
        return currentLanguage;
    }
    
    /**
     * Ottiene il percorso del file menu corrente
     */
    public String getMenuFilePath() {
        String fileName = "APP_" + currentLanguage + ".MNU";
        // Prova prima da resources
        try {
            java.net.URL resource = getClass().getClassLoader()
                .getResource(LANGUAGES_DIR + "/" + fileName);
            if (resource != null) {
                return resource.getPath();
            }
        } catch (Exception e) {
            // Ignora
        }
        
        // Fallback alla directory corrente
        return Paths.get(LANGUAGES_DIR, fileName).toAbsolutePath().toString();
    }
    
    /**
     * Elenca tutte le lingue disponibili
     */
    public java.util.List<String> getAvailableLanguages() {
        java.util.List<String> languages = new java.util.ArrayList<>();
        
        // Cerca in resources
        try {
            java.net.URL resourceDir = getClass().getClassLoader().getResource(LANGUAGES_DIR);
            if (resourceDir != null) {
                java.io.File dir = new java.io.File(resourceDir.toURI());
                if (dir.exists() && dir.isDirectory()) {
                    File[] files = dir.listFiles((d, name) -> 
                        name.startsWith("APP_") && name.endsWith(".MNU"));
                    if (files != null) {
                        for (File file : files) {
                            String name = file.getName();
                            String langCode = name.substring(4, name.length() - 4);
                            languages.add(langCode);
                        }
                    }
                }
            }
        } catch (Exception e) {
            // Ignora
        }
        
        // Cerca anche nella directory corrente
        try {
            Path languagesPath = Paths.get(LANGUAGES_DIR);
            if (Files.exists(languagesPath) && Files.isDirectory(languagesPath)) {
                Files.list(languagesPath)
                    .filter(path -> path.getFileName().toString().startsWith("APP_") &&
                                   path.getFileName().toString().endsWith(".MNU"))
                    .forEach(path -> {
                        String name = path.getFileName().toString();
                        String langCode = name.substring(4, name.length() - 4);
                        if (!languages.contains(langCode)) {
                            languages.add(langCode);
                        }
                    });
            }
        } catch (Exception e) {
            // Ignora
        }
        
        return languages;
    }
}

