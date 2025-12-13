package com.totalcommander.services;

import com.totalcommander.models.TransferTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.MalformedJsonException;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Servizio per salvare e caricare log dei trasferimenti
 */
public class TransferLogService {
    
    private static final String LOG_DIR = System.getProperty("user.home") + File.separator + ".totalcommander" + File.separator + "transfers";
    private static final String LOG_FILE = LOG_DIR + File.separator + "transfer_log.json";
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();
    
    static {
        try {
            Files.createDirectories(Paths.get(LOG_DIR));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static class TransferLogEntry {
        public String sourcePath;
        public String destinationPath;
        public String fileName;
        public String transferType;
        public long bytesTransferred;
        public long totalBytes;
        public String status;
        public boolean completed;
        public boolean failed;
        public String errorMessage;
        public long timestamp;
        
        public TransferLogEntry() {}
        
        public TransferLogEntry(TransferTask task) {
            this.sourcePath = task.getSourcePath();
            this.destinationPath = task.getDestinationPath();
            this.fileName = task.getFileName();
            this.transferType = task.getTransferType();
            this.bytesTransferred = task.getBytesTransferred();
            this.totalBytes = task.getTotalBytes();
            this.status = task.getStatus();
            this.completed = task.isCompleted();
            this.failed = task.isFailed();
            this.errorMessage = task.getErrorMessage();
            this.timestamp = System.currentTimeMillis();
        }
    }
    
    public static void saveTransfer(TransferTask task) {
        synchronized (TransferLogService.class) {
            try {
                List<TransferLogEntry> logs = loadAllTransfers();
                
                // Rimuovi entry vecchie (più di 24 ore)
                long dayAgo = System.currentTimeMillis() - (24 * 60 * 60 * 1000);
                logs.removeIf(log -> log.timestamp < dayAgo && log.completed);
                
                // Aggiungi o aggiorna entry corrente
                TransferLogEntry entry = new TransferLogEntry(task);
                logs.removeIf(log -> log.sourcePath.equals(entry.sourcePath) && 
                                  log.destinationPath.equals(entry.destinationPath) &&
                                  !log.completed && !log.failed);
                logs.add(entry);
                
                // Salva in un file temporaneo prima di sostituire quello originale
                String tempFile = LOG_FILE + ".tmp";
                try (Writer writer = new FileWriter(tempFile)) {
                    gson.toJson(logs, writer);
                    writer.flush();
                }
                
                // Sostituisci il file originale solo se la scrittura è riuscita
                File temp = new File(tempFile);
                File original = new File(LOG_FILE);
                if (temp.exists() && temp.length() > 0) {
                    if (original.exists()) {
                        original.delete();
                    }
                    temp.renameTo(original);
                }
            } catch (IOException e) {
                System.err.println("Errore durante il salvataggio del log di trasferimento: " + e.getMessage());
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Errore imprevisto durante il salvataggio del log: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    public static List<TransferLogEntry> loadAllTransfers() {
        synchronized (TransferLogService.class) {
            File file = new File(LOG_FILE);
            if (!file.exists() || file.length() == 0) {
                return new ArrayList<>();
            }
            
            // Prova prima con il parser normale
            try (FileReader fileReader = new FileReader(file);
                 JsonReader jsonReader = new JsonReader(fileReader)) {
                
                jsonReader.setLenient(false);
                
                List<TransferLogEntry> logs = gson.fromJson(jsonReader, 
                    new com.google.gson.reflect.TypeToken<List<TransferLogEntry>>(){}.getType());
                return logs != null ? logs : new ArrayList<>();
                
            } catch (MalformedJsonException | JsonSyntaxException e) {
                // Se il JSON è malformato, prova con lenient mode
                System.err.println("JSON malformato rilevato, tentativo di recupero con lenient mode: " + e.getMessage());
                try (FileReader fileReader = new FileReader(file);
                     JsonReader jsonReader = new JsonReader(fileReader)) {
                    
                    jsonReader.setLenient(true);
                    
                    List<TransferLogEntry> logs = gson.fromJson(jsonReader, 
                        new com.google.gson.reflect.TypeToken<List<TransferLogEntry>>(){}.getType());
                    return logs != null ? logs : new ArrayList<>();
                    
                } catch (Exception e2) {
                    // Se anche con lenient mode fallisce, crea un backup e ricomincia
                    System.err.println("Impossibile recuperare il file JSON corrotto, creazione backup e reset: " + e2.getMessage());
                    try {
                        String backupFile = LOG_FILE + ".backup." + System.currentTimeMillis();
                        Files.copy(file.toPath(), Paths.get(backupFile));
                        file.delete();
                    } catch (IOException backupEx) {
                        System.err.println("Errore durante la creazione del backup: " + backupEx.getMessage());
                    }
                    return new ArrayList<>();
                }
            } catch (IOException e) {
                System.err.println("Errore durante la lettura del file di log: " + e.getMessage());
                return new ArrayList<>();
            } catch (Exception e) {
                System.err.println("Errore imprevisto durante il caricamento dei log: " + e.getMessage());
                e.printStackTrace();
                return new ArrayList<>();
            }
        }
    }
    
    public static TransferLogEntry findIncompleteTransfer(String sourcePath, String destinationPath) {
        List<TransferLogEntry> logs = loadAllTransfers();
        for (TransferLogEntry log : logs) {
            if (log.sourcePath.equals(sourcePath) && 
                log.destinationPath.equals(destinationPath) &&
                !log.completed && !log.failed) {
                return log;
            }
        }
        return null;
    }
}

