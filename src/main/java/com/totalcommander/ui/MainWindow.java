package com.totalcommander.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import com.totalcommander.ui.panels.FilePanel;
import com.totalcommander.services.FileOperationService;
import com.totalcommander.services.TransferService;
import com.totalcommander.services.LanguageService;
import com.totalcommander.services.SettingsService;
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
    private LanguageService languageService;
    private SettingsService settingsService;
    private MenuBar menuBar;
    private TransferManagerDialog transferManagerDialog;
    
    // Barra FTP
    private HBox ftpBar;
    private TextArea ftpStatusArea;
    private ComboBox<String> ftpTransmissionMode;
    private FilePanel ftpConnectedPanel;
    
    // Riferimenti per aggiornamento traduzioni
    private ToolBar mainToolbar;
    private HBox buttonBar;
    private VBox mainContainer;

    public MainWindow() {
        this.fileOperationService = new FileOperationService();
        this.transferService = new TransferService();
        this.languageService = LanguageService.getInstance();
        this.settingsService = SettingsService.getInstance();
        
        // Carica la lingua salvata
        SettingsService.ApplicationSettings settings = settingsService.getSettings();
        if (settings.language != null && !settings.language.isEmpty()) {
            languageService.loadLanguage(settings.language);
        }
        
        // Configura callback per aggiornare lo status FTP
        transferService.setStatusCallback((type, message) -> {
            if (type.equals("FTP")) {
                appendFtpStatus(message);
            }
        });
        
        initializeUI();
        setupKeyboardShortcuts();
        loadPanelPaths();
    }
    
    /**
     * Carica i percorsi dei pannelli dalle impostazioni
     */
    private void loadPanelPaths() {
        SettingsService.ApplicationSettings settings = settingsService.getSettings();
        if (settings.leftPanelPath != null && !settings.leftPanelPath.isEmpty()) {
            try {
                java.nio.file.Path leftPath = java.nio.file.Paths.get(settings.leftPanelPath);
                if (java.nio.file.Files.exists(leftPath)) {
                    leftPanel.navigateToPath(leftPath);
                }
            } catch (Exception e) {
                // Ignora errori
            }
        }
        if (settings.rightPanelPath != null && !settings.rightPanelPath.isEmpty()) {
            try {
                java.nio.file.Path rightPath = java.nio.file.Paths.get(settings.rightPanelPath);
                if (java.nio.file.Files.exists(rightPath)) {
                    rightPanel.navigateToPath(rightPath);
                }
            } catch (Exception e) {
                // Ignora errori
            }
        }
    }
    
    /**
     * Configura i listener per salvare i percorsi dei pannelli quando cambiano
     */
    private void setupPanelPathListeners() {
        // Salva i percorsi periodicamente (ogni 3 secondi) per evitare troppi salvataggi
        javafx.animation.Timeline timeline = new javafx.animation.Timeline(
            new javafx.animation.KeyFrame(javafx.util.Duration.seconds(3), e -> savePanelPaths())
        );
        timeline.setCycleCount(javafx.animation.Animation.INDEFINITE);
        timeline.play();
    }
    
    /**
     * Salva i percorsi correnti dei pannelli
     */
    private void savePanelPaths() {
        if (leftPanel != null && rightPanel != null && !leftPanel.isFtpMode() && !rightPanel.isFtpMode()) {
            try {
                String leftPath = leftPanel.getCurrentPath().toString();
                String rightPath = rightPanel.getCurrentPath().toString();
                settingsService.setPanelPaths(leftPath, rightPath);
            } catch (Exception e) {
                // Ignora errori durante il salvataggio
            }
        }
    }
    
    /**
     * Metodo helper per ottenere traduzioni
     */
    private String tr(String key) {
        return languageService.getTranslation(key, key);
    }

    private void initializeUI() {
        // Menu bar
        menuBar = createMenuBar();
        setTop(menuBar);

        // Container principale per toolbar e pannelli
        mainContainer = new VBox();
        
        // Prima toolbar (toolbar principale)
        mainToolbar = createMainToolbar();
        
        // Pannelli file (devono essere inizializzati prima della barra unità)
        HBox panelsContainer = new HBox(5);
        panelsContainer.setPadding(new Insets(5));
        
        leftPanel = new FilePanel(this::setActivePanel);
        rightPanel = new FilePanel(this::setActivePanel);
        
        // Aggiungi listener per salvare i percorsi quando cambiano
        setupPanelPathListeners();
        
        panelsContainer.getChildren().addAll(leftPanel, rightPanel);
        HBox.setHgrow(leftPanel, Priority.ALWAYS);
        HBox.setHgrow(rightPanel, Priority.ALWAYS);
        
        // Seconda toolbar (barra unità) - creata dopo i pannelli
        HBox driveBar = createDriveBar();
        
        mainContainer.getChildren().addAll(mainToolbar, driveBar, panelsContainer);
        VBox.setVgrow(panelsContainer, Priority.ALWAYS);
        
        setCenter(mainContainer);
        
        // Container per barra bottoni e barra FTP
        VBox bottomContainer = new VBox();
        
        // Barra di bottoni in basso
        buttonBar = createButtonBar();
        
        // Barra FTP (inizialmente nascosta)
        HBox ftpBar = createFtpBar();
        ftpBar.setVisible(false);
        ftpBar.setManaged(false);
        
        bottomContainer.getChildren().addAll(buttonBar, ftpBar);
        setBottom(bottomContainer);
        
        // Pannello attivo di default
        activePanel = leftPanel;
        leftPanel.setActive(true);
        
        // Salva i percorsi iniziali
        savePanelPaths();
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
        // Carica la modalità salvata
        SettingsService.ApplicationSettings settings = settingsService.getSettings();
        if (settings.ftpTransmissionMode != null && !settings.ftpTransmissionMode.isEmpty()) {
            transmissionMode.setValue(settings.ftpTransmissionMode);
        } else {
            transmissionMode.setValue("Binaria (archivi, doc ecc.)");
        }
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
        
        Button visualizzaBtn = new Button(tr("button.view"));
        visualizzaBtn.setMaxWidth(Double.MAX_VALUE);
        visualizzaBtn.setOnAction(e -> activePanel.openSelectedItem());
        
        Button modificaBtn = new Button(tr("button.edit"));
        modificaBtn.setMaxWidth(Double.MAX_VALUE);
        modificaBtn.setOnAction(e -> activePanel.editSelectedFile());
        
        Button copiaBtn = new Button(tr("button.copy"));
        copiaBtn.setMaxWidth(Double.MAX_VALUE);
        copiaBtn.setOnAction(e -> copySelected());
        
        Button spostaBtn = new Button(tr("button.move"));
        spostaBtn.setMaxWidth(Double.MAX_VALUE);
        spostaBtn.setOnAction(e -> moveSelected());
        
        Button creaCartellaBtn = new Button(tr("button.createfolder"));
        creaCartellaBtn.setMaxWidth(Double.MAX_VALUE);
        creaCartellaBtn.setOnAction(e -> activePanel.createNewFolder());
        
        Button eliminaBtn = new Button(tr("button.delete"));
        eliminaBtn.setMaxWidth(Double.MAX_VALUE);
        eliminaBtn.setOnAction(e -> activePanel.deleteSelected());
        
        Button esciBtn = new Button(tr("button.exit"));
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
    
    private ToolBar createMainToolbar() {
        ToolBar toolbar = new ToolBar();
        toolbar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        // Separatori verticali per raggruppare i bottoni
        Separator separator1 = new Separator();
        separator1.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Separator separator2 = new Separator();
        separator2.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Separator separator3 = new Separator();
        separator3.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Separator separator4 = new Separator();
        separator4.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Separator separator5 = new Separator();
        separator5.setOrientation(javafx.geometry.Orientation.VERTICAL);
        Separator separator6 = new Separator();
        separator6.setOrientation(javafx.geometry.Orientation.VERTICAL);
        
        // Stile comune per i bottoni
        String buttonStyle = "-fx-background-color: transparent; -fx-border-color: transparent; -fx-padding: 5;";
        String buttonHoverStyle = "-fx-background-color: #e0e0e0; -fx-border-color: transparent; -fx-padding: 5;";
        
        // Gruppo 1: Refresh, Copia cartelle, Nuova cartella
        Button refreshBtn = createToolbarButton("↻", tr("toolbar.refresh"), buttonStyle, buttonHoverStyle);
        refreshBtn.setOnAction(e -> activePanel.refresh());
        
        Button copyFoldersBtn = createToolbarButton("📁", tr("toolbar.copyfolders"), buttonStyle, buttonHoverStyle);
        copyFoldersBtn.setOnAction(e -> copySelected());
        
        Button newFolderBtn = createToolbarButton("📁", tr("toolbar.newfolder"), buttonStyle, buttonHoverStyle);
        newFolderBtn.setStyle(buttonStyle + " -fx-background-color: #e3f2fd;");
        newFolderBtn.setOnAction(e -> activePanel.createNewFolder());
        
        // Gruppo 2: Visualizzatore, Sposta, Download
        Button viewerBtn = createToolbarButton("🖼", tr("toolbar.viewer"), buttonStyle, buttonHoverStyle);
        viewerBtn.setOnAction(e -> activePanel.openSelectedItem());
        
        Button moveBtn = createToolbarButton("→", tr("toolbar.move"), buttonStyle, buttonHoverStyle);
        moveBtn.setOnAction(e -> moveSelected());
        
        Button downloadBtn = createToolbarButton("↓", tr("toolbar.download"), buttonStyle, buttonHoverStyle);
        downloadBtn.setOnAction(e -> copySelected());
        
        // Gruppo 3: Impostazioni, Navigazione
        Button settingsBtn = createToolbarButton("⚙", tr("toolbar.settings"), buttonStyle, buttonHoverStyle);
        settingsBtn.setOnAction(e -> showSettingsDialog());
        
        Button backBtn = createToolbarButton("←", tr("toolbar.back"), buttonStyle, buttonHoverStyle);
        backBtn.setOnAction(e -> {
            // TODO: Implementare navigazione indietro
        });
        
        Button forwardBtn = createToolbarButton("→", tr("toolbar.forward"), buttonStyle, buttonHoverStyle);
        forwardBtn.setOnAction(e -> {
            // TODO: Implementare navigazione avanti
        });
        
        // Gruppo 4: Upload, Download
        Button uploadBtn = createToolbarButton("⬆", tr("toolbar.upload"), buttonStyle, buttonHoverStyle);
        uploadBtn.setOnAction(e -> copySelected());
        
        Button downloadBtn2 = createToolbarButton("⬇", tr("toolbar.download"), buttonStyle, buttonHoverStyle);
        downloadBtn2.setOnAction(e -> copySelected());
        
        // Gruppo 5: FTP, URL
        Button ftpBtn = createToolbarButton("FTP", tr("toolbar.connectftp"), buttonStyle, buttonHoverStyle);
        ftpBtn.setOnAction(e -> showFtpManagerDialog());
        
        Button urlBtn = createToolbarButton("URL", tr("toolbar.connecturl"), buttonStyle, buttonHoverStyle);
        urlBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("URL");
            alert.setHeaderText("Connessione URL");
            alert.setContentText("Funzionalità in sviluppo.");
            alert.showAndWait();
        });
        
        // Gruppo 6: Ricerca, Modifica, Sincronizza, Clipboard, Notepad, Nuova cartella vuota
        Button searchBtn = createToolbarButton("🔍", tr("toolbar.search"), buttonStyle, buttonHoverStyle);
        searchBtn.setOnAction(e -> showFindDialog());
        
        Button editBtn = createToolbarButton("✏", tr("toolbar.edit"), buttonStyle, buttonHoverStyle);
        editBtn.setOnAction(e -> activePanel.editSelectedFile());
        
        Button syncBtn = createToolbarButton("⇄", tr("toolbar.sync"), buttonStyle, buttonHoverStyle);
        syncBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Sincronizza");
            alert.setHeaderText("Sincronizzazione cartelle");
            alert.setContentText("Funzionalità in sviluppo.");
            alert.showAndWait();
        });
        
        Button clipboardBtn = createToolbarButton("📋", tr("toolbar.clipboard"), buttonStyle, buttonHoverStyle);
        clipboardBtn.setOnAction(e -> {
            // TODO: Implementare clipboard
        });
        
        Button notepadBtn = createToolbarButton("📝", tr("toolbar.notepad"), buttonStyle, buttonHoverStyle);
        notepadBtn.setOnAction(e -> {
            try {
                java.io.File tempFile = java.io.File.createTempFile("notepad_", ".txt");
                java.awt.Desktop.getDesktop().open(tempFile);
            } catch (Exception ex) {
                showError("Errore", "Impossibile aprire notepad: " + ex.getMessage());
            }
        });
        
        Button newEmptyFolderBtn = createToolbarButton("📂", tr("toolbar.newemptyfolder"), buttonStyle, buttonHoverStyle);
        newEmptyFolderBtn.setOnAction(e -> activePanel.createNewFolder());
        
        toolbar.getItems().addAll(
            refreshBtn, copyFoldersBtn, newFolderBtn, separator1,
            viewerBtn, moveBtn, downloadBtn, separator2,
            settingsBtn, backBtn, forwardBtn, separator3,
            uploadBtn, downloadBtn2, separator4,
            ftpBtn, urlBtn, separator5,
            searchBtn, editBtn, syncBtn, clipboardBtn, notepadBtn, newEmptyFolderBtn, separator6
        );
        
        return toolbar;
    }
    
    private Button createToolbarButton(String text, String tooltip, String style, String hoverStyle) {
        Button btn = new Button(text);
        btn.setTooltip(new Tooltip(tooltip));
        btn.setStyle(style);
        btn.setOnMouseEntered(e -> btn.setStyle(hoverStyle));
        btn.setOnMouseExited(e -> btn.setStyle(style));
        return btn;
    }
    
    private HBox createDriveBar() {
        HBox driveBar = new HBox(5);
        driveBar.setPadding(new Insets(5, 10, 5, 10));
        driveBar.setStyle("-fx-background-color: #ffffff; -fx-border-color: #d0d0d0; -fx-border-width: 0 0 1 0;");
        
        // Carica le unità disponibili
        java.io.File[] roots = java.io.File.listRoots();
        
        // Pannello sinistro
        HBox leftDrives = new HBox(3);
        leftDrives.setSpacing(3);
        for (java.io.File root : roots) {
            Button driveBtn = createDriveButton(root, leftPanel, leftDrives);
            leftDrives.getChildren().add(driveBtn);
        }
        
        // Separatore
        Separator separator = new Separator();
        separator.setOrientation(javafx.geometry.Orientation.VERTICAL);
        separator.setPrefHeight(30);
        
        // Pannello destro
        HBox rightDrives = new HBox(3);
        rightDrives.setSpacing(3);
        for (java.io.File root : roots) {
            Button driveBtn = createDriveButton(root, rightPanel, rightDrives);
            rightDrives.getChildren().add(driveBtn);
        }
        
        // Icona rete (solo per pannello destro)
        Button networkBtn = new Button("🌐");
        networkBtn.setPrefWidth(40);
        networkBtn.setPrefHeight(30);
        networkBtn.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #bdbdbd; -fx-border-radius: 3;");
        networkBtn.setTooltip(new Tooltip(tr("toolbar.network")));
        networkBtn.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Rete");
            alert.setHeaderText("Navigazione rete");
            alert.setContentText("Funzionalità in sviluppo.");
            alert.showAndWait();
        });
        rightDrives.getChildren().add(networkBtn);
        
        driveBar.getChildren().addAll(leftDrives, separator, rightDrives);
        HBox.setHgrow(leftDrives, Priority.ALWAYS);
        HBox.setHgrow(rightDrives, Priority.ALWAYS);
        
        return driveBar;
    }
    
    private Button createDriveButton(java.io.File root, FilePanel panel, HBox parentContainer) {
        String driveLetter = root.getAbsolutePath().substring(0, 1);
        Button driveBtn = new Button(driveLetter.toUpperCase());
        driveBtn.setPrefWidth(40);
        driveBtn.setPrefHeight(30);
        
        // Determina il tipo di unità
        String driveType = "HDD";
        try {
            java.nio.file.FileStore store = java.nio.file.Files.getFileStore(root.toPath());
            String type = store.type();
            if (type != null && (type.contains("CD") || type.contains("DVD"))) {
                driveType = "CD";
            }
        } catch (Exception e) {
            // Usa default
        }
        
        // Stile iniziale
        String normalStyle = "-fx-background-color: #f5f5f5; -fx-border-color: #bdbdbd; -fx-border-radius: 3;";
        String selectedStyle = "-fx-background-color: #e3f2fd; -fx-border-color: #90caf9; -fx-border-radius: 3;";
        
        // Evidenzia C: di default per il pannello sinistro (se i pannelli sono inizializzati)
        if (root.getAbsolutePath().startsWith("C:") && panel != null && panel == leftPanel) {
            driveBtn.setStyle(selectedStyle);
        } else {
            driveBtn.setStyle(normalStyle);
        }
        
        driveBtn.setOnAction(e -> {
            // Controlla che il pannello non sia null
            if (panel == null) {
                showError("Errore", "Pannello non disponibile. Riprova.");
                return;
            }
            
            // Rimuovi evidenziazione da tutti i bottoni nel container
            for (javafx.scene.Node node : parentContainer.getChildren()) {
                if (node instanceof Button) {
                    Button btn = (Button) node;
                    if (!btn.getText().equals("🌐")) {
                        btn.setStyle(normalStyle);
                    }
                }
            }
            // Evidenzia il bottone selezionato
            driveBtn.setStyle(selectedStyle);
            
            // Naviga al drive
            try {
                panel.navigateToPath(root.toPath());
                setActivePanel(panel);
            } catch (Exception ex) {
                showError("Errore", "Impossibile navigare al drive: " + ex.getMessage());
            }
        });
        
        return driveBtn;
    }

    private MenuBar createMenuBar() {
        MenuBar menuBar = new MenuBar();
        
        // Menu File
        Menu fileMenu = new Menu(tr("menu.file"));
        MenuItem newFolder = new MenuItem(tr("menu.file.newfolder"));
        newFolder.setOnAction(e -> activePanel.createNewFolder());
        MenuItem delete = new MenuItem(tr("menu.file.delete"));
        delete.setOnAction(e -> activePanel.deleteSelected());
        MenuItem rename = new MenuItem(tr("menu.file.rename"));
        rename.setOnAction(e -> activePanel.renameSelected());
        MenuItem multiRename = new MenuItem(tr("menu.file.multirename"));
        multiRename.setOnAction(e -> showMultiRenameDialog());
        fileMenu.getItems().addAll(newFolder, new SeparatorMenuItem(), delete, rename, multiRename);
        
        // Menu Modifica
        Menu editMenu = new Menu(tr("menu.edit"));
        MenuItem copy = new MenuItem(tr("menu.edit.copy"));
        copy.setOnAction(e -> copySelected());
        MenuItem move = new MenuItem(tr("menu.edit.move"));
        move.setOnAction(e -> moveSelected());
        MenuItem edit = new MenuItem(tr("menu.edit.edit"));
        edit.setOnAction(e -> activePanel.editSelectedFile());
        editMenu.getItems().addAll(copy, move, new SeparatorMenuItem(), edit);
        
        // Menu Archivi
        Menu archiveMenu = new Menu(tr("menu.archive"));
        MenuItem createArchive = new MenuItem(tr("menu.archive.create"));
        createArchive.setOnAction(e -> showCreateArchiveDialog());
        MenuItem extractArchive = new MenuItem(tr("menu.archive.extract"));
        extractArchive.setOnAction(e -> activePanel.extractArchive());
        archiveMenu.getItems().addAll(createArchive, extractArchive);
        
        // Menu FTP
        Menu ftpMenu = new Menu(tr("menu.ftp"));
        MenuItem manageConnections = new MenuItem(tr("menu.ftp.connect"));
        manageConnections.setOnAction(e -> showFtpManagerDialog());
        ftpMenu.getItems().addAll(manageConnections);
        
        // Menu Comandi
        Menu commandsMenu = new Menu(tr("menu.commands"));
        
        MenuItem diskStructure = new MenuItem(tr("menu.commands.diskstructure"));
        diskStructure.setOnAction(e -> showDiskStructure());
        
        MenuItem find = new MenuItem(tr("menu.commands.find"));
        find.setOnAction(e -> showFindDialog());
        
        MenuItem findInBackground = new MenuItem(tr("menu.commands.findbackground"));
        findInBackground.setOnAction(e -> showFindInBackground());
        
        MenuItem systemInfo = new MenuItem(tr("menu.commands.systeminfo"));
        systemInfo.setOnAction(e -> showSystemInfo());
        
        Menu terminalMenu = new Menu(tr("menu.commands.terminal"));
        MenuItem openTerminal = new MenuItem(tr("menu.commands.terminal"));
        openTerminal.setOnAction(e -> openTerminal());
        MenuItem openPowerShell = new MenuItem(tr("menu.commands.powershell"));
        openPowerShell.setOnAction(e -> openPowerShell());
        terminalMenu.getItems().addAll(openTerminal, openPowerShell);
        
        Menu openWindowMenu = new Menu(tr("menu.commands.openwindow"));
        MenuItem openDesktop = new MenuItem(tr("menu.commands.desktop"));
        openDesktop.setOnAction(e -> openSpecialFolder("Desktop"));
        MenuItem openDocuments = new MenuItem(tr("menu.commands.documents"));
        openDocuments.setOnAction(e -> openSpecialFolder("Documents"));
        MenuItem openDownloads = new MenuItem(tr("menu.commands.downloads"));
        openDownloads.setOnAction(e -> openSpecialFolder("Downloads"));
        openWindowMenu.getItems().addAll(openDesktop, openDocuments, openDownloads);
        
        MenuItem transferManager = new MenuItem(tr("menu.commands.transfermanager"));
        transferManager.setOnAction(e -> showTransferManager());
        
        commandsMenu.getItems().addAll(diskStructure, find, findInBackground, 
                                      new SeparatorMenuItem(), systemInfo,
                                      new SeparatorMenuItem(), terminalMenu, openWindowMenu,
                                      new SeparatorMenuItem(), transferManager);
        
        // Menu Impostazioni
        Menu settingsMenu = new Menu(tr("menu.settings"));
        MenuItem settings = new MenuItem(tr("menu.settings.settings"));
        settings.setOnAction(e -> showSettingsDialog());
        settingsMenu.getItems().addAll(settings);
        
        menuBar.getMenus().addAll(fileMenu, editMenu, archiveMenu, ftpMenu, commandsMenu, settingsMenu);
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
        
        // Configura callback per aggiornare le cartelle quando i trasferimenti sono completati
        transferService.setOnAllTransfersComplete(() -> {
            // Aggiorna la cartella di destinazione
            if (sourcePanel.isFtpMode()) {
                // Download: aggiorna il pannello locale di destinazione
                targetPanel.refresh();
            } else {
                // Upload: aggiorna il pannello FTP di destinazione
                if (targetPanel.isFtpMode()) {
                    targetPanel.refresh();
                }
            }
        });
        
        if (sourcePanel.isFtpMode()) {
            // Download da FTP a locale
            com.totalcommander.services.FtpService ftpService = sourcePanel.getFtpService();
            List<String> ftpPaths = sourcePanel.getSelectedFtpPaths();
            java.io.File localDestinationDir = targetPanel.getCurrentPath().toFile();
            
            appendFtpStatus("Aggiunti " + ftpPaths.size() + " elementi alla coda di download...");
            
            // Aggiungi tutti i file/cartelle alla coda
            transferService.queueDownload(ftpPaths, localDestinationDir, ftpService);
        } else {
            // Upload da locale a FTP
            com.totalcommander.services.FtpService ftpService = targetPanel.getFtpService();
            String remotePath = targetPanel.getCurrentFtpPath();
            
            appendFtpStatus("Aggiunti " + selectedFiles.size() + " elementi alla coda di upload...");
            
            // Aggiungi tutti i file/cartelle alla coda
            transferService.queueUpload(selectedFiles, remotePath, ftpService);
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
                    
                    // Salva la modalità FTP nelle impostazioni
                    String mode = binary ? "Binaria (archivi, doc ecc.)" : "ASCII (testo)";
                    settingsService.setFtpTransmissionMode(mode);
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
        DiskStructureDialog dialog = new DiskStructureDialog();
        dialog.show();
    }
    
    private void showFindDialog() {
        FindFilesDialog dialog = new FindFilesDialog();
        dialog.show();
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
                // Usa start per aprire PowerShell in una nuova finestra
                String currentDir = activePanel.getCurrentPath().toString();
                ProcessBuilder pb = new ProcessBuilder("cmd.exe", "/c", "start", "powershell.exe", 
                    "-NoExit", "-Command", "cd '" + currentDir + "'");
                pb.start();
            } else {
                showError("Errore", "PowerShell è disponibile solo su Windows.");
            }
        } catch (Exception e) {
            showError("Errore", "Impossibile aprire PowerShell: " + e.getMessage());
            e.printStackTrace();
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
    
    private void showSettingsDialog() {
        SettingsDialog dialog = new SettingsDialog();
        dialog.showAndWait();
        // Dopo la chiusura del dialog, ricrea il menu bar e le toolbar per applicare le traduzioni
        if (menuBar != null) {
            menuBar = createMenuBar();
            setTop(menuBar);
        }
        // Ricrea anche la toolbar principale e la barra bottoni
        if (mainContainer != null && mainContainer.getChildren().size() >= 3) {
            mainToolbar = createMainToolbar();
            HBox newDriveBar = createDriveBar();
            mainContainer.getChildren().set(0, mainToolbar);
            mainContainer.getChildren().set(1, newDriveBar);
        }
        // Ricrea la barra bottoni in basso
        VBox bottomContainer = (VBox) getBottom();
        if (bottomContainer != null && !bottomContainer.getChildren().isEmpty()) {
            buttonBar = createButtonBar();
            bottomContainer.getChildren().set(0, buttonBar);
        }
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        
        // Usa un TextArea per rendere il contenuto selezionabile e copiabile
        TextArea textArea = new TextArea(message);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        textArea.setStyle("-fx-font-family: monospace; -fx-font-size: 12px;");
        
        // Imposta la dimensione preferita
        textArea.setPrefRowCount(Math.min(10, message.split("\n").length + 2));
        textArea.setPrefColumnCount(60);
        
        alert.getDialogPane().setContent(textArea);
        alert.getDialogPane().setPrefWidth(600);
        
        // Aggiungi pulsante "Copia"
        ButtonType copyButtonType = new ButtonType("Copia", ButtonBar.ButtonData.LEFT);
        alert.getButtonTypes().add(copyButtonType);
        
        // Gestisci il click su "Copia"
        alert.showAndWait().ifPresent(response -> {
            if (response == copyButtonType) {
                String textToCopy = title + "\n\n" + message;
                Clipboard clipboard = Clipboard.getSystemClipboard();
                ClipboardContent content = new ClipboardContent();
                content.putString(textToCopy);
                clipboard.setContent(content);
                
                // Mostra conferma breve
                Alert confirmAlert = new Alert(Alert.AlertType.INFORMATION);
                confirmAlert.setTitle("Copiato");
                confirmAlert.setHeaderText(null);
                confirmAlert.setContentText("Testo copiato negli appunti!");
                confirmAlert.showAndWait();
            }
        });
    }
}

