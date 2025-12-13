package com.totalcommander.services;

import com.totalcommander.models.TransferTask;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import org.apache.commons.net.ftp.FTPFile;
import java.io.*;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * Servizio per gestire trasferimenti file in background con coda sequenziale
 */
public class TransferService {
    
    private final ObservableList<TransferTask> activeTransfers = FXCollections.observableArrayList();
    private final Queue<TransferItem> transferQueue = new LinkedList<>();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();
    private boolean isProcessing = false;
    private BiConsumer<String, String> statusCallback;
    private FtpService currentFtpService; // Servizio FTP corrente
    private Runnable onAllTransfersComplete; // Callback quando tutti i trasferimenti sono completati
    
    /**
     * Classe per rappresentare un elemento nella coda di trasferimento
     */
    private static class TransferItem {
        final String sourcePath;      // Percorso assoluto sorgente
        final String destinationPath;  // Percorso assoluto destinazione
        final boolean isDirectory;     // Se è una directory
        final String fileName;         // Nome del file/cartella
        final String transferType;     // "Upload" o "Download"
        
        TransferItem(String sourcePath, String destinationPath, boolean isDirectory, 
                    String fileName, String transferType) {
            this.sourcePath = sourcePath;
            this.destinationPath = destinationPath;
            this.isDirectory = isDirectory;
            this.fileName = fileName;
            this.transferType = transferType;
        }
    }
    
    public void setStatusCallback(BiConsumer<String, String> callback) {
        this.statusCallback = callback;
    }
    
    public void setOnAllTransfersComplete(Runnable callback) {
        this.onAllTransfersComplete = callback;
    }
    
    private void notifyStatus(String type, String message) {
        if (statusCallback != null) {
            javafx.application.Platform.runLater(() -> statusCallback.accept(type, message));
        }
    }
    
    public ObservableList<TransferTask> getActiveTransfers() {
        return activeTransfers;
    }
    
    /**
     * Aggiunge file/cartelle alla coda per download (FTP -> Locale)
     */
    public void queueDownload(List<String> remotePaths, File localDestinationDir, FtpService ftpService) {
        this.currentFtpService = ftpService;
        for (String remotePath : remotePaths) {
            queueDownloadItem(remotePath, localDestinationDir, ftpService);
        }
        startProcessing();
    }
    
    /**
     * Aggiunge file/cartelle alla coda per upload (Locale -> FTP)
     */
    public void queueUpload(List<File> localFiles, String remoteDestinationPath, FtpService ftpService) {
        this.currentFtpService = ftpService;
        for (File localFile : localFiles) {
            queueUploadItem(localFile, remoteDestinationPath, ftpService);
        }
        startProcessing();
    }
    
    /**
     * Aggiunge un elemento remoto alla coda di download
     */
    private void queueDownloadItem(String remotePath, File localDestinationDir, FtpService ftpService) {
        try {
            // Normalizza il percorso remoto
            String normalizedRemotePath = remotePath;
            if (!normalizedRemotePath.startsWith("/")) {
                normalizedRemotePath = "/" + normalizedRemotePath;
            }
            
            // Estrai il nome del file/cartella
            String fileName = normalizedRemotePath.substring(normalizedRemotePath.lastIndexOf("/") + 1);
            if (fileName.isEmpty()) {
                fileName = "root";
            }
            
            // Determina se è un file o una directory
            FTPFile[] files = ftpService.listFiles(normalizedRemotePath);
            boolean isDirectory = false;
            
            if (files != null && files.length > 0) {
                if (files.length > 1) {
                    // Più elementi = directory
                    isDirectory = true;
                } else {
                    // Un elemento: controlla se è directory o file
                    FTPFile ftpFile = files[0];
                    isDirectory = ftpFile.isDirectory();
                }
            } else {
                // Se listFiles restituisce vuoto, prova a verificare nella directory parent
                try {
                    String parentPath = normalizedRemotePath.substring(0, normalizedRemotePath.lastIndexOf("/"));
                    if (parentPath.isEmpty()) {
                        parentPath = "/";
                    }
                    FTPFile[] parentFiles = ftpService.listFiles(parentPath);
                    if (parentFiles != null) {
                        for (FTPFile f : parentFiles) {
                            if (f.getName().equals(fileName)) {
                                isDirectory = f.isDirectory();
                                break;
                            }
                        }
                    }
                } catch (Exception e) {
                    // Se non riesce a determinare, assume che sia un file
                }
            }
            
            File localDestination = new File(localDestinationDir, fileName);
            
            if (isDirectory) {
                // Aggiungi la directory e poi esplora ricorsivamente
                transferQueue.offer(new TransferItem(normalizedRemotePath, localDestination.getAbsolutePath(), 
                    true, fileName, "Download"));
                
                // Esplora ricorsivamente la directory
                exploreRemoteDirectory(normalizedRemotePath, localDestination, ftpService);
            } else {
                // Aggiungi il file alla coda
                transferQueue.offer(new TransferItem(normalizedRemotePath, localDestination.getAbsolutePath(), 
                    false, fileName, "Download"));
            }
        } catch (Exception e) {
            System.err.println("Errore nell'aggiungere alla coda: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Esplora ricorsivamente una directory remota e aggiunge tutti i file alla coda
     */
    private void exploreRemoteDirectory(String remoteDirPath, File localDir, FtpService ftpService) {
        try {
            // Normalizza il percorso
            String normalizedPath = remoteDirPath;
            if (!normalizedPath.endsWith("/")) {
                normalizedPath = normalizedPath + "/";
            }
            
            FTPFile[] files = ftpService.listFiles(normalizedPath);
            if (files != null) {
                for (FTPFile ftpFile : files) {
                    String fileName = ftpFile.getName();
                    if (fileName.equals(".") || fileName.equals("..")) {
                        continue;
                    }
                    
                    String childRemotePath = normalizedPath + fileName;
                    File childLocalFile = new File(localDir, fileName);
                    
                    if (ftpFile.isDirectory()) {
                        // Aggiungi la directory alla coda
                        transferQueue.offer(new TransferItem(childRemotePath, childLocalFile.getAbsolutePath(), 
                            true, fileName, "Download"));
                        // Esplora ricorsivamente
                        exploreRemoteDirectory(childRemotePath, childLocalFile, ftpService);
                    } else {
                        // Aggiungi il file alla coda
                        transferQueue.offer(new TransferItem(childRemotePath, childLocalFile.getAbsolutePath(), 
                            false, fileName, "Download"));
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Errore nell'esplorazione directory remota: " + e.getMessage());
        }
    }
    
    /**
     * Aggiunge un elemento locale alla coda di upload
     */
    private void queueUploadItem(File localFile, String remoteDestinationPath, FtpService ftpService) {
        try {
            // Normalizza il percorso remoto
            String normalizedRemotePath = remoteDestinationPath;
            if (!normalizedRemotePath.endsWith("/") && !normalizedRemotePath.isEmpty()) {
                normalizedRemotePath = normalizedRemotePath + "/";
            }
            if (normalizedRemotePath.isEmpty()) {
                normalizedRemotePath = "/";
            }
            
            String remotePath = normalizedRemotePath + localFile.getName();
            String localPath = localFile.getAbsolutePath();
            
            if (localFile.isDirectory()) {
                // Aggiungi la directory e poi esplora ricorsivamente
                transferQueue.offer(new TransferItem(localPath, remotePath, true, localFile.getName(), "Upload"));
                
                // Esplora ricorsivamente la directory locale
                exploreLocalDirectory(localFile, remotePath, ftpService);
            } else {
                // Aggiungi il file alla coda
                transferQueue.offer(new TransferItem(localPath, remotePath, false, localFile.getName(), "Upload"));
            }
        } catch (Exception e) {
            System.err.println("Errore nell'aggiungere alla coda: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Esplora ricorsivamente una directory locale e aggiunge tutti i file alla coda
     */
    private void exploreLocalDirectory(File localDir, String remoteDirPath, FtpService ftpService) {
        File[] files = localDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String remotePath = remoteDirPath.endsWith("/") ? 
                    remoteDirPath + file.getName() : 
                    remoteDirPath + "/" + file.getName();
                
                if (file.isDirectory()) {
                    // Aggiungi la directory alla coda
                    transferQueue.offer(new TransferItem(file.getAbsolutePath(), remotePath, 
                        true, file.getName(), "Upload"));
                    // Esplora ricorsivamente
                    exploreLocalDirectory(file, remotePath, ftpService);
                } else {
                    // Aggiungi il file alla coda
                    transferQueue.offer(new TransferItem(file.getAbsolutePath(), remotePath, 
                        false, file.getName(), "Upload"));
                }
            }
        }
    }
    
    /**
     * Avvia il processing della coda se non è già in corso
     */
    private void startProcessing() {
        if (!isProcessing) {
            isProcessing = true;
            executorService.submit(this::processQueue);
        }
    }
    
    /**
     * Processa la coda sequenzialmente, un file alla volta
     */
    private void processQueue() {
        int maxIterations = 10000; // Limite di sicurezza per evitare loop infiniti
        int iteration = 0;
        
        while (!transferQueue.isEmpty() && iteration < maxIterations) {
            iteration++;
            TransferItem item = transferQueue.poll();
            if (item == null) {
                break;
            }
            
            // Crea il task per la visualizzazione
            TransferTask task = new TransferTask(item.sourcePath, item.destinationPath, 
                item.fileName, item.transferType);
            
            // Aggiungi il task immediatamente alla lista sulla JavaFX thread
            javafx.application.Platform.runLater(() -> {
                activeTransfers.add(task);
            });
            
            try {
                // Salva il log in modo asincrono per non bloccare
                try {
                    TransferLogService.saveTransfer(task);
                } catch (Exception logEx) {
                    // Ignora errori di log, non bloccanti
                    System.err.println("Errore nel salvataggio log: " + logEx.getMessage());
                }
                
                if (item.isDirectory) {
                    // Crea la directory
                    if (item.transferType.equals("Download")) {
                        // Download: crea directory locale
                        File localDir = new File(item.destinationPath);
                        if (!localDir.exists()) {
                            boolean created = localDir.mkdirs();
                            if (!created && !localDir.exists()) {
                                throw new IOException("Impossibile creare directory locale: " + item.destinationPath);
                            }
                            javafx.application.Platform.runLater(() -> {
                                task.setStatus("Directory creata: " + item.fileName);
                            });
                        } else {
                            javafx.application.Platform.runLater(() -> {
                                task.setStatus("Directory già esistente: " + item.fileName);
                            });
                        }
                    } else {
                        // Upload: crea directory remota
                        FtpService ftpService = getFtpServiceForItem(item);
                        if (ftpService != null) {
                            // Verifica se la directory esiste già (con timeout implicito)
                            try {
                                // Usa un timeout per evitare blocchi
                                FTPFile[] files = null;
                                try {
                                    files = ftpService.listFiles(item.destinationPath);
                                } catch (Exception listEx) {
                                    // Se listFiles fallisce, ignora e prova a creare
                                    System.err.println("Errore listFiles: " + listEx.getMessage());
                                }
                                
                                if (files != null) {
                                    // Directory esiste già
                                    task.setStatus("Directory remota già esistente: " + item.fileName);
                                } else {
                                    // Prova a crearla
                                    if (!ftpService.createDirectory(item.destinationPath)) {
                                        // Potrebbe esistere già, non è un errore critico
                                        task.setStatus("Directory remota: " + item.fileName);
                                    } else {
                                        task.setStatus("Directory remota creata: " + item.fileName);
                                    }
                                }
                            } catch (Exception e) {
                                // Se listFiles fallisce, prova comunque a creare
                                try {
                                    if (!ftpService.createDirectory(item.destinationPath)) {
                                        // Potrebbe esistere già, non è un errore critico
                                        task.setStatus("Directory remota: " + item.fileName);
                                    } else {
                                        task.setStatus("Directory remota creata: " + item.fileName);
                                    }
                                } catch (Exception createEx) {
                                    // Anche la creazione fallisce, ma non blocchiamo
                                    task.setStatus("Directory remota: " + item.fileName + " (errore ignorato)");
                                    System.err.println("Errore creazione directory: " + createEx.getMessage());
                                }
                            }
                        } else {
                            throw new IOException("Servizio FTP non disponibile");
                        }
                    }
                } else {
                    // Trasferisci il file
                    if (item.transferType.equals("Download")) {
                        downloadFile(item.sourcePath, item.destinationPath, task);
                    } else {
                        uploadFile(item.sourcePath, item.destinationPath, task);
                    }
                }
                
                // Aggiorna lo stato sulla JavaFX thread
                javafx.application.Platform.runLater(() -> {
                    task.setProgress(1.0);
                    task.setStatus("Completato");
                    task.setCompleted(true);
                });
                
                try {
                    TransferLogService.saveTransfer(task);
                } catch (Exception logEx) {
                    // Ignora errori di log
                    System.err.println("Errore nel salvataggio log finale: " + logEx.getMessage());
                }
                
                notifyStatus(item.transferType, "Completato: " + item.fileName);
                
            } catch (Exception e) {
                task.setFailed(true, e.getMessage());
                task.setStatus("Errore: " + e.getMessage());
                
                try {
                    TransferLogService.saveTransfer(task);
                } catch (Exception logEx) {
                    // Ignora errori di log
                    System.err.println("Errore nel salvataggio log errore: " + logEx.getMessage());
                }
                
                notifyStatus(item.transferType, "Errore: " + item.fileName + " - " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        if (iteration >= maxIterations) {
            System.err.println("ATTENZIONE: Raggiunto il limite massimo di iterazioni nella coda!");
        }
        
        isProcessing = false;
        
        // Notifica quando tutti i trasferimenti sono completati
        javafx.application.Platform.runLater(() -> {
            boolean allComplete = activeTransfers.stream()
                .allMatch(t -> t.isCompleted() || t.isFailed());
            if (allComplete && onAllTransfersComplete != null) {
                onAllTransfersComplete.run();
            }
        });
    }
    
    /**
     * Download di un singolo file
     */
    private void downloadFile(String remotePath, String localPath, TransferTask task) throws IOException {
        javafx.application.Platform.runLater(() -> {
            task.setStatus("Download in corso...");
        });
        
        File localFile = new File(localPath);
        
        // Crea la directory parent se non esiste
        File parentDir = localFile.getParentFile();
        if (parentDir != null && !parentDir.exists()) {
            parentDir.mkdirs();
        }
        
        // Ottieni il servizio FTP
        FtpService ftpService = getFtpServiceForItem(new TransferItem(remotePath, localPath, false, 
            new File(remotePath).getName(), "Download"));
        
        if (ftpService == null) {
            throw new IOException("Servizio FTP non disponibile");
        }
        
        // Usa il metodo semplice di download
        boolean success = ftpService.downloadFile(remotePath, localFile.toPath());
        
        if (success) {
            if (localFile.exists()) {
                task.setTotalBytes(localFile.length());
                task.setBytesTransferred(localFile.length());
                task.setProgress(1.0);
                task.setStatus("Download completato");
            } else {
                throw new IOException("File scaricato ma non trovato localmente: " + localPath);
            }
        } else {
            // Verifica se il file esiste comunque (potrebbe essere stato scaricato parzialmente)
            if (localFile.exists() && localFile.length() > 0) {
                // File esiste, potrebbe essere stato scaricato correttamente nonostante il return false
                task.setTotalBytes(localFile.length());
                task.setBytesTransferred(localFile.length());
                task.setProgress(1.0);
                task.setStatus("Download completato");
            } else {
                throw new IOException("Download fallito per: " + remotePath);
            }
        }
    }
    
    /**
     * Upload di un singolo file
     */
    private void uploadFile(String localPath, String remotePath, TransferTask task) throws IOException {
        javafx.application.Platform.runLater(() -> {
            task.setStatus("Upload in corso...");
        });
        
        File localFile = new File(localPath);
        if (!localFile.exists() || !localFile.isFile()) {
            throw new IOException("File locale non trovato: " + localPath);
        }
        
        // Ottieni il servizio FTP
        FtpService ftpService = getFtpServiceForItem(new TransferItem(localPath, remotePath, false, 
            localFile.getName(), "Upload"));
        
        if (ftpService == null) {
            throw new IOException("Servizio FTP non disponibile");
        }
        
        // Controlla se il file esiste già sul server
        try {
            FTPFile[] files = ftpService.listFiles(remotePath);
            if (files != null && files.length > 0 && files[0].isFile()) {
                // File esiste già - per ora sovrascrive (potresti voler chiedere conferma)
            }
        } catch (Exception e) {
            // Ignora errori di controllo
        }
        
        // Usa il metodo semplice di upload
        boolean success = ftpService.uploadFile(localFile.toPath(), remotePath);
        
        if (success) {
            task.setTotalBytes(localFile.length());
            task.setBytesTransferred(localFile.length());
            task.setProgress(1.0);
        } else {
            throw new IOException("Upload fallito per: " + remotePath);
        }
    }
    
    /**
     * Ottiene il servizio FTP corrente
     */
    private FtpService getFtpServiceForItem(TransferItem item) {
        return currentFtpService;
    }
    
    /**
     * Metodi pubblici per compatibilità con il codice esistente
     */
    public void downloadFile(String remotePath, File localFile, FtpService ftpService, 
                            Consumer<TransferTask> onComplete) {
        // Aggiungi alla coda
        queueDownload(Arrays.asList(remotePath), localFile.getParentFile(), ftpService);
    }
    
    public void uploadFile(File localFile, String remotePath, FtpService ftpService,
                          Consumer<TransferTask> onComplete) {
        // Aggiungi alla coda
        queueUpload(Arrays.asList(localFile), remotePath, ftpService);
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
