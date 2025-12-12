package com.totalcommander.services;

import com.totalcommander.models.TransferTask;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
            
            // Salva
            try (Writer writer = new FileWriter(LOG_FILE)) {
                gson.toJson(logs, writer);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static List<TransferLogEntry> loadAllTransfers() {
        File file = new File(LOG_FILE);
        if (!file.exists()) {
            return new ArrayList<>();
        }
        
        try (Reader reader = new FileReader(file)) {
            List<TransferLogEntry> logs = gson.fromJson(reader, 
                new com.google.gson.reflect.TypeToken<List<TransferLogEntry>>(){}.getType());
            return logs != null ? logs : new ArrayList<>();
        } catch (IOException e) {
            e.printStackTrace();
            return new ArrayList<>();
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

