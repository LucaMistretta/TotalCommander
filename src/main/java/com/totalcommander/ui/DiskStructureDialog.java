package com.totalcommander.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.nio.file.Path;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dialog per visualizzare la struttura del disco
 */
public class DiskStructureDialog extends Stage {
    
    private TreeTableView<DiskItem> treeTable;
    private Label statusLabel;
    private ProgressBar progressBar;
    private ExecutorService executorService;
    
    public DiskStructureDialog() {
        initStyle(StageStyle.UTILITY);
        setTitle("Struttura del Disco");
        setWidth(900);
        setHeight(700);
        setResizable(true);
        
        executorService = Executors.newSingleThreadExecutor();
        
        initializeUI();
        loadDiskStructure();
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // Label informazioni
        Label infoLabel = new Label("Struttura delle directory del disco:");
        infoLabel.setStyle("-fx-font-weight: bold;");
        
        // TreeTableView per mostrare la struttura
        treeTable = new TreeTableView<>();
        treeTable.setColumnResizePolicy(TreeTableView.UNCONSTRAINED_RESIZE_POLICY);
        
        // Colonna Nome
        TreeTableColumn<DiskItem, String> nameColumn = new TreeTableColumn<>("Nome");
        nameColumn.setCellValueFactory(param -> param.getValue().getValue().nameProperty());
        nameColumn.setPrefWidth(300);
        
        // Colonna Tipo
        TreeTableColumn<DiskItem, String> typeColumn = new TreeTableColumn<>("Tipo");
        typeColumn.setCellValueFactory(param -> param.getValue().getValue().typeProperty());
        typeColumn.setPrefWidth(100);
        
        // Colonna Dimensione
        TreeTableColumn<DiskItem, String> sizeColumn = new TreeTableColumn<>("Dimensione");
        sizeColumn.setCellValueFactory(param -> param.getValue().getValue().sizeProperty());
        sizeColumn.setPrefWidth(150);
        
        // Colonna File/Cartelle
        TreeTableColumn<DiskItem, String> countColumn = new TreeTableColumn<>("File/Cartelle");
        countColumn.setCellValueFactory(param -> param.getValue().getValue().countProperty());
        countColumn.setPrefWidth(150);
        
        // Colonna Percorso
        TreeTableColumn<DiskItem, String> pathColumn = new TreeTableColumn<>("Percorso");
        pathColumn.setCellValueFactory(param -> param.getValue().getValue().pathProperty());
        pathColumn.setPrefWidth(200);
        
        treeTable.getColumns().addAll(nameColumn, typeColumn, sizeColumn, countColumn, pathColumn);
        
        // Progress bar e status
        progressBar = new ProgressBar();
        progressBar.setProgress(-1); // Indeterminato
        progressBar.setVisible(false);
        
        statusLabel = new Label("Caricamento in corso...");
        
        // Pulsante aggiorna
        Button refreshButton = new Button("Aggiorna");
        refreshButton.setOnAction(e -> loadDiskStructure());
        
        // Pulsante chiudi
        Button closeButton = new Button("Chiudi");
        closeButton.setOnAction(e -> close());
        
        HBox buttonBox = new HBox(10);
        buttonBox.getChildren().addAll(refreshButton, closeButton);
        
        root.getChildren().addAll(infoLabel, treeTable, progressBar, statusLabel, buttonBox);
        VBox.setVgrow(treeTable, Priority.ALWAYS);
        
        setScene(new javafx.scene.Scene(root));
        
        setOnCloseRequest(e -> {
            if (executorService != null) {
                executorService.shutdown();
            }
        });
    }
    
    private void loadDiskStructure() {
        progressBar.setVisible(true);
        statusLabel.setText("Caricamento unità disco...");
        treeTable.setRoot(null);
        
        executorService.submit(() -> {
            try {
                // Crea la root con tutte le unità disco
                TreeItem<DiskItem> rootItem = new TreeItem<>(new DiskItem("Disco", "Root", "", "", ""));
                rootItem.setExpanded(true);
                
                File[] roots = File.listRoots();
                for (File root : roots) {
                    try {
                        long totalSpace = root.getTotalSpace();
                        long freeSpace = root.getUsableSpace();
                        long usedSpace = totalSpace - freeSpace;
                        
                        DiskItem diskItem = new DiskItem(
                            root.getAbsolutePath(),
                            "Unità",
                            formatBytes(totalSpace) + " (Libero: " + formatBytes(freeSpace) + ")",
                            "",
                            root.getAbsolutePath()
                        );
                        TreeItem<DiskItem> diskNode = new TreeItem<>(diskItem);
                        diskNode.setExpanded(false);
                        
                        // Aggiungi un nodo placeholder che verrà caricato quando si espande
                        TreeItem<DiskItem> placeholder = new TreeItem<>(
                            new DiskItem("Caricamento...", "Cartella", "", "", ""));
                        diskNode.getChildren().add(placeholder);
                        
                        // Listener per caricare quando si espande
                        diskNode.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                            if (isNowExpanded && diskNode.getChildren().size() == 1 && 
                                diskNode.getChildren().get(0).getValue().getName().equals("Caricamento...")) {
                                loadDirectoryLazy(root.toPath(), diskNode);
                            }
                        });
                        
                        javafx.application.Platform.runLater(() -> {
                            rootItem.getChildren().add(diskNode);
                        });
                    } catch (Exception e) {
                        // Ignora errori per singole unità
                        System.err.println("Errore caricamento unità " + root + ": " + e.getMessage());
                    }
                }
                
                javafx.application.Platform.runLater(() -> {
                    treeTable.setRoot(rootItem);
                    progressBar.setVisible(false);
                    statusLabel.setText("Struttura caricata. Espandi le cartelle per vedere i dettagli.");
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    progressBar.setVisible(false);
                    statusLabel.setText("Errore: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }
    
    private void loadDirectoryLazy(Path dirPath, TreeItem<DiskItem> parentNode) {
        executorService.submit(() -> {
            try {
                File dir = dirPath.toFile();
                if (!dir.exists() || !dir.isDirectory()) {
                    javafx.application.Platform.runLater(() -> {
                        parentNode.getChildren().clear();
                    });
                    return;
                }
                
                File[] children = dir.listFiles();
                if (children == null) {
                    javafx.application.Platform.runLater(() -> {
                        parentNode.getChildren().clear();
                    });
                    return;
                }
                
                int fileCount = 0;
                int dirCount = 0;
                long totalSize = 0;
                int maxItems = 50; // Limite per performance
                
                javafx.application.Platform.runLater(() -> {
                    parentNode.getChildren().clear();
                });
                
                for (File child : children) {
                    if (dirCount + fileCount >= maxItems) {
                        break; // Limita il numero di elementi
                    }
                    
                    try {
                        if (child.isDirectory()) {
                            dirCount++;
                            
                            DiskItem dirItem = new DiskItem(
                                child.getName(),
                                "Cartella",
                                "-",
                                "",
                                child.getAbsolutePath()
                            );
                            TreeItem<DiskItem> dirNode = new TreeItem<>(dirItem);
                            dirNode.setExpanded(false);
                            
                            // Aggiungi placeholder per caricamento lazy
                            TreeItem<DiskItem> placeholder = new TreeItem<>(
                                new DiskItem("Caricamento...", "Cartella", "", "", ""));
                            dirNode.getChildren().add(placeholder);
                            
                            // Listener per caricare quando si espande
                            dirNode.expandedProperty().addListener((obs, wasExpanded, isNowExpanded) -> {
                                if (isNowExpanded && dirNode.getChildren().size() == 1 && 
                                    dirNode.getChildren().get(0).getValue().getName().equals("Caricamento...")) {
                                    loadDirectoryLazy(child.toPath(), dirNode);
                                }
                            });
                            
                            javafx.application.Platform.runLater(() -> {
                                parentNode.getChildren().add(dirNode);
                            });
                        } else {
                            fileCount++;
                            totalSize += child.length();
                        }
                    } catch (Exception e) {
                        // Ignora errori di accesso per singoli file
                    }
                }
                
                // Aggiorna le informazioni del nodo parent
                final String sizeStr = totalSize > 0 ? formatBytes(totalSize) : "-";
                String countStrBase = fileCount + " file, " + dirCount + " cartelle";
                final String countStr = (dirCount + fileCount >= maxItems) ? 
                    countStrBase + " (altri elementi non mostrati)" : countStrBase;
                
                javafx.application.Platform.runLater(() -> {
                    DiskItem parentItem = parentNode.getValue();
                    if (parentItem != null) {
                        parentItem.setSize(sizeStr);
                        parentItem.setCount(countStr);
                    }
                });
                
            } catch (Exception e) {
                // Ignora errori di accesso (permessi, ecc.)
                javafx.application.Platform.runLater(() -> {
                    parentNode.getChildren().clear();
                    TreeItem<DiskItem> errorNode = new TreeItem<>(
                        new DiskItem("Errore di accesso", "Errore", "", "", ""));
                    parentNode.getChildren().add(errorNode);
                });
            }
        });
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    /**
     * Classe per rappresentare un elemento nella struttura del disco
     */
    private static class DiskItem {
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty type;
        private final javafx.beans.property.SimpleStringProperty size;
        private final javafx.beans.property.SimpleStringProperty count;
        private final javafx.beans.property.SimpleStringProperty path;
        
        public DiskItem(String name, String type, String size, String count, String path) {
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.type = new javafx.beans.property.SimpleStringProperty(type);
            this.size = new javafx.beans.property.SimpleStringProperty(size);
            this.count = new javafx.beans.property.SimpleStringProperty(count);
            this.path = new javafx.beans.property.SimpleStringProperty(path);
        }
        
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty typeProperty() { return type; }
        public javafx.beans.property.StringProperty sizeProperty() { return size; }
        public javafx.beans.property.StringProperty countProperty() { return count; }
        public javafx.beans.property.StringProperty pathProperty() { return path; }
        
        public String getName() { return name.get(); }
        public void setSize(String size) { this.size.set(size); }
        public void setCount(String count) { this.count.set(count); }
    }
}

