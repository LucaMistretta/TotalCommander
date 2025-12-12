package com.totalcommander.models;

import javafx.beans.property.*;

/**
 * Modello per rappresentare un task di trasferimento file
 */
public class TransferTask {
    private final StringProperty sourcePath = new SimpleStringProperty();
    private final StringProperty destinationPath = new SimpleStringProperty();
    private final StringProperty fileName = new SimpleStringProperty();
    private final DoubleProperty progress = new SimpleDoubleProperty(0.0);
    private final StringProperty status = new SimpleStringProperty("In attesa...");
    private final LongProperty bytesTransferred = new SimpleLongProperty(0);
    private final LongProperty totalBytes = new SimpleLongProperty(0);
    private final StringProperty transferType = new SimpleStringProperty(); // "Upload" o "Download"
    private final BooleanProperty completed = new SimpleBooleanProperty(false);
    private final BooleanProperty failed = new SimpleBooleanProperty(false);
    private String errorMessage;

    public TransferTask(String sourcePath, String destinationPath, String fileName, String transferType) {
        this.sourcePath.set(sourcePath);
        this.destinationPath.set(destinationPath);
        this.fileName.set(fileName);
        this.transferType.set(transferType);
    }

    // Properties
    public StringProperty sourcePathProperty() { return sourcePath; }
    public StringProperty destinationPathProperty() { return destinationPath; }
    public StringProperty fileNameProperty() { return fileName; }
    public DoubleProperty progressProperty() { return progress; }
    public StringProperty statusProperty() { return status; }
    public LongProperty bytesTransferredProperty() { return bytesTransferred; }
    public LongProperty totalBytesProperty() { return totalBytes; }
    public StringProperty transferTypeProperty() { return transferType; }
    public BooleanProperty completedProperty() { return completed; }
    public BooleanProperty failedProperty() { return failed; }

    // Getters
    public String getSourcePath() { return sourcePath.get(); }
    public String getDestinationPath() { return destinationPath.get(); }
    public String getFileName() { return fileName.get(); }
    public double getProgress() { return progress.get(); }
    public String getStatus() { return status.get(); }
    public long getBytesTransferred() { return bytesTransferred.get(); }
    public long getTotalBytes() { return totalBytes.get(); }
    public String getTransferType() { return transferType.get(); }
    public boolean isCompleted() { return completed.get(); }
    public boolean isFailed() { return failed.get(); }
    public String getErrorMessage() { return errorMessage; }

    // Setters
    public void setProgress(double progress) { this.progress.set(progress); }
    public void setStatus(String status) { this.status.set(status); }
    public void setBytesTransferred(long bytes) { this.bytesTransferred.set(bytes); }
    public void setTotalBytes(long bytes) { this.totalBytes.set(bytes); }
    public void setCompleted(boolean completed) { this.completed.set(completed); }
    public void setFailed(boolean failed, String errorMessage) { 
        this.failed.set(failed); 
        this.errorMessage = errorMessage;
    }
}

