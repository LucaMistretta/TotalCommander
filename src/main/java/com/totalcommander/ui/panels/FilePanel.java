package com.totalcommander.ui.panels;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.ListChangeListener;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import com.totalcommander.models.FileItem;
import com.totalcommander.models.FtpConnection;
import com.totalcommander.services.FileOperationService;
import com.totalcommander.services.ArchiveService;
import com.totalcommander.services.FtpService;
import org.apache.commons.net.ftp.FTPFile;
import java.io.File;
import java.nio.file.FileStore;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Consumer;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Pannello file con lista file e navigazione
 */
public class FilePanel extends VBox {
    
    private ComboBox<String> driveComboBox;
    private TextField pathField;
    private TableView<FileItem> fileTable;
    private ObservableList<FileItem> fileItems;
    private Path currentPath;
    private FileOperationService fileOperationService;
    private ArchiveService archiveService;
    private Consumer<FilePanel> onActivate;
    private boolean isActive;
    private Label statusLabel;
    
    // Supporto FTP
    private boolean isFtpMode = false;
    private FtpService ftpService;
    private FtpConnection ftpConnection;
    private String currentFtpPath = "/";
    private boolean isRefreshing = false; // Flag per evitare refresh durante operazioni

    public FilePanel(Consumer<FilePanel> onActivate) {
        try {
        this.onActivate = onActivate;
        this.fileOperationService = new FileOperationService();
        this.archiveService = new ArchiveService();
        this.fileItems = FXCollections.observableArrayList();
            
            // Prova a caricare la directory home, altrimenti usa la directory corrente
            try {
        this.currentPath = Paths.get(System.getProperty("user.home"));
            } catch (Exception e) {
                this.currentPath = Paths.get(".");
            }
        
        initializeUI();
        loadDirectory(currentPath);
        } catch (Exception e) {
            System.err.println("Errore nell'inizializzazione di FilePanel: " + e.getMessage());
            e.printStackTrace();
            // Continua comunque, ma con una directory di default
            this.currentPath = Paths.get(".");
            initializeUI();
        }
    }

    private void initializeUI() {
        setSpacing(5);
        setPadding(new Insets(5));
        
        // Attiva il pannello quando si clicca su qualsiasi parte
        setOnMouseClicked(e -> {
            if (onActivate != null && !isActive) {
                onActivate.accept(this);
            }
        });

        // ComboBox per selezionare le unità disco
        driveComboBox = new ComboBox<>();
        driveComboBox.setPrefWidth(120);
        loadAvailableDrives();
        driveComboBox.setOnAction(e -> {
            String selectedDrive = driveComboBox.getSelectionModel().getSelectedItem();
            if (selectedDrive != null) {
                // Estrai solo il percorso dell'unità (es. "C:\" da "C:\ (Windows)")
                String drivePath = selectedDrive.split(" ")[0];
                if (drivePath.endsWith("\\") || drivePath.endsWith("/")) {
                    navigateToPath(Paths.get(drivePath));
                } else {
                    navigateToPath(Paths.get(drivePath + "\\"));
                }
            }
        });

        // Barra percorso
        HBox pathBar = new HBox(5);
        pathField = new TextField();
        pathField.setOnAction(e -> {
            if (isFtpMode) {
                navigateToFtpPath(pathField.getText());
            } else {
                try {
                    navigateToPath(Paths.get(pathField.getText()));
                } catch (Exception ex) {
                    showError("Errore", "Percorso non valido: " + ex.getMessage());
                }
            }
        });
        Button browseButton = new Button("Sfoglia");
        browseButton.setOnAction(e -> browseDirectory());
        pathBar.getChildren().addAll(driveComboBox, new Label("Percorso:"), pathField, browseButton);
        HBox.setHgrow(pathField, Priority.ALWAYS);
        
        // Attiva pannello quando si clicca sulla barra percorso
        pathBar.setOnMouseClicked(e -> {
            if (onActivate != null && !isActive) {
                onActivate.accept(this);
            }
        });
        
        // Tabella file
        fileTable = new TableView<>();
        fileTable.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
        fileTable.setFixedCellSize(25); // Imposta altezza fissa per calcolo corretto dell'indice
        
        TableColumn<FileItem, String> nameColumn = new TableColumn<>("Nome");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(300);
        
        TableColumn<FileItem, String> sizeColumn = new TableColumn<>("Dimensione");
        sizeColumn.setCellValueFactory(new PropertyValueFactory<>("size"));
        sizeColumn.setPrefWidth(100);
        
        TableColumn<FileItem, String> typeColumn = new TableColumn<>("Tipo");
        typeColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        typeColumn.setPrefWidth(100);
        
        TableColumn<FileItem, String> dateColumn = new TableColumn<>("Data Modifica");
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("modifiedDate"));
        dateColumn.setPrefWidth(150);
        
        fileTable.getColumns().addAll(nameColumn, sizeColumn, typeColumn, dateColumn);
        fileTable.setItems(fileItems);
        
        // Abilita selezione multipla
        fileTable.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        // Doppio click per aprire
        fileTable.setOnMouseClicked(e -> {
            // Attiva sempre il pannello quando si clicca sulla tabella
            if (onActivate != null && !isActive) {
                onActivate.accept(this);
            }
            
            if (e.getButton() == MouseButton.PRIMARY && e.getClickCount() == 2) {
                openSelectedItem();
            }
        });
        
        // Click per attivare pannello
        fileTable.setOnMousePressed(e -> {
            if (onActivate != null) {
                onActivate.accept(this);
            }
        });
        
        // Anche clic sulla barra percorso attiva il pannello
        pathField.setOnMouseClicked(e -> {
            if (onActivate != null) {
                onActivate.accept(this);
            }
        });
        
        pathField.setOnMousePressed(e -> {
            if (onActivate != null) {
                onActivate.accept(this);
            }
        });
        
        // Selezione multipla con drag del mouse destro
        setupMultiSelectionDrag();
        
        // Menu contestuale
        ContextMenu contextMenu = createContextMenu();
        fileTable.setContextMenu(contextMenu);
        
        // Setup selezione multipla con mouse destro
        setupMultiSelectionDrag();
        
        // Barra di stato con statistiche
        statusLabel = new Label();
        statusLabel.setPadding(new Insets(5));
        statusLabel.setStyle("-fx-background-color: #e0e0e0; -fx-font-size: 11px;");
        
        VBox.setVgrow(fileTable, Priority.ALWAYS);
        getChildren().addAll(pathBar, fileTable, statusLabel);
        
        // Aggiorna statistiche quando cambia la directory
        updateStatistics();
    }

    private ContextMenu createContextMenu() {
        ContextMenu menu = new ContextMenu();
        
        MenuItem open = new MenuItem("Apri");
        open.setOnAction(e -> openSelectedItem());
        
        MenuItem edit = new MenuItem("Modifica");
        edit.setOnAction(e -> editSelectedFile());
        
        MenuItem copy = new MenuItem("Copia");
        copy.setOnAction(e -> copySelected());
        
        MenuItem move = new MenuItem("Sposta");
        move.setOnAction(e -> moveSelected());
        
        MenuItem delete = new MenuItem("Elimina");
        delete.setOnAction(e -> deleteSelected());
        
        MenuItem rename = new MenuItem("Rinomina");
        rename.setOnAction(e -> renameSelected());
        
        MenuItem extract = new MenuItem("Estrai Archivio");
        extract.setOnAction(e -> extractArchive());
        
        menu.getItems().addAll(open, new SeparatorMenuItem(), edit, 
                              new SeparatorMenuItem(), copy, move, 
                              new SeparatorMenuItem(), delete, rename,
                              new SeparatorMenuItem(), extract);
        
        return menu;
    }

    private void loadDirectory(Path path) {
        if (isFtpMode) {
            loadFtpDirectory(currentFtpPath);
        } else {
            loadLocalDirectory(path);
        }
    }
    
    private void loadLocalDirectory(Path path) {
        try {
            File dir = path.toFile();
            if (!dir.exists() || !dir.isDirectory()) {
                return;
            }
            
            currentPath = path;
            pathField.setText(path.toString());
            fileItems.clear();
            
            // Aggiorna il ComboBox dell'unità se necessario
            updateDriveSelection();
            
            // Cartella parent
            if (path.getParent() != null) {
                fileItems.add(new FileItem("..", "Cartella", "", "", true));
            }
            
            // File e cartelle
            File[] files = dir.listFiles();
            if (files != null) {
                for (File file : files) {
                    fileItems.add(FileItem.fromFile(file));
                }
            }
            
            // Aggiorna statistiche dopo il caricamento
            updateStatistics();
        } catch (Exception e) {
            showError("Errore", "Impossibile caricare la directory: " + e.getMessage());
        }
    }
    
    private void loadFtpDirectory(String remotePath) {
        if (ftpService == null || !ftpService.isConnected()) {
            showError("Errore", "Connessione FTP non disponibile.");
            return;
        }
        
        try {
            if (remotePath == null || remotePath.isEmpty()) {
                remotePath = "/";
            }
            
            // Cambia directory se necessario
            if (!remotePath.equals(currentFtpPath)) {
                if (!ftpService.changeDirectory(remotePath)) {
                    showError("Errore", "Impossibile accedere alla directory: " + remotePath);
                    return;
                }
            }
            
            currentFtpPath = ftpService.getCurrentDirectory();
            pathField.setText("ftp://" + ftpConnection.getHost() + currentFtpPath);
            fileItems.clear();
            
            // Cartella parent (se non siamo alla root)
            if (!currentFtpPath.equals("/") && !currentFtpPath.isEmpty()) {
                fileItems.add(new FileItem("..", "Cartella", "", "", true));
            }
            
            // Lista file FTP
            FTPFile[] ftpFiles = ftpService.listFiles(currentFtpPath);
            if (ftpFiles != null) {
                for (FTPFile ftpFile : ftpFiles) {
                    String name = ftpFile.getName();
                    if (name.equals(".") || name.equals("..")) {
                        continue; // Salta . e ..
                    }
                    
                    boolean isDir = ftpFile.isDirectory();
                    String type = isDir ? "Cartella" : getFileExtension(name);
                    String size = isDir ? "<DIR>" : formatFileSize(ftpFile.getSize());
                    String date = formatFtpDate(ftpFile.getTimestamp());
                    
                    fileItems.add(new FileItem(name, type, size, date, isDir));
                }
            }
            
            // Aggiorna statistiche
            updateFtpStatistics();
        } catch (Exception e) {
            showError("Errore", "Impossibile caricare la directory FTP: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toUpperCase();
        }
        return "File";
    }
    
    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private String formatFtpDate(java.util.Calendar calendar) {
        if (calendar == null) {
            return "";
        }
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(calendar.getTime());
    }
    
    private void updateFtpStatistics() {
        int folderCount = 0;
        int fileCount = 0;
        long totalBytes = 0;
        
        for (FileItem item : fileItems) {
            if (item.getName().equals("..")) {
                continue;
            }
            if (item.isDirectory()) {
                folderCount++;
            } else {
                fileCount++;
                // Prova a estrarre la dimensione dal formato stringa
                String sizeStr = item.getSize();
                if (!sizeStr.equals("<DIR>")) {
                    try {
                        // Estrai numero dalla stringa (es. "1.23 MB" -> 1.23)
                        String[] parts = sizeStr.split(" ");
                        if (parts.length >= 2) {
                            double value = Double.parseDouble(parts[0]);
                            String unit = parts[1].toUpperCase();
                            if (unit.equals("B")) {
                                totalBytes += (long)value;
                            } else if (unit.equals("KB")) {
                                totalBytes += (long)(value * 1024);
                            } else if (unit.equals("MB")) {
                                totalBytes += (long)(value * 1024 * 1024);
                            } else if (unit.equals("GB")) {
                                totalBytes += (long)(value * 1024 * 1024 * 1024);
                            }
                        }
                    } catch (Exception e) {
                        // Ignora errori di parsing
                    }
                }
            }
        }
        
        double totalMB = totalBytes / (1024.0 * 1024.0);
        statusLabel.setText(String.format("Cartelle: %d | File: %d | Totale: %.2f MB | FTP: %s",
            folderCount, fileCount, totalMB, ftpConnection.getHost()));
    }
    
    /**
     * Connetti questo pannello a un server FTP
     */
    public void connectToFtp(FtpConnection connection, FtpService service) {
        this.isFtpMode = true;
        this.ftpConnection = connection;
        this.ftpService = service;
        this.currentFtpPath = connection.getInitialPath() != null ? connection.getInitialPath() : "/";
        
        // Nascondi il ComboBox delle unità in modalità FTP
        driveComboBox.setVisible(false);
        
        // Carica la directory FTP iniziale
        loadFtpDirectory(currentFtpPath);
    }
    
    /**
     * Disconnetti dal server FTP e torna alla modalità locale
     */
    public void disconnectFromFtp() {
        this.isFtpMode = false;
        this.ftpService = null;
        this.ftpConnection = null;
        this.currentFtpPath = "/";
        
        // Mostra di nuovo il ComboBox delle unità
        driveComboBox.setVisible(true);
        
        // Torna alla directory home locale
        try {
            this.currentPath = Paths.get(System.getProperty("user.home"));
            loadLocalDirectory(currentPath);
        } catch (Exception e) {
            this.currentPath = Paths.get(".");
            loadLocalDirectory(currentPath);
        }
    }
    
    public boolean isFtpMode() {
        return isFtpMode;
    }
    
    public FtpService getFtpService() {
        return ftpService;
    }
    
    public String getCurrentFtpPath() {
        return currentFtpPath;
    }
    
    public FtpConnection getFtpConnection() {
        return ftpConnection;
    }
    
    private void loadAvailableDrives() {
        ObservableList<String> drives = FXCollections.observableArrayList();
        File[] roots = File.listRoots();
        for (File root : roots) {
            String drivePath = root.getAbsolutePath();
            try {
                // Prova a ottenere il nome del volume
                String volumeName = getVolumeName(root.toPath());
                String displayName = drivePath;
                if (volumeName != null && !volumeName.isEmpty()) {
                    displayName = drivePath + " (" + volumeName + ")";
                }
                drives.add(displayName);
            } catch (Exception e) {
                drives.add(drivePath);
            }
        }
        driveComboBox.setItems(drives);
        
        // Seleziona l'unità corrente
        String currentDrive = getCurrentDrive();
        if (currentDrive != null) {
            driveComboBox.getSelectionModel().select(currentDrive);
        }
    }
    
    private String getCurrentDrive() {
        if (currentPath != null) {
            String pathStr = currentPath.toString();
            File[] roots = File.listRoots();
            for (File root : roots) {
                String rootPath = root.getAbsolutePath();
                if (pathStr.startsWith(rootPath)) {
                    try {
                        String volumeName = getVolumeName(root.toPath());
                        String displayName = rootPath;
                        if (volumeName != null && !volumeName.isEmpty()) {
                            displayName = rootPath + " (" + volumeName + ")";
                        }
                        return displayName;
                    } catch (Exception e) {
                        return rootPath;
                    }
                }
            }
        }
        return null;
    }
    
    private String getVolumeName(Path path) {
        try {
            FileStore store = Files.getFileStore(path);
            String name = store.name();
            if (name != null && !name.isEmpty() && !name.equals(path.getRoot().toString().replace("\\", "").replace("/", ""))) {
                return name;
            }
            // Su Windows, prova a ottenere il nome del volume dal tipo
            String type = store.type();
            if (type != null && !type.isEmpty()) {
                return type;
            }
        } catch (Exception e) {
            // Ignora errori
        }
        return null;
    }
    
    private void updateDriveSelection() {
        String currentDrive = getCurrentDrive();
        if (currentDrive != null) {
            // Cerca il drive nella lista
            for (String drive : driveComboBox.getItems()) {
                if (drive.startsWith(currentDrive.split(" ")[0])) {
                    driveComboBox.getSelectionModel().select(drive);
                    break;
                }
            }
        }
    }
    
    private void updateStatistics() {
        int folderCount = 0;
        int fileCount = 0;
        long totalBytes = 0;
        
        for (FileItem item : fileItems) {
            if (item.getName().equals("..")) {
                continue; // Salta la cartella parent
            }
            
            if (item.isDirectory()) {
                folderCount++;
            } else {
                fileCount++;
                // Calcola la dimensione del file
                try {
                    Path itemPath = currentPath.resolve(item.getName());
                    File file = itemPath.toFile();
                    if (file.exists() && file.isFile()) {
                        totalBytes += file.length();
                    }
                } catch (Exception e) {
                    // Ignora errori nel calcolo della dimensione
                }
            }
        }
        
        // Converti bytes in MB
        double totalMB = totalBytes / (1024.0 * 1024.0);
        
        // Ottieni informazioni sul disco
        String diskInfo = getDiskInfo();
        
        // Aggiorna la label di stato
        statusLabel.setText(String.format("Cartelle: %d | File: %d | Totale: %.2f MB | %s", 
            folderCount, fileCount, totalMB, diskInfo));
    }
    
    private String getDiskInfo() {
        try {
            FileStore store = Files.getFileStore(currentPath);
            String volumeName = store.name();
            if (volumeName == null || volumeName.isEmpty()) {
                volumeName = "Volume";
            }
            
            long totalSpace = store.getTotalSpace();
            long usableSpace = store.getUsableSpace();
            long usedSpace = totalSpace - usableSpace;
            
            double totalGB = totalSpace / (1024.0 * 1024.0 * 1024.0);
            double freeGB = usableSpace / (1024.0 * 1024.0 * 1024.0);
            double usedGB = usedSpace / (1024.0 * 1024.0 * 1024.0);
            
            return String.format("%s | Disco: %.2f GB | Libero: %.2f GB | Usato: %.2f GB", 
                volumeName, totalGB, freeGB, usedGB);
        } catch (Exception e) {
            return "Informazioni disco non disponibili";
        }
    }

    private void navigateToPath(String pathString) {
        if (isFtpMode) {
            navigateToFtpPath(pathString);
        } else {
        try {
            Path path = Paths.get(pathString);
            loadDirectory(path);
        } catch (Exception e) {
            showError("Errore", "Percorso non valido: " + e.getMessage());
            }
        }
    }

    private void browseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setInitialDirectory(currentPath.toFile());
        File selected = chooser.showDialog(getScene().getWindow());
        if (selected != null) {
            loadDirectory(selected.toPath());
        }
    }

    public void openSelectedItem() {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null) return;
        
        if (isFtpMode) {
            openFtpItem(selected);
        } else {
            openLocalItem(selected);
        }
    }
    
    private void openLocalItem(FileItem selected) {
        if (selected.getName().equals("..")) {
            if (currentPath.getParent() != null) {
                loadDirectory(currentPath.getParent());
            }
        } else {
            Path itemPath = currentPath.resolve(selected.getName());
            File file = itemPath.toFile();
            
            if (file.isDirectory()) {
                loadDirectory(itemPath);
            } else {
                // Apri file con applicazione predefinita
                fileOperationService.openFile(file);
            }
        }
    }
    
    private void openFtpItem(FileItem selected) {
        if (selected.getName().equals("..")) {
            // Vai alla directory parent
            try {
                String parentPath = getFtpParentPath(currentFtpPath);
                loadFtpDirectory(parentPath);
            } catch (Exception e) {
                showError("Errore", "Impossibile tornare alla directory parent: " + e.getMessage());
            }
        } else {
            String itemPath = currentFtpPath.endsWith("/") ? 
                currentFtpPath + selected.getName() : 
                currentFtpPath + "/" + selected.getName();
            
            if (selected.isDirectory()) {
                loadFtpDirectory(itemPath);
            } else {
                // Per ora, mostra un messaggio - il download può essere implementato dopo
                showInfo("File FTP", "File selezionato: " + selected.getName() + "\nIl download può essere eseguito tramite il menu contestuale.");
            }
        }
    }
    
    private String getFtpParentPath(String currentPath) {
        if (currentPath == null || currentPath.equals("/") || currentPath.isEmpty()) {
            return "/";
        }
        
        // Rimuovi lo slash finale se presente
        String path = currentPath.endsWith("/") ? currentPath.substring(0, currentPath.length() - 1) : currentPath;
        
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        
        return path.substring(0, lastSlash);
    }

    public void createNewFolder() {
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Nuova Cartella");
        dialog.setHeaderText("Inserisci il nome della cartella:");
        dialog.setContentText("Nome:");
        
        dialog.showAndWait().ifPresent(name -> {
            if (!name.isEmpty()) {
                Path newFolderPath = currentPath.resolve(name);
                if (fileOperationService.createFolder(newFolderPath)) {
                    refresh();
                } else {
                    showError("Errore", "Impossibile creare la cartella.");
                }
            }
        });
    }

    public void deleteSelected() {
        List<FileItem> selected = fileTable.getSelectionModel().getSelectedItems();
        if (selected.isEmpty()) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Conferma Eliminazione");
        confirm.setHeaderText("Sei sicuro di voler eliminare gli elementi selezionati?");
        confirm.setContentText("Questa operazione non può essere annullata.");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                List<File> filesToDelete = new ArrayList<>();
                for (FileItem item : selected) {
                    if (!item.getName().equals("..")) {
                        filesToDelete.add(currentPath.resolve(item.getName()).toFile());
                    }
                }
                
                if (fileOperationService.deleteFiles(filesToDelete)) {
                    refresh();
                } else {
                    showError("Errore", "Impossibile eliminare alcuni file.");
                }
            }
        });
    }

    public void renameSelected() {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getName().equals("..")) return;
        
        TextInputDialog dialog = new TextInputDialog(selected.getName());
        dialog.setTitle("Rinomina");
        dialog.setHeaderText("Inserisci il nuovo nome:");
        dialog.setContentText("Nome:");
        
        dialog.showAndWait().ifPresent(newName -> {
            if (!newName.isEmpty() && !newName.equals(selected.getName())) {
                Path oldPath = currentPath.resolve(selected.getName());
                Path newPath = currentPath.resolve(newName);
                
                if (fileOperationService.renameFile(oldPath, newPath)) {
                    refresh();
                } else {
                    showError("Errore", "Impossibile rinominare il file.");
                }
            }
        });
    }

    public void editSelectedFile() {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isDirectory()) return;
        
        Path filePath = currentPath.resolve(selected.getName());
        fileOperationService.editFile(filePath.toFile());
    }

    public void extractArchive() {
        FileItem selected = fileTable.getSelectionModel().getSelectedItem();
        if (selected == null || selected.isDirectory()) return;
        
        Path archivePath = currentPath.resolve(selected.getName());
        DirectoryChooser chooser = new DirectoryChooser();
        chooser.setTitle("Seleziona cartella di destinazione");
        chooser.setInitialDirectory(currentPath.toFile());
        
        File destDir = chooser.showDialog(getScene().getWindow());
        if (destDir != null) {
            if (archiveService.extractArchive(archivePath.toFile(), destDir)) {
                showInfo("Successo", "Archivio estratto con successo.");
                refresh();
            } else {
                showError("Errore", "Impossibile estrarre l'archivio.");
            }
        }
    }

    public List<File> getSelectedFiles() {
        List<File> files = new ArrayList<>();
        
        // Ottieni gli indici selezionati invece degli items (più affidabile)
        ObservableList<Integer> selectedIndices = fileTable.getSelectionModel().getSelectedIndices();
        
        if (isFtpMode) {
            // Per i file FTP, crea file temporanei con i nomi per riferimento
            // Il trasferimento userà i percorsi FTP reali
            for (Integer index : selectedIndices) {
                if (index >= 0 && index < fileItems.size()) {
                    FileItem item = fileItems.get(index);
                    if (!item.getName().equals("..")) {
                        // Crea un file fittizio con il nome per riferimento
                        // Il percorso reale sarà costruito nel MainWindow
                        String ftpPath = currentFtpPath.endsWith("/") ? 
                            currentFtpPath + item.getName() : 
                            currentFtpPath + "/" + item.getName();
                        // Usa un file temporaneo come placeholder
                        File tempFile = new File(System.getProperty("java.io.tmpdir"), item.getName());
                        tempFile.deleteOnExit();
                        files.add(tempFile);
                    }
                }
            }
        } else {
            for (Integer index : selectedIndices) {
                if (index >= 0 && index < fileItems.size()) {
                    FileItem item = fileItems.get(index);
            if (!item.getName().equals("..")) {
                        File file = currentPath.resolve(item.getName()).toFile();
                        if (file.exists()) {
                            files.add(file);
                        }
                    }
                }
            }
        }
        return files;
    }
    
    /**
     * Ottieni i percorsi FTP dei file selezionati
     */
    public List<String> getSelectedFtpPaths() {
        List<String> paths = new ArrayList<>();
        if (isFtpMode) {
            // Usa gli indici invece degli items per maggiore affidabilità
            ObservableList<Integer> selectedIndices = fileTable.getSelectionModel().getSelectedIndices();
            for (Integer index : selectedIndices) {
                if (index >= 0 && index < fileItems.size()) {
                    FileItem item = fileItems.get(index);
                    if (!item.getName().equals("..")) {
                        String ftpPath = currentFtpPath.endsWith("/") ? 
                            currentFtpPath + item.getName() : 
                            currentFtpPath + "/" + item.getName();
                        paths.add(ftpPath);
                    }
                }
            }
        }
        return paths;
    }

    public Path getCurrentPath() {
        return currentPath;
    }

    public void refresh() {
        if (isRefreshing) {
            return; // Evita refresh multipli simultanei
        }
        
        isRefreshing = true;
        
        // Salva la selezione corrente prima del refresh
        List<String> selectedFileNames = new ArrayList<>();
        for (FileItem item : fileTable.getSelectionModel().getSelectedItems()) {
            if (!item.getName().equals("..")) {
                selectedFileNames.add(item.getName());
            }
        }
        
        // Esegui il refresh
        if (isFtpMode) {
            loadFtpDirectory(currentFtpPath);
        } else {
            loadLocalDirectory(currentPath);
            updateStatistics();
        }
        
        // Ripristina la selezione dopo il refresh
        if (!selectedFileNames.isEmpty()) {
            javafx.application.Platform.runLater(() -> {
                try {
                    // Aspetta che la tabella sia aggiornata
                    fileTable.getSelectionModel().clearSelection();
                    
                    // Ripristina la selezione usando i nomi
                    for (int i = 0; i < fileItems.size(); i++) {
                        FileItem item = fileItems.get(i);
                        if (selectedFileNames.contains(item.getName())) {
                            fileTable.getSelectionModel().select(i);
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Errore nel ripristino della selezione: " + e.getMessage());
                } finally {
                    isRefreshing = false;
                }
            });
        } else {
            isRefreshing = false;
        }
    }
    
    public void refreshWithoutPreservingSelection() {
        // Refresh senza preservare la selezione (utile per navigazione)
        if (isFtpMode) {
            loadFtpDirectory(currentFtpPath);
        } else {
            loadLocalDirectory(currentPath);
            updateStatistics();
        }
    }
    
    public void navigateToPath(Path path) {
        if (!isFtpMode) {
            loadDirectory(path);
        }
    }
    
    public void navigateToFtpPath(String ftpPath) {
        if (isFtpMode) {
            loadFtpDirectory(ftpPath);
        }
    }

    public void setActive(boolean active) {
        this.isActive = active;
        if (active) {
            this.setStyle("-fx-border-color: blue; -fx-border-width: 2px;");
            // Dai il focus alla tabella quando il pannello diventa attivo
            fileTable.requestFocus();
        } else {
            this.setStyle("-fx-border-color: gray; -fx-border-width: 1px;");
        }
    }

    private void copySelected() {
        // Implementato in MainWindow
    }

    private void moveSelected() {
        // Implementato in MainWindow
    }
    
    private void setupMultiSelectionDrag() {
        // Variabile per tracciare l'inizio del drag
        final int[] dragStartIndex = {-1};
        final boolean[] isDragging = {false};
        
        // Selezione multipla con drag del mouse destro
        fileTable.setOnMousePressed(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                // Ottieni l'indice della riga cliccata
                int index = fileTable.getSelectionModel().getFocusedIndex();
                if (index < 0) {
                    // Prova a ottenere l'indice dalla posizione del mouse
                    javafx.scene.input.MouseEvent me = (javafx.scene.input.MouseEvent) e;
                    double y = me.getY();
                    int rowIndex = (int) (y / fileTable.getFixedCellSize());
                    if (rowIndex >= 0 && rowIndex < fileItems.size()) {
                        index = rowIndex;
                    }
                }
                
                if (index >= 0) {
                    dragStartIndex[0] = index;
                    isDragging[0] = false; // Reset del flag di drag
                    // Se l'elemento non è già selezionato, selezionalo
                    if (!fileTable.getSelectionModel().getSelectedIndices().contains(index)) {
                        fileTable.getSelectionModel().clearSelection();
                        fileTable.getSelectionModel().select(index);
                    }
                    e.consume();
                }
            } else if (e.getButton() == MouseButton.PRIMARY) {
                // Ctrl+Click o Shift+Click per selezione multipla standard
                if (e.isControlDown()) {
                    int index = fileTable.getSelectionModel().getFocusedIndex();
                    if (index >= 0) {
                        if (fileTable.getSelectionModel().getSelectedIndices().contains(index)) {
                            fileTable.getSelectionModel().clearSelection(index);
                        } else {
                            fileTable.getSelectionModel().select(index);
                        }
                        e.consume();
                    }
                } else if (e.isShiftDown()) {
                    int currentIndex = fileTable.getSelectionModel().getSelectedIndex();
                    int clickedIndex = fileTable.getSelectionModel().getFocusedIndex();
                    if (currentIndex >= 0 && clickedIndex >= 0) {
                        int start = Math.min(currentIndex, clickedIndex);
                        int end = Math.max(currentIndex, clickedIndex);
                        fileTable.getSelectionModel().selectRange(start, end);
                        e.consume();
                    }
                }
            }
        });
        
        // Drag con mouse destro per selezionare più elementi
        fileTable.setOnMouseDragged(e -> {
            if (e.getButton() == MouseButton.SECONDARY || e.isSecondaryButtonDown()) {
                isDragging[0] = true;
                
                // Ottieni l'indice della riga sotto il mouse usando il metodo corretto
                javafx.scene.control.TableRow<FileItem> row = (javafx.scene.control.TableRow<FileItem>) 
                    fileTable.lookup(".table-row");
                int rowIndex = fileTable.getSelectionModel().getFocusedIndex();
                
                // Calcola l'indice dalla posizione Y
                double cellHeight = fileTable.getFixedCellSize() > 0 ? 
                    fileTable.getFixedCellSize() : 25.0; // Default se non impostato
                double y = e.getY();
                int calculatedIndex = (int) (y / cellHeight);
                
                // Usa il metodo più affidabile: ottieni l'indice dalla posizione
                javafx.scene.input.MouseEvent me = (javafx.scene.input.MouseEvent) e;
                double localY = me.getY();
                int indexFromY = (int) (localY / cellHeight);
                
                // Prova anche a ottenere l'indice dalla tabella direttamente
                if (indexFromY < 0) indexFromY = 0;
                if (indexFromY >= fileItems.size()) indexFromY = fileItems.size() - 1;
                
                // Usa l'indice calcolato o quello focalizzato
                int currentIndex = (indexFromY >= 0 && indexFromY < fileItems.size()) ? 
                    indexFromY : rowIndex;
                
                if (currentIndex >= 0 && dragStartIndex[0] >= 0) {
                    int start = Math.min(dragStartIndex[0], currentIndex);
                    int end = Math.max(dragStartIndex[0], currentIndex);
                    fileTable.getSelectionModel().clearSelection();
                    fileTable.getSelectionModel().selectRange(start, end + 1);
                }
                e.consume();
            }
        });
        
        fileTable.setOnMouseReleased(e -> {
            if (e.getButton() == MouseButton.SECONDARY) {
                if (!isDragging[0]) {
                    // Se non c'è stato drag, mostra il menu contestuale
                    // (gestito dal ContextMenu)
                }
                dragStartIndex[0] = -1;
                isDragging[0] = false;
            }
        });
        
        // Gestione del mouse move per aggiornare la selezione durante il drag
        fileTable.setOnMouseMoved(e -> {
            if (e.isSecondaryButtonDown() && dragStartIndex[0] >= 0) {
                double cellHeight = fileTable.getFixedCellSize() > 0 ? 
                    fileTable.getFixedCellSize() : 25.0;
                double y = e.getY();
                int rowIndex = (int) (y / cellHeight);
                if (rowIndex < 0) rowIndex = 0;
                if (rowIndex >= fileItems.size()) rowIndex = fileItems.size() - 1;
                
                if (rowIndex >= 0) {
                    int start = Math.min(dragStartIndex[0], rowIndex);
                    int end = Math.max(dragStartIndex[0], rowIndex);
                    fileTable.getSelectionModel().clearSelection();
                    fileTable.getSelectionModel().selectRange(start, end + 1);
                }
            }
        });
    }
    

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showInfo(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

