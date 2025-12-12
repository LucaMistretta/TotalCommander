package com.totalcommander.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import com.totalcommander.ui.panels.FilePanel;
import com.totalcommander.services.FileOperationService;
import com.totalcommander.services.TransferService;
import com.totalcommander.ui.MultiRenameDialog;
import com.totalcommander.ui.CreateArchiveDialog;
import java.util.List;

/**
 * Finestra principale con due pannelli file
 */
public class MainWindow extends BorderPane {
    
    private FilePanel leftPanel;
    private FilePanel rightPanel;
    private FilePanel activePanel;
    private FileOperationService fileOperationService;
    private TransferService transferService;
    private MenuBar menuBar;
    private TransferManagerDialog transferManagerDialog;
    
    // Barra FTP
    private HBox ftpBar;
    private TextArea ftpStatusArea;
    private ComboBox<String> ftpTransmissionMode;
    private FilePanel ftpConnectedPanel;

    public MainWindow() {
        this.fileOperationService = new FileOperationService();
        this.transferService = new TransferService();
        
        // Configura callback per aggiornare lo status FTP
        transferService.setStatusCallback((type, message) -> {
            if (type.equals("FTP")) {
                appendFtpStatus(message);
            }
        });
        
        initializeUI();
        setupKeyboardShortcuts();
    }

    private void initializeUI() {
        // Menu bar
        menuBar = createMenuBar();
        setTop(menuBar);

        // Pannelli file
        HBox panelsContainer = new HBox(5);
        panelsContainer.setPadding(new Insets(5));
        
        leftPanel = new FilePanel(this::setActivePanel);
        rightPanel = new FilePanel(this::setActivePanel);
        
        panelsContainer.getChildren().addAll(leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        
        setCenter(panelsContainer);
        
        // Container per barra bottoni e barra FTP
        VBox bottomContainer = new VBox();
        
        // Barra di bottoni in basso
        HBox buttonBar = createButtonBar();
        
        // Barra FTP (inizialmente nascosta)
        HBox ftpBar = createFtpBar();
        ftpBar.setVisible(false);
        ftpBar.setManaged(false);
        
        bottomContainer.getChildren().addAll(buttonBar, ftpBar);
        setBottom(bottomContainer);
        
        // Pannello attivo di default
        activePanel = leftPanel;
        leftPanel.setActive(true);
    }
    
    private HBox createFtpBar() {
        HBox ftpBar = new HBox(10);
        ftpBar.setPadding(new Insets(5, 10, 5, 10));
        ftpBar.setStyle("-fx-background-color: #e0e0e0; -fx-border-color: #c0c0c0; -fx-border-width: 1 0 0 0;");
        
        // Label FTP
        Label ftpLabel = new Label("FTP");
        ftpLabel.setStyle("-fx-font-weight: bold;");
        
        // Dropdown modalità trasmissione
        Label modeLabel = new Label("Modalità di trasmissione:");
        ComboBox<String> transmissionMode = new ComboBox<>();
        transmissionMode.getItems().addAll(
            "Binaria (archivi, doc ecc.)",
            "ASCII (testo)"
        );
        transmissionMode.setValue("Binaria (archivi, doc ecc.)");
        transmissionMode.setOnAction(e -> {
            String selected = transmissionMode.getSelectionModel().getSelectedItem();
            // Aggiorna modalità FTP se connesso
            updateFtpTransmissionMode(selected.equals("Binaria (archivi, doc ecc.)"));
        });
        
        // Bottone Disconnetti
        Button disconnectBtn = new Button("Disconnetti");
        disconnectBtn.setOnAction(e -> disconnectFtp());
        
        // Area di stato FTP
        TextArea statusArea = new TextArea();
        statusArea.setEditable(false);
        statusArea.setPrefRowCount(2);
        statusArea.setMaxHeight(50);
        statusArea.setStyle("-fx-font-family: monospace; -fx-font-size: 10pt;");
        HBox.setHgrow(statusArea, Priority.ALWAYS);
        
        // Salva riferimento per aggiornamenti
        this.ftpStatusArea = statusArea;
        this.ftpTransmissionMode = transmissionMode;
        this.ftpBar = ftpBar;
        
        ftpBar.getChildren().addAll(ftpLabel, modeLabel, transmissionMode, disconnectBtn, statusArea);
        
        return ftpBar;
    }
    
    private HBox createButtonBar() {
        HBox buttonBar = new HBox(5);
        buttonBar.setPadding(new Insets(10));
        buttonBar.setStyle("-fx-background-color: #f0f0f0;");
        
        Button visualizzaBtn = new Button("Visualizza");
        visualizzaBtn.setMaxWidth(Double.MAX_VALUE);
        visualizzaBtn.setOnAction(e -> activePanel.openSelectedItem());
        
        Button modificaBtn = new Button("Modifica");
        modificaBtn.setMaxWidth(Double.MAX_VALUE);
        modificaBtn.setOnAction(e -> activePanel.editSelectedFile());
        
        Button copiaBtn = new Button("Copia");
        copiaBtn.setMaxWidth(Double.MAX_VALUE);
        copiaBtn.setOnAction(e -> copySelected());
        
        Button spostaBtn = new Button("Sposta");
        spostaBtn.setMaxWidth(Double.MAX_VALUE);
        spostaBtn.setOnAction(e -> moveSelected());
        
        Button creaCartellaBtn = new Button("Crea Cartella");
        creaCartellaBtn.setMaxWidth(Double.MAX_VALUE);
        creaCartellaBtn.setOnAction(e -> activePanel.createNewFolder());
        
        Button eliminaBtn = new Button("Elimina");
        eliminaBtn.setMaxWidth(Double.MAX_VALUE);
        eliminaBtn.setOnAction(e -> activePanel.deleteSelected());
        
        Button esciBtn = new Button("Esci");
        esciBtn.setMaxWidth(Double.MAX_VALUE);
        esciBtn.setOnAction(e -> {
            javafx.application.Platform.exit();
        });
        
        buttonBar.getChildren().addAll(visualizzaBtn, modificaBtn, copiaBtn, 
                                      spostaBtn, creaCartellaBtn, eliminaBtn, esciBtn);
        
        // Fai in modo che i bottoni si espandano uniformemente
        for (javafx.scene.Node node : buttonBar.getChildren()) {
            HBox.setHgrow(node, Priority.ALWAYS);
        }
        
        return buttonBar;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Menu File
        Menu fileMenu = new Menu("File");
        MenuItem newFolder = new MenuItem("Nuova Cartella");
        newFolder.setOnAction(e -> activePanel.createNewFolder());
        MenuItem delete = new MenuItem("Elimina");
        delete.setOnAction(e -> activePanel.deleteSelected());
        MenuItem rename = new MenuItem("Rinomina");
        rename.setOnAction(e -> activePanel.renameSelected());
        MenuItem multiRename = new MenuItem("Rinomina Multipla");
        multiRename.setOnAction(e -> showMultiRenameDialog());
        fileMenu.getItems().addAll(newFolder, new SeparatorMenuItem(), delete, rename, multiRename);
        
        // Menu Modifica
        Menu editMenu = new Menu("Modifica");
        MenuItem copy = new MenuItem("Copia");
        copy.setOnAction(e -> copySelected());
        MenuItem move = new MenuItem("Sposta");
        move.setOnAction(e -> moveSelected());
        MenuItem edit = new MenuItem("Modifica");
        edit.setOnAction(e -> activePanel.editSelectedFile());
        editMenu.getItems().addAll(copy, move, new SeparatorMenuItem(), edit);
        
        // Menu Archivi
        Menu archiveMenu = new Menu("Archivi");
        MenuItem createArchive = new MenuItem("Crea Archivio");
        createArchive.setOnAction(e -> showCreateArchiveDialog());
        MenuItem extractArchive = new MenuItem("Estrai Archivio");
        extractArchive.setOnAction(e -> activePanel.extractArchive());
        archiveMenu.getItems().addAll(createArchive, extractArchive);
        
        // Menu FTP
        Menu ftpMenu = new Menu("FTP");
        MenuItem manageConnections = new MenuItem("Connetti FTP");
        manageConnections.setOnAction(e -> showFtpManagerDialog());
        ftpMenu.getItems().addAll(manageConnections);
        
        // Menu Comandi
        Menu commandsMenu = new Menu("Comandi");
        
        MenuItem diskStructure = new MenuItem("Struttura del Disco");
        diskStructure.setOnAction(e -> showDiskStructure());
        
        MenuItem find = new MenuItem("Trova");
        find.setOnAction(e -> showFindDialog());
        
        MenuItem findInBackground = new MenuItem("Trova in Processo Sparato");
        findInBackground.setOnAction(e -> showFindInBackground());
        
        MenuItem systemInfo = new MenuItem("Informazioni Sistema");
        systemInfo.setOnAction(e -> showSystemInfo());
        
        Menu terminalMenu = new Menu("Apri Terminal");
        MenuItem openTerminal = new MenuItem("Terminal");
        openTerminal.setOnAction(e -> openTerminal());
        MenuItem openPowerShell = new MenuItem("PowerShell");
        openPowerShell.setOnAction(e -> openPowerShell());
        terminalMenu.getItems().addAll(openTerminal, openPowerShell);
        
        Menu openWindowMenu = new Menu("Apri Finestra");
        MenuItem openDesktop = new MenuItem("Desktop");
        openDesktop.setOnAction(e -> openSpecialFolder("Desktop"));
        MenuItem openDocuments = new MenuItem("Documenti");
        openDocuments.setOnAction(e -> openSpecialFolder("Documents"));
        MenuItem openDownloads = new MenuItem("Download");
        openDownloads.setOnAction(e -> openSpecialFolder("Downloads"));
        openWindowMenu.getItems().addAll(openDesktop, openDocuments, openDownloads);
        
        MenuItem transferManager = new MenuItem("Gestore Trasferimenti in Background");
        transferManager.setOnAction(e -> showTransferManager());
        
        commandsMenu.getItems().addAll(diskStructure, find, findInBackground, 
                                      new SeparatorMenuItem(), systemInfo,
                                      new SeparatorMenuItem(), terminalMenu, openWindowMenu,
                                      new SeparatorMenuItem(), transferManager);
        
        menuBar.getMenus().addAll(fileMenu, editMenu, archiveMenu, ftpMenu, commandsMenu);
        return menuBar;
    }

    private void setupKeyboardShortcuts() {
        // Tab per cambiare pannello
        setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.TAB) {
                switchActivePanel();
            }
        });
    }

    private void setActivePanel(FilePanel panel) {
        if (activePanel != null && activePanel != panel) {
            activePanel.setActive(false);
        }
        if (panel != null) {
            activePanel = panel;
            activePanel.setActive(true);
            // Assicura che il pannello riceva il focus
            activePanel.requestFocus();
        }
    }

    private void switchActivePanel() {
        if (activePanel == leftPanel) {
            setActivePanel(rightPanel);
        } else {
            setActivePanel(leftPanel);
        }
    }

    private void copySelected() {
        FilePanel sourcePanel = activePanel;
        FilePanel targetPanel = (activePanel == leftPanel) ? rightPanel : leftPanel;
        
        // Verifica che ci siano file selezionati
        List<java.io.File> selectedFiles = sourcePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Nessun file selezionato");
            alert.setContentText("Seleziona almeno un file per copiare.");
            alert.showAndWait();
            return;
        }
        
        // Se uno dei pannelli è in modalità FTP, usa il trasferimento FTP
        if (sourcePanel.isFtpMode() || targetPanel.isFtpMode()) {
            copyFtpFiles(sourcePanel, targetPanel);
        } else {
            // Copia locale normale
            if (fileOperationService.copyFiles(selectedFiles, targetPanel.getCurrentPath())) {
                targetPanel.refresh();
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore");
                alert.setHeaderText("Impossibile copiare i file");
                alert.setContentText("Verifica i permessi e riprova.");
                alert.showAndWait();
            }
        }
    }

    private void moveSelected() {
        FilePanel sourcePanel = activePanel;
        FilePanel targetPanel = (activePanel == leftPanel) ? rightPanel : leftPanel;
        
        // Se uno dei pannelli è in modalità FTP, usa il trasferimento FTP
        if (sourcePanel.isFtpMode() || targetPanel.isFtpMode()) {
            moveFtpFiles(sourcePanel, targetPanel);
        } else {
            // Sposta locale normale
            fileOperationService.moveFiles(
                sourcePanel.getSelectedFiles(),
                targetPanel.getCurrentPath()
            );
            sourcePanel.refresh();
            targetPanel.refresh();
        }
    }
    
    private void copyFtpFiles(FilePanel sourcePanel, FilePanel targetPanel) {
        // Implementazione copia tra locale e FTP
        List<java.io.File> selectedFiles = sourcePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Nessun file selezionato");
            alert.setContentText("Seleziona almeno un file per copiare.");
            alert.showAndWait();
            return;
        }
        
        // Mostra il dialog dei trasferimenti se non è già visibile
        if (transferManagerDialog == null || !transferManagerDialog.isShowing()) {
            showTransferManager();
        }
        
        if (sourcePanel.isFtpMode()) {
            // Download da FTP a locale
            com.totalcommander.services.FtpService ftpService = sourcePanel.getFtpService();
            List<String> ftpPaths = sourcePanel.getSelectedFtpPaths();
            
            appendFtpStatus("Inizio download di " + ftpPaths.size() + " file...");
            
            for (String remoteFilePath : ftpPaths) {
                String fileName = new java.io.File(remoteFilePath).getName();
                java.io.File localFile = targetPanel.getCurrentPath().resolve(fileName).toFile();
                
                transferService.downloadFile(remoteFilePath, localFile, ftpService, task -> {
                    if (task.isCompleted()) {
                        appendFtpStatus("Download completato: " + task.getFileName());
                        targetPanel.refresh();
                        // Aggiorna anche il pannello FTP se è quello sorgente
                        if (sourcePanel.isFtpMode()) {
                            sourcePanel.refresh();
                        }
                    } else if (task.isFailed()) {
                        appendFtpStatus("Errore download: " + task.getFileName() + " - " + task.getErrorMessage());
                    }
                });
            }
        } else {
            // Upload da locale a FTP
            com.totalcommander.services.FtpService ftpService = targetPanel.getFtpService();
            String remotePath = targetPanel.getCurrentFtpPath();
            
            appendFtpStatus("Inizio upload di " + selectedFiles.size() + " file...");
            
            for (java.io.File localFile : selectedFiles) {
                transferService.uploadFile(localFile, remotePath, ftpService, task -> {
                    if (task.isCompleted()) {
                        appendFtpStatus("Upload completato: " + task.getFileName());
                        // Aggiorna il pannello FTP per mostrare i nuovi file
                        if (targetPanel.isFtpMode()) {
                            targetPanel.refresh();
                        }
                    } else if (task.isFailed()) {
                        appendFtpStatus("Errore upload: " + task.getFileName() + " - " + task.getErrorMessage());
                    }
                });
            }
        }
    }
    
    private void moveFtpFiles(FilePanel sourcePanel, FilePanel targetPanel) {
        // Per ora, implementiamo come copia (il delete può essere aggiunto dopo)
        copyFtpFiles(sourcePanel, targetPanel);
        
        // TODO: Aggiungere eliminazione file sorgente dopo trasferimento riuscito
    }

    private void showMultiRenameDialog() {
        List<java.io.File> selectedFiles = activePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Nessun file selezionato");
            alert.setContentText("Seleziona almeno un file per la rinomina multipla.");
            alert.showAndWait();
            return;
        }
        
        MultiRenameDialog dialog = new MultiRenameDialog(selectedFiles, activePanel.getCurrentPath());
        dialog.showAndWait();
        activePanel.refresh();
    }

    private void showCreateArchiveDialog() {
        List<java.io.File> selectedFiles = activePanel.getSelectedFiles();
        if (selectedFiles.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Nessun file selezionato");
            alert.setContentText("Seleziona almeno un file per creare l'archivio.");
            alert.showAndWait();
            return;
        }
        
        CreateArchiveDialog dialog = new CreateArchiveDialog(selectedFiles, activePanel.getCurrentPath());
        dialog.showAndWait();
        activePanel.refresh();
    }

    private javafx.concurrent.Task<Void> ftpRefreshTask;
    private javafx.concurrent.Service<Void> ftpRefreshService;
    
    private void startFtpAutoRefresh() {
        if (ftpRefreshService != null) {
            ftpRefreshService.cancel();
        }
        
        ftpRefreshService = new javafx.concurrent.Service<Void>() {
            @Override
            protected javafx.concurrent.Task<Void> createTask() {
                return new javafx.concurrent.Task<Void>() {
                    @Override
                    protected Void call() throws Exception {
                        while (!isCancelled() && ftpConnectedPanel != null && ftpConnectedPanel.isFtpMode()) {
                            Thread.sleep(5000); // Aggiorna ogni 5 secondi (ridotto per evitare problemi)
                            if (!isCancelled()) {
                                javafx.application.Platform.runLater(() -> {
                                    if (ftpConnectedPanel != null && ftpConnectedPanel.isFtpMode()) {
                                        // Controlla se ci sono trasferimenti in corso
                                        boolean hasActiveTransfers = transferService.getActiveTransfers().stream()
                                            .anyMatch(t -> !t.isCompleted() && !t.isFailed());
                                        
                                        // Aggiorna solo se non ci sono trasferimenti attivi o selezioni
                                        if (!hasActiveTransfers) {
                                            ftpConnectedPanel.refresh();
                                        }
                                    }
                                });
                            }
                        }
                        return null;
                    }
                };
            }
        };
        ftpRefreshService.start();
    }
    
    private void stopFtpAutoRefresh() {
        if (ftpRefreshService != null) {
            ftpRefreshService.cancel();
            ftpRefreshService = null;
        }
    }
    
    private void showFtpBar(com.totalcommander.models.FtpConnection connection) {
        if (ftpBar != null) {
            ftpBar.setVisible(true);
            ftpBar.setManaged(true);
            appendFtpStatus("Connessione stabilita con " + connection.getHost());
        }
    }
    
    private void hideFtpBar() {
        if (ftpBar != null) {
            ftpBar.setVisible(false);
            ftpBar.setManaged(false);
            if (ftpStatusArea != null) {
                ftpStatusArea.clear();
            }
        }
    }
    
    private void disconnectFtp() {
        if (ftpConnectedPanel != null && ftpConnectedPanel.isFtpMode()) {
            com.totalcommander.models.FtpConnection connection = ftpConnectedPanel.getFtpConnection();
            if (connection != null) {
                appendFtpStatus("Disconnessione in corso...");
                stopFtpAutoRefresh();
                com.totalcommander.services.FtpConnectionManager.disconnect(connection);
                ftpConnectedPanel.disconnectFromFtp();
                ftpConnectedPanel = null;
                appendFtpStatus("Disconnesso");
                hideFtpBar();
            }
        }
    }
    
    private void updateFtpTransmissionMode(boolean binary) {
        if (ftpConnectedPanel != null && ftpConnectedPanel.isFtpMode()) {
            com.totalcommander.services.FtpService ftpService = ftpConnectedPanel.getFtpService();
            if (ftpService != null) {
                try {
                    ftpService.setFileType(binary);
                    appendFtpStatus("Modalità trasmissione: " + (binary ? "Binaria" : "ASCII"));
                } catch (Exception e) {
                    appendFtpStatus("Errore cambio modalità: " + e.getMessage());
                }
            }
        }
    }
    
    public void appendFtpStatus(String message) {
        if (ftpStatusArea != null) {
            javafx.application.Platform.runLater(() -> {
                String timestamp = java.time.LocalTime.now().format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
                ftpStatusArea.appendText("[" + timestamp + "] " + message + "\n");
                // Scroll automatico all'ultima riga
                ftpStatusArea.setScrollTop(Double.MAX_VALUE);
            });
        }
    }

    private void showFtpManagerDialog() {
        FtpManagerDialog dialog = new FtpManagerDialog(connection -> {
            // Quando la connessione è stabilita, apri nel pannello attivo
            com.totalcommander.services.FtpService ftpService = 
                com.totalcommander.services.FtpConnectionManager.getActiveConnection(connection);
            if (ftpService != null) {
                activePanel.connectToFtp(connection, ftpService);
                ftpConnectedPanel = activePanel;
                showFtpBar(connection);
                appendFtpStatus("Connesso a: " + connection.getHost() + ":" + connection.getPort());
                appendFtpStatus("Attesa risposta server...");
                
                // Avvia aggiornamento periodico della lista file
                startFtpAutoRefresh();
            }
        });
        dialog.showAndWait();
    }
    
    private void showDiskStructure() {
        // TODO: Implementare visualizzazione struttura disco
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Struttura del Disco");
        alert.setHeaderText("Funzionalità in sviluppo");
        alert.setContentText("La visualizzazione della struttura del disco sarà disponibile a breve.");
        alert.showAndWait();
    }
    
    private void showFindDialog() {
        // TODO: Implementare dialog di ricerca file
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trova");
        alert.setHeaderText("Funzionalità in sviluppo");
        alert.setContentText("La ricerca file sarà disponibile a breve.");
        alert.showAndWait();
    }
    
    private void showFindInBackground() {
        // TODO: Implementare ricerca in background
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Trova in Processo Sparato");
        alert.setHeaderText("Funzionalità in sviluppo");
        alert.setContentText("La ricerca in background sarà disponibile a breve.");
        alert.showAndWait();
    }
    
    private void showSystemInfo() {
        StringBuilder info = new StringBuilder();
        info.append("Sistema Operativo: ").append(System.getProperty("os.name")).append("\n");
        info.append("Versione OS: ").append(System.getProperty("os.version")).append("\n");
        info.append("Architettura: ").append(System.getProperty("os.arch")).append("\n");
        info.append("Java Version: ").append(System.getProperty("java.version")).append("\n");
        info.append("Java Vendor: ").append(System.getProperty("java.vendor")).append("\n");
        info.append("User Home: ").append(System.getProperty("user.home")).append("\n");
        info.append("User Name: ").append(System.getProperty("user.name")).append("\n");
        info.append("Memoria Totale: ").append(formatBytes(Runtime.getRuntime().totalMemory())).append("\n");
        info.append("Memoria Libera: ").append(formatBytes(Runtime.getRuntime().freeMemory())).append("\n");
        info.append("Memoria Massima: ").append(formatBytes(Runtime.getRuntime().maxMemory())).append("\n");
        
        // Informazioni disco
        java.io.File[] roots = java.io.File.listRoots();
        info.append("\n--- Unità Disco ---\n");
        for (java.io.File root : roots) {
            info.append(root.getAbsolutePath()).append(" - ");
            info.append("Totale: ").append(formatBytes(root.getTotalSpace())).append(" | ");
            info.append("Libero: ").append(formatBytes(root.getUsableSpace())).append("\n");
        }
        
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Informazioni Sistema");
        alert.setHeaderText("Dettagli del Sistema");
        alert.setContentText(info.toString());
        alert.getDialogPane().setPrefWidth(500);
        alert.showAndWait();
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private void openTerminal() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            ProcessBuilder pb;
            if (os.contains("win")) {
                pb = new ProcessBuilder("cmd.exe", "/c", "start", "cmd.exe");
            } else if (os.contains("mac")) {
                pb = new ProcessBuilder("open", "-a", "Terminal");
            } else {
                pb = new ProcessBuilder("xterm");
            }
            pb.directory(activePanel.getCurrentPath().toFile());
            pb.start();
        } catch (Exception e) {
            showError("Errore", "Impossibile aprire il terminal: " + e.getMessage());
        }
    }
    
    private void openPowerShell() {
        try {
            String os = System.getProperty("os.name").toLowerCase();
            if (os.contains("win")) {
                ProcessBuilder pb = new ProcessBuilder("powershell.exe");
                pb.directory(activePanel.getCurrentPath().toFile());
                pb.start();
            } else {
                showError("Errore", "PowerShell è disponibile solo su Windows.");
            }
        } catch (Exception e) {
            showError("Errore", "Impossibile aprire PowerShell: " + e.getMessage());
        }
    }
    
    private void openSpecialFolder(String folderName) {
        try {
            String path = "";
            if (folderName.equals("Desktop")) {
                path = System.getProperty("user.home") + java.io.File.separator + "Desktop";
            } else if (folderName.equals("Documents")) {
                path = System.getProperty("user.home") + java.io.File.separator + "Documents";
            } else if (folderName.equals("Downloads")) {
                path = System.getProperty("user.home") + java.io.File.separator + "Downloads";
            }
            
            java.io.File folder = new java.io.File(path);
            if (folder.exists() && folder.isDirectory()) {
                activePanel.navigateToPath(java.nio.file.Paths.get(path));
            } else {
                showError("Errore", "La cartella " + folderName + " non esiste.");
            }
        } catch (Exception e) {
            showError("Errore", "Impossibile aprire la cartella: " + e.getMessage());
        }
    }
    
    private void showTransferManager() {
        if (transferManagerDialog == null) {
            transferManagerDialog = new TransferManagerDialog(transferService);
            transferManagerDialog.setOnCloseRequest(e -> {
                // Non chiudere completamente, solo nascondere
                transferManagerDialog.hide();
            });
        }
        
        if (!transferManagerDialog.isShowing()) {
            transferManagerDialog.show();
        } else {
            transferManagerDialog.toFront();
        }
    }
    
    public TransferService getTransferService() {
        return transferService;
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

