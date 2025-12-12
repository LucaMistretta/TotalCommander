package com.totalcommander.ui;

import com.totalcommander.models.TransferTask;
import com.totalcommander.services.TransferService;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 * Dialog per visualizzare e gestire i trasferimenti in background
 */
public class TransferManagerDialog extends Stage {
    
    private TableView<TransferTask> transfersTable;
    private TransferService transferService;
    private Label statusLabel;
    
    public TransferManagerDialog(TransferService transferService) {
        this.transferService = transferService;
        initStyle(StageStyle.UTILITY);
        setTitle("Gestore Trasferimenti");
        setWidth(800);
        setHeight(500);
        setResizable(true);
        
        initializeUI();
        
        // Aggiorna automaticamente la tabella quando cambiano i trasferimenti
        ObservableList<TransferTask> activeTransfers = transferService.getActiveTransfers();
        transfersTable.setItems(activeTransfers);
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Tabella trasferimenti
        transfersTable = new TableView<>();
        transfersTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Solo le colonne richieste: Origine, Destinazione, Progresso, Stato, Velocità
        TableColumn<TransferTask, String> sourceColumn = new TableColumn<>("Origine");
        sourceColumn.setCellValueFactory(new PropertyValueFactory<>("sourcePath"));
        sourceColumn.setPrefWidth(250);
        
        TableColumn<TransferTask, String> destinationColumn = new TableColumn<>("Destinazione");
        destinationColumn.setCellValueFactory(new PropertyValueFactory<>("destinationPath"));
        destinationColumn.setPrefWidth(250);
        
        TableColumn<TransferTask, Double> progressColumn = new TableColumn<>("Progresso");
        progressColumn.setCellValueFactory(new PropertyValueFactory<>("progress"));
        progressColumn.setCellFactory(column -> new TableCell<TransferTask, Double>() {
            private final ProgressBar progressBar = new ProgressBar();
            
            @Override
            protected void updateItem(Double progress, boolean empty) {
                super.updateItem(progress, empty);
                if (empty || progress == null) {
                    setGraphic(null);
                } else {
                    progressBar.setProgress(progress);
                    setGraphic(progressBar);
                }
            }
        });
        progressColumn.setPrefWidth(200);
        
        TableColumn<TransferTask, String> statusColumn = new TableColumn<>("Stato");
        statusColumn.setCellValueFactory(new PropertyValueFactory<>("status"));
        statusColumn.setPrefWidth(150);
        
        TableColumn<TransferTask, String> speedColumn = new TableColumn<>("Velocità");
        speedColumn.setCellValueFactory(cellData -> {
            TransferTask task = cellData.getValue();
            long bytes = task.getBytesTransferred();
            long total = task.getTotalBytes();
            if (total > 0 && bytes > 0) {
                return new javafx.beans.property.SimpleStringProperty(
                    formatBytes(bytes) + " / " + formatBytes(total));
            } else if (bytes > 0) {
                return new javafx.beans.property.SimpleStringProperty(formatBytes(bytes));
            }
            return new javafx.beans.property.SimpleStringProperty("-");
        });
        speedColumn.setPrefWidth(150);
        
        transfersTable.getColumns().addAll(sourceColumn, destinationColumn, 
                                          progressColumn, statusColumn, speedColumn);
        
        // Label stato (senza bottoni)
        statusLabel = new Label("Trasferimenti attivi: 0");
        statusLabel.setPadding(new Insets(5, 0, 0, 0));
        
        // Aggiorna il contatore
        transferService.getActiveTransfers().addListener(
            (javafx.collections.ListChangeListener.Change<? extends TransferTask> c) -> {
                long active = transferService.getActiveTransfers().stream()
                    .filter(t -> !t.isCompleted() && !t.isFailed())
                    .count();
                long completed = transferService.getActiveTransfers().stream()
                    .filter(TransferTask::isCompleted)
                    .count();
                long failed = transferService.getActiveTransfers().stream()
                    .filter(TransferTask::isFailed)
                    .count();
                statusLabel.setText(String.format(
                    "Attivi: %d | Completati: %d | Falliti: %d | Totale: %d",
                    active, completed, failed, transferService.getActiveTransfers().size()));
            });
        
        root.getChildren().addAll(transfersTable, statusLabel);
        VBox.setVgrow(transfersTable, Priority.ALWAYS);
        
        setScene(new javafx.scene.Scene(root));
    }
    
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
}

