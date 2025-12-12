package com.totalcommander.services;

import com.totalcommander.models.TransferTask;
import com.totalcommander.services.TransferLogService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import org.apache.commons.net.ftp.FTPFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import java.util.function.BiConsumer;

/**
 * Servizio per gestire trasferimenti file in background
 */
public class TransferService {
    
    private final ObservableList<TransferTask> activeTransfers = FXCollections.observableArrayList();
    private final ExecutorService executorService = Executors.newFixedThreadPool(3);
    private BiConsumer<String, String> statusCallback; // (type, message)
    
    public void setStatusCallback(BiConsumer<String, String> callback) {
        this.statusCallback = callback;
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
     * Upload file locale a server FTP
     */
    public void uploadFile(File localFile, String remotePath, FtpService ftpService, 
                          Consumer<TransferTask> onComplete) {
        String remoteFilePath = remotePath.endsWith("/") ? 
            remotePath + localFile.getName() : 
            remotePath + "/" + localFile.getName();
        
        // Controlla se il file esiste già sul server
        try {
            FTPFile[] files = ftpService.listFiles(remoteFilePath);
            if (files != null && files.length > 0 && files[0].isFile()) {
                // File esiste già - mostra alert
                javafx.application.Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                    alert.setTitle("File Esistente");
                    alert.setHeaderText("Il file esiste già sul server");
                    alert.setContentText("Il file \"" + localFile.getName() + "\" esiste già nella destinazione.\n" +
                                      "Vuoi sovrascriverlo?");
                    
                    alert.showAndWait().ifPresent(response -> {
                        if (response == ButtonType.OK) {
                            proceedWithUpload(localFile, remotePath, remoteFilePath, ftpService, onComplete);
                        }
                    });
                });
                return;
            }
        } catch (Exception e) {
            // Ignora errori di controllo, procedi comunque
        }
        
        proceedWithUpload(localFile, remotePath, remoteFilePath, ftpService, onComplete);
    }
    
    private void proceedWithUpload(File localFile, String remotePath, String remoteFilePath, 
                                  FtpService ftpService, Consumer<TransferTask> onComplete) {
        // Controlla se c'è un trasferimento incompleto da riprendere
        TransferLogService.TransferLogEntry incomplete = 
            TransferLogService.findIncompleteTransfer(localFile.getAbsolutePath(), remoteFilePath);
        
        TransferTask task = new TransferTask(
            localFile.getAbsolutePath(),
            remoteFilePath,
            localFile.getName(),
            "Upload"
        );
        
        task.setTotalBytes(localFile.length());
        
        // Se c'è un trasferimento incompleto, riprendi da lì
        if (incomplete != null && incomplete.bytesTransferred > 0 && 
            incomplete.bytesTransferred < incomplete.totalBytes) {
            task.setBytesTransferred(incomplete.bytesTransferred);
            task.setProgress((double) incomplete.bytesTransferred / incomplete.totalBytes);
            task.setStatus("Ripresa trasferimento...");
        }
        
        activeTransfers.add(task);
        
        executorService.submit(() -> {
            try {
                // Salva log iniziale
                TransferLogService.saveTransfer(task);
                
                task.setStatus("Inizio trasferimento...");
                
                // Se è una cartella, crea la directory remota e carica i file
                if (localFile.isDirectory()) {
                    uploadDirectory(localFile, remoteFilePath, ftpService, task);
                } else {
                    uploadSingleFile(localFile, remoteFilePath, ftpService, task, incomplete);
                }
                
                task.setProgress(1.0);
                task.setStatus("Completato");
                task.setCompleted(true);
                
                // Salva log finale
                TransferLogService.saveTransfer(task);
                
                if (onComplete != null) {
                    javafx.application.Platform.runLater(() -> onComplete.accept(task));
                }
            } catch (Exception e) {
                task.setFailed(true, e.getMessage());
                task.setStatus("Errore: " + e.getMessage());
                TransferLogService.saveTransfer(task); // Salva anche gli errori
                e.printStackTrace();
            }
        });
    }
    
    private void uploadSingleFile(File localFile, String remotePath, FtpService ftpService, 
                                  TransferTask task, TransferLogService.TransferLogEntry resumeFrom) throws IOException {
        task.setStatus("Caricamento in corso...");
        
        long startPosition = 0;
        if (resumeFrom != null && resumeFrom.bytesTransferred > 0) {
            startPosition = resumeFrom.bytesTransferred;
            // Salta i bytes già trasferiti
            try (FileInputStream fis = new FileInputStream(localFile)) {
                fis.skip(startPosition);
            }
        }
        
        try (FileInputStream fis = new FileInputStream(localFile);
             BufferedInputStream bis = new BufferedInputStream(fis);
             ProgressInputStream pis = new ProgressInputStream(bis, task, startPosition)) {
            
            // Se stiamo riprendendo, salta i bytes già trasferiti
            if (startPosition > 0) {
                bis.skip(startPosition);
            }
            
            boolean success = ftpService.uploadFile(pis, remotePath);
            
            // Salva log durante il trasferimento
            TransferLogService.saveTransfer(task);
            
            // Leggi la risposta del server
            String reply = ftpService.getReplyString();
            if (reply != null && !reply.trim().isEmpty()) {
                notifyStatus("FTP", reply.trim());
            }
            
            if (!success) {
                throw new IOException("Upload fallito per: " + remotePath);
            }
        }
    }
    
    private void uploadDirectory(File localDir, String remotePath, FtpService ftpService, 
                                TransferTask task) throws IOException {
        // Crea la directory remota
        if (!ftpService.createDirectory(remotePath)) {
            throw new IOException("Impossibile creare la directory remota: " + remotePath);
        }
        
        task.setStatus("Creazione directory...");
        
        // Carica tutti i file nella directory
        File[] files = localDir.listFiles();
        if (files != null) {
            for (File file : files) {
                String childRemotePath = remotePath.endsWith("/") ? 
                    remotePath + file.getName() : 
                    remotePath + "/" + file.getName();
                
                if (file.isDirectory()) {
                    uploadDirectory(file, childRemotePath, ftpService, task);
                } else {
                    // Controlla se c'è un trasferimento incompleto per questo file
                    TransferLogService.TransferLogEntry incomplete = 
                        TransferLogService.findIncompleteTransfer(file.getAbsolutePath(), childRemotePath);
                    uploadSingleFile(file, childRemotePath, ftpService, task, incomplete);
                }
            }
        }
    }
    
    /**
     * Download file da server FTP a locale
     */
    public void downloadFile(String remotePath, File localFile, FtpService ftpService,
                            Consumer<TransferTask> onComplete) {
        // Controlla se il file esiste già localmente
        if (localFile.exists() && localFile.isFile()) {
            javafx.application.Platform.runLater(() -> {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("File Esistente");
                alert.setHeaderText("Il file esiste già localmente");
                alert.setContentText("Il file \"" + localFile.getName() + "\" esiste già nella destinazione.\n" +
                                  "Vuoi sovrascriverlo?");
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        proceedWithDownload(remotePath, localFile, ftpService, onComplete);
                    }
                });
            });
            return;
        }
        
        proceedWithDownload(remotePath, localFile, ftpService, onComplete);
    }
    
    private void proceedWithDownload(String remotePath, File localFile, FtpService ftpService,
                                    Consumer<TransferTask> onComplete) {
        // Controlla se c'è un trasferimento incompleto da riprendere
        TransferLogService.TransferLogEntry incomplete = 
            TransferLogService.findIncompleteTransfer(remotePath, localFile.getAbsolutePath());
        
        TransferTask task = new TransferTask(
            remotePath,
            localFile.getAbsolutePath(),
            new File(remotePath).getName(),
            "Download"
        );
        
        activeTransfers.add(task);
        
        executorService.submit(() -> {
            try {
                // Salva log iniziale
                TransferLogService.saveTransfer(task);
                
                task.setStatus("Recupero informazioni file...");
                
                // Ottieni dimensione file
                FTPFile[] files = ftpService.listFiles(remotePath);
                if (files != null && files.length > 0) {
                    FTPFile ftpFile = files[0];
                    if (ftpFile.isFile()) {
                        task.setTotalBytes(ftpFile.getSize());
                    }
                }
                
                // Se c'è un trasferimento incompleto, riprendi da lì
                if (incomplete != null && incomplete.bytesTransferred > 0 && 
                    incomplete.bytesTransferred < incomplete.totalBytes) {
                    task.setBytesTransferred(incomplete.bytesTransferred);
                    task.setProgress((double) incomplete.bytesTransferred / incomplete.totalBytes);
                    task.setStatus("Ripresa trasferimento...");
                }
                
                if (task.getTotalBytes() == 0) {
                    // Prova a scaricare come directory
                    downloadDirectory(remotePath, localFile, ftpService, task);
                } else {
                    downloadSingleFile(remotePath, localFile, ftpService, task, incomplete);
                }
                
                task.setProgress(1.0);
                task.setStatus("Completato");
                task.setCompleted(true);
                
                // Salva log finale
                TransferLogService.saveTransfer(task);
                
                if (onComplete != null) {
                    javafx.application.Platform.runLater(() -> onComplete.accept(task));
                }
            } catch (Exception e) {
                task.setFailed(true, e.getMessage());
                task.setStatus("Errore: " + e.getMessage());
                TransferLogService.saveTransfer(task); // Salva anche gli errori
                e.printStackTrace();
            }
        });
    }
    
    private void downloadSingleFile(String remotePath, File localFile, FtpService ftpService,
                                    TransferTask task, TransferLogService.TransferLogEntry resumeFrom) throws IOException {
        task.setStatus("Download in corso...");
        
        long startPosition = 0;
        boolean appendMode = false;
        
        // Se stiamo riprendendo un trasferimento, usa append mode
        if (resumeFrom != null && resumeFrom.bytesTransferred > 0 && localFile.exists()) {
            startPosition = resumeFrom.bytesTransferred;
            appendMode = true;
        }
        
        try (InputStream is = ftpService.retrieveFileStream(remotePath);
             BufferedInputStream bis = new BufferedInputStream(is);
             ProgressInputStream pis = new ProgressInputStream(bis, task, startPosition);
             FileOutputStream fos = new FileOutputStream(localFile, appendMode);
             BufferedOutputStream bos = new BufferedOutputStream(fos)) {
            
            // Se stiamo riprendendo, salta i bytes già scaricati
            if (startPosition > 0 && !appendMode) {
                bis.skip(startPosition);
            }
            
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = pis.read(buffer)) != -1) {
                bos.write(buffer, 0, bytesRead);
                
                // Salva log periodicamente durante il trasferimento
                if (task.getBytesTransferred() % (1024 * 1024) == 0) { // Ogni MB
                    TransferLogService.saveTransfer(task);
                }
            }
            
            bos.flush();
            
            // Salva log durante il trasferimento
            TransferLogService.saveTransfer(task);
            
            // Completa il comando FTP e leggi la risposta
            boolean success = ftpService.completePendingCommand();
            String reply = ftpService.getReplyString();
            if (reply != null && !reply.trim().isEmpty()) {
                notifyStatus("FTP", reply.trim());
            }
            
            if (!success) {
                throw new IOException("Download fallito per: " + remotePath);
            }
        }
    }
    
    private void downloadDirectory(String remotePath, File localDir, FtpService ftpService,
                                  TransferTask task) throws IOException {
        // Crea la directory locale
        if (!localDir.exists()) {
            localDir.mkdirs();
        }
        
        task.setStatus("Scaricamento directory...");
        
        // Lista file nella directory remota
        FTPFile[] files = ftpService.listFiles(remotePath);
        if (files != null) {
            for (FTPFile ftpFile : files) {
                String fileName = ftpFile.getName();
                if (fileName.equals(".") || fileName.equals("..")) {
                    continue;
                }
                
                String childRemotePath = remotePath.endsWith("/") ? 
                    remotePath + fileName : 
                    remotePath + "/" + fileName;
                
                File childLocalFile = new File(localDir, fileName);
                
                if (ftpFile.isDirectory()) {
                    downloadDirectory(childRemotePath, childLocalFile, ftpService, task);
                } else {
                    // Controlla se c'è un trasferimento incompleto per questo file
                    TransferLogService.TransferLogEntry incomplete = 
                        TransferLogService.findIncompleteTransfer(childRemotePath, childLocalFile.getAbsolutePath());
                    downloadSingleFile(childRemotePath, childLocalFile, ftpService, task, incomplete);
                }
            }
        }
    }
    
    /**
     * InputStream che traccia il progresso
     */
    private class ProgressInputStream extends FilterInputStream {
        private final TransferTask task;
        private long bytesRead;
        
        public ProgressInputStream(InputStream in, TransferTask task, long startPosition) {
            super(in);
            this.task = task;
            this.bytesRead = startPosition;
        }
        
        @Override
        public int read() throws IOException {
            int result = super.read();
            if (result != -1) {
                bytesRead++;
                updateProgress();
            }
            return result;
        }
        
        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            int result = super.read(b, off, len);
            if (result > 0) {
                bytesRead += result;
                updateProgress();
            }
            return result;
        }
        
        private void updateProgress() {
            if (task.getTotalBytes() > 0) {
                double progress = (double) bytesRead / task.getTotalBytes();
                javafx.application.Platform.runLater(() -> {
                    task.setProgress(progress);
                    task.setBytesTransferred(bytesRead);
                    task.setStatus(String.format("%s... %.1f%%", 
                        task.getTransferType(), progress * 100));
                });
            } else {
                javafx.application.Platform.runLater(() -> {
                    task.setBytesTransferred(bytesRead);
                    task.setStatus(String.format("%s... %d bytes", 
                        task.getTransferType(), bytesRead));
                });
            }
        }
    }
    
    public void shutdown() {
        executorService.shutdown();
    }
}
