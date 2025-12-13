package com.totalcommander.ui;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Dialog per la ricerca di file
 */
public class FindFilesDialog extends Stage {
    
    private TextField findField;
    private TextField searchInField;
    private ComboBox<String> searchInCombo;
    private CheckBox regexCheckBox;
    private CheckBox everythingCheckBox;
    private CheckBox searchOnlySelectedCheckBox;
    private CheckBox searchArchivesCheckBox;
    private ComboBox<String> subfoldersCombo;
    
    private CheckBox findTextCheckBox;
    private TextField findTextField;
    private CheckBox wholeWordCheckBox;
    private CheckBox caseSensitiveCheckBox;
    private CheckBox regex2CheckBox;
    private CheckBox hexadecimalCheckBox;
    private CheckBox searchFilesNotContainingCheckBox;
    
    private CheckBox ansiCharsetCheckBox;
    private CheckBox dosCharsetCheckBox;
    private CheckBox unicodeUtf16CheckBox;
    private CheckBox utf8CheckBox;
    private CheckBox officeXmlCheckBox;
    
    private CheckBox pluginCheckBox;
    private TextField pluginField;
    
    private TableView<SearchResult> resultsTable;
    private Label statusLabel;
    private ProgressBar progressBar;
    private ExecutorService executorService;
    private boolean isSearching = false;
    
    public FindFilesDialog() {
        initStyle(StageStyle.UTILITY);
        setTitle("Ricerca file");
        setWidth(900);
        setHeight(700);
        setResizable(true);
        
        executorService = Executors.newSingleThreadExecutor();
        
        initializeUI();
        
        setOnCloseRequest(e -> {
            if (executorService != null) {
                executorService.shutdown();
            }
        });
    }
    
    private void initializeUI() {
        TabPane tabPane = new TabPane();
        
        // Tab Generale
        Tab generalTab = new Tab("Generale");
        generalTab.setClosable(false);
        generalTab.setContent(createGeneralTab());
        tabPane.getTabs().add(generalTab);
        
        // Tab Estesa (placeholder per ora)
        Tab extendedTab = new Tab("Estesa");
        extendedTab.setClosable(false);
        extendedTab.setContent(createExtendedTab());
        tabPane.getTabs().add(extendedTab);
        
        // Tab Plugin (placeholder)
        Tab pluginTab = new Tab("Plugin");
        pluginTab.setClosable(false);
        pluginTab.setContent(createPluginTab());
        tabPane.getTabs().add(pluginTab);
        
        // Tab Carica/salva (placeholder)
        Tab loadSaveTab = new Tab("Carica/salva");
        loadSaveTab.setClosable(false);
        loadSaveTab.setContent(createLoadSaveTab());
        tabPane.getTabs().add(loadSaveTab);
        
        VBox root = new VBox(5);
        root.setPadding(new Insets(8));
        root.getChildren().add(tabPane);
        
        // Tabella risultati (nascosta inizialmente)
        resultsTable = new TableView<>();
        resultsTable.setColumnResizePolicy(TableView.UNCONSTRAINED_RESIZE_POLICY);
        resultsTable.setVisible(false);
        resultsTable.setManaged(false);
        
        TableColumn<SearchResult, String> pathColumn = new TableColumn<>("Percorso");
        pathColumn.setCellValueFactory(param -> param.getValue().pathProperty());
        pathColumn.setPrefWidth(400);
        
        TableColumn<SearchResult, String> nameColumn = new TableColumn<>("Nome");
        nameColumn.setCellValueFactory(param -> param.getValue().nameProperty());
        nameColumn.setPrefWidth(200);
        
        TableColumn<SearchResult, String> sizeColumn = new TableColumn<>("Dimensione");
        sizeColumn.setCellValueFactory(param -> param.getValue().sizeProperty());
        sizeColumn.setPrefWidth(100);
        
        TableColumn<SearchResult, String> dateColumn = new TableColumn<>("Data");
        dateColumn.setCellValueFactory(param -> param.getValue().dateProperty());
        dateColumn.setPrefWidth(150);
        
        resultsTable.getColumns().addAll(pathColumn, nameColumn, sizeColumn, dateColumn);
        
        // Status e progress
        progressBar = new ProgressBar();
        progressBar.setProgress(-1);
        progressBar.setVisible(false);
        
        statusLabel = new Label("Pronto per la ricerca");
        
        // Bottoni
        Button startButton = new Button("Avvia ricerca");
        startButton.setOnAction(e -> startSearch());
        
        Button cancelButton = new Button("Annulla");
        cancelButton.setOnAction(e -> {
            isSearching = false;
            progressBar.setVisible(false);
            statusLabel.setText("Ricerca annullata");
        });
        
        Button helpButton = new Button("?");
        helpButton.setOnAction(e -> showHelp());
        
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(Pos.CENTER_RIGHT);
        buttonBox.getChildren().addAll(startButton, cancelButton, helpButton);
        
        root.getChildren().addAll(resultsTable, progressBar, statusLabel, buttonBox);
        VBox.setVgrow(resultsTable, Priority.ALWAYS);
        
        setScene(new javafx.scene.Scene(root));
    }
    
    private VBox createGeneralTab() {
        VBox content = new VBox(5);
        content.setPadding(new Insets(8));
        content.setSpacing(5);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(5);
        grid.setPadding(new Insets(5));
        
        // Trova:
        Label findLabel = new Label("Trova:");
        findField = new TextField();
        findField.setPrefWidth(300);
        HBox findBox = new HBox(5);
        findBox.getChildren().addAll(findField, new ComboBox<>());
        grid.add(findLabel, 0, 0);
        grid.add(findBox, 1, 0);
        
        // Cerca in:
        Label searchInLabel = new Label("Cerca in:");
        searchInField = new TextField();
        searchInField.setPrefWidth(300);
        searchInField.setText(System.getProperty("user.home"));
        HBox searchInBox = new HBox(5);
        searchInCombo = new ComboBox<>();
        Button browseButton = new Button(">>");
        browseButton.setOnAction(e -> browseDirectory());
        Button driveButton = new Button("Unità");
        driveButton.setOnAction(e -> selectDrive());
        searchInBox.getChildren().addAll(searchInField, searchInCombo, browseButton, driveButton);
        grid.add(searchInLabel, 0, 1);
        grid.add(searchInBox, 1, 1);
        
        // Opzioni sinistra
        VBox leftOptions = new VBox(3);
        regexCheckBox = new CheckBox("RegEx");
        everythingCheckBox = new CheckBox("'Everything'");
        searchOnlySelectedCheckBox = new CheckBox("Cerca solo nelle cartelle/file selezionate/i");
        searchArchivesCheckBox = new CheckBox("Ricerca archivi (tutti eccetto uc2)");
        
        Label subfoldersLabel = new Label("Ricerca nelle sottocartelle:");
        subfoldersCombo = new ComboBox<>();
        subfoldersCombo.getItems().addAll(
            "Tutte (profondità illimitata)",
            "Nessuna",
            "1 livello",
            "2 livelli",
            "3 livelli",
            "4 livelli",
            "5 livelli"
        );
        subfoldersCombo.setValue("Tutte (profondità illimitata)");
        
        leftOptions.getChildren().addAll(
            regexCheckBox, everythingCheckBox, searchOnlySelectedCheckBox,
            searchArchivesCheckBox, subfoldersLabel, subfoldersCombo
        );
        
        // Trova testo:
        VBox textSearchOptions = new VBox(3);
        textSearchOptions.setPadding(new Insets(5, 0, 0, 0));
        
        Label textSearchLabel = new Label("Ricerca nel contenuto:");
        textSearchLabel.setStyle("-fx-font-weight: bold;");
        
        HBox findTextBox = new HBox(5);
        findTextCheckBox = new CheckBox("Trova testo:");
        findTextField = new TextField();
        findTextField.setPrefWidth(250);
        findTextField.setDisable(true);
        findTextCheckBox.setOnAction(e -> findTextField.setDisable(!findTextCheckBox.isSelected()));
        findTextBox.getChildren().addAll(findTextCheckBox, findTextField);
        
        wholeWordCheckBox = new CheckBox("Parola intera");
        caseSensitiveCheckBox = new CheckBox("Controlla Maiuscole/minuscole");
        regex2CheckBox = new CheckBox("RegEx (2)");
        hexadecimalCheckBox = new CheckBox("Esadecimale");
        searchFilesNotContainingCheckBox = new CheckBox("Cerca file NON contenenti il testo");
        
        textSearchOptions.getChildren().addAll(
            textSearchLabel, findTextBox, wholeWordCheckBox, caseSensitiveCheckBox,
            regex2CheckBox, hexadecimalCheckBox, searchFilesNotContainingCheckBox
        );
        
        // Set caratteri
        VBox charsetOptions = new VBox(3);
        ansiCharsetCheckBox = new CheckBox("Set caratteri ANSI (Windows)");
        ansiCharsetCheckBox.setSelected(true);
        dosCharsetCheckBox = new CheckBox("Set caratteri DOS (ASCII)");
        unicodeUtf16CheckBox = new CheckBox("Unicode UTF16");
        utf8CheckBox = new CheckBox("UTF8");
        officeXmlCheckBox = new CheckBox("Office XML (docx, xlsx, odt");
        
        Button addCharsetButton = new Button("+");
        
        charsetOptions.getChildren().addAll(
            ansiCharsetCheckBox, dosCharsetCheckBox, unicodeUtf16CheckBox,
            utf8CheckBox, officeXmlCheckBox, addCharsetButton
        );
        
        // Plugin
        HBox pluginBox = new HBox(5);
        pluginCheckBox = new CheckBox("Plugin:");
        pluginField = new TextField();
        pluginField.setDisable(true);
        pluginCheckBox.setOnAction(e -> pluginField.setDisable(!pluginCheckBox.isSelected()));
        pluginBox.getChildren().addAll(pluginCheckBox, pluginField);
        
        // Layout a due colonne
        HBox optionsBox = new HBox(15);
        optionsBox.setPadding(new Insets(5, 0, 0, 0));
        
        VBox leftColumn = new VBox(5);
        leftColumn.setPadding(new Insets(0, 10, 0, 0));
        leftColumn.getChildren().addAll(leftOptions, textSearchOptions);
        
        VBox rightColumn = new VBox(5);
        rightColumn.setPadding(new Insets(0, 0, 0, 10));
        rightColumn.getChildren().addAll(charsetOptions, pluginBox);
        
        optionsBox.getChildren().addAll(leftColumn, rightColumn);
        
        // Aggiungi un separatore visivo
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 5, 0));
        
        content.getChildren().addAll(grid, separator, optionsBox);
        
        return content;
    }
    
    private VBox createExtendedTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(new Label("Opzioni estese - In sviluppo"));
        return content;
    }
    
    private VBox createPluginTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        content.getChildren().add(new Label("Plugin - In sviluppo"));
        return content;
    }
    
    private VBox createLoadSaveTab() {
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        Button saveButton = new Button("Salva ricerca");
        Button loadButton = new Button("Carica ricerca");
        content.getChildren().addAll(
            new Label("Salva/Carica criteri di ricerca"),
            saveButton, loadButton
        );
        return content;
    }
    
    private void browseDirectory() {
        DirectoryChooser chooser = new DirectoryChooser();
        File initialDir = new File(searchInField.getText());
        if (initialDir.exists()) {
            chooser.setInitialDirectory(initialDir);
        }
        File selected = chooser.showDialog(this);
        if (selected != null) {
            searchInField.setText(selected.getAbsolutePath());
        }
    }
    
    private void selectDrive() {
        File[] roots = File.listRoots();
        ChoiceDialog<String> dialog = new ChoiceDialog<>(roots[0].getAbsolutePath());
        dialog.setTitle("Seleziona Unità");
        dialog.setHeaderText("Scegli un'unità disco:");
        dialog.setContentText("Unità:");
        
        List<String> drives = new ArrayList<>();
        for (File root : roots) {
            drives.add(root.getAbsolutePath());
        }
        dialog.getItems().addAll(drives);
        
        dialog.showAndWait().ifPresent(selected -> {
            searchInField.setText(selected);
        });
    }
    
    private void startSearch() {
        if (isSearching) {
            return;
        }
        
        String searchPattern = findField.getText().trim();
        String searchPath = searchInField.getText().trim();
        String contentPattern = findTextField.getText().trim();
        
        if (searchPattern.isEmpty() && !findTextCheckBox.isSelected()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Pattern di ricerca vuoto");
            alert.setContentText("Inserisci un pattern per il nome del file o attiva la ricerca nel contenuto.");
            alert.showAndWait();
            return;
        }
        
        if (findTextCheckBox.isSelected() && contentPattern.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Testo di ricerca vuoto");
            alert.setContentText("Inserisci il testo da cercare nei file.");
            alert.showAndWait();
            return;
        }
        
        if (searchPath.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Percorso di ricerca vuoto");
            alert.setContentText("Seleziona una directory dove cercare.");
            alert.showAndWait();
            return;
        }
        
        File searchDir = new File(searchPath);
        if (!searchDir.exists() || !searchDir.isDirectory()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("Directory non valida");
            alert.setContentText("La directory specificata non esiste o non è accessibile.");
            alert.showAndWait();
            return;
        }
        
        isSearching = true;
        progressBar.setVisible(true);
        statusLabel.setText("Ricerca in corso...");
        resultsTable.getItems().clear();
        // Mostra la tabella quando si avvia la ricerca
        resultsTable.setVisible(true);
        resultsTable.setManaged(true);
        
        executorService.submit(() -> {
            try {
                int maxDepth = getMaxDepth();
                boolean useRegex = regexCheckBox.isSelected();
                boolean searchInContent = findTextCheckBox.isSelected();
                String contentPatternText = findTextField.getText().trim();
                boolean caseSensitive = caseSensitiveCheckBox.isSelected();
                
                searchFiles(searchDir.toPath(), searchPattern, useRegex, maxDepth, 
                           searchInContent, contentPatternText, caseSensitive, 0);
                
                javafx.application.Platform.runLater(() -> {
                    isSearching = false;
                    progressBar.setVisible(false);
                    statusLabel.setText("Ricerca completata. Trovati " + resultsTable.getItems().size() + " file.");
                });
                
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    isSearching = false;
                    progressBar.setVisible(false);
                    statusLabel.setText("Errore durante la ricerca: " + e.getMessage());
                    e.printStackTrace();
                });
            }
        });
    }
    
    private int getMaxDepth() {
        String selected = subfoldersCombo.getValue();
        if (selected == null || selected.equals("Tutte (profondità illimitata)")) {
            return Integer.MAX_VALUE;
        } else if (selected.equals("Nessuna")) {
            return 0;
        } else {
            try {
                return Integer.parseInt(selected.split(" ")[0]);
            } catch (Exception e) {
                return Integer.MAX_VALUE;
            }
        }
    }
    
    private void searchFiles(Path dir, String pattern, boolean useRegex, int maxDepth,
                            boolean searchInContent, String contentPattern, boolean caseSensitive, int currentDepth) {
        if (!isSearching || currentDepth > maxDepth) {
            return;
        }
        
        try {
            File dirFile = dir.toFile();
            if (!dirFile.exists() || !dirFile.isDirectory()) {
                return;
            }
            
            File[] files = dirFile.listFiles();
            if (files == null) {
                return;
            }
            
            Pattern namePattern = null;
            if (!pattern.isEmpty()) {
                try {
                    if (useRegex) {
                        namePattern = caseSensitive ? Pattern.compile(pattern) : 
                                     Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
                    } else {
                        // Converti wildcard in regex
                        // Escape dei caratteri speciali regex, poi sostituisci * e ?
                        StringBuilder regexBuilder = new StringBuilder();
                        for (char c : pattern.toCharArray()) {
                            if (c == '*') {
                                regexBuilder.append(".*");
                            } else if (c == '?') {
                                regexBuilder.append(".");
                            } else if (c == '.' || c == '^' || c == '$' || c == '+' || 
                                      c == '{' || c == '}' || c == '(' || c == ')' || 
                                      c == '|' || c == '[' || c == ']' || c == '\\') {
                                // Escape caratteri speciali regex
                                regexBuilder.append("\\").append(c);
                            } else {
                                regexBuilder.append(c);
                            }
                        }
                        String regexPattern = regexBuilder.toString();
                        namePattern = caseSensitive ? Pattern.compile(regexPattern) :
                                     Pattern.compile(regexPattern, Pattern.CASE_INSENSITIVE);
                    }
                } catch (PatternSyntaxException e) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Errore nel pattern RegEx: " + e.getMessage());
                    });
                    return;
                }
            }
            
            Pattern contentPatternObj = null;
            if (searchInContent && !contentPattern.isEmpty()) {
                try {
                    String patternToUse = contentPattern;
                    
                    // Gestione esadecimale
                    if (hexadecimalCheckBox.isSelected()) {
                        // Converti esadecimale in pattern
                        patternToUse = convertHexToPattern(contentPattern);
                    }
                    
                    // Gestione parola intera
                    if (wholeWordCheckBox.isSelected() && !regex2CheckBox.isSelected()) {
                        patternToUse = "\\b" + Pattern.quote(patternToUse) + "\\b";
                    } else if (wholeWordCheckBox.isSelected() && regex2CheckBox.isSelected()) {
                        patternToUse = "\\b" + patternToUse + "\\b";
                    } else if (!regex2CheckBox.isSelected()) {
                        patternToUse = Pattern.quote(patternToUse);
                    }
                    
                    if (regex2CheckBox.isSelected()) {
                        contentPatternObj = caseSensitive ? Pattern.compile(patternToUse) :
                                           Pattern.compile(patternToUse, Pattern.CASE_INSENSITIVE);
                    } else {
                        contentPatternObj = caseSensitive ? Pattern.compile(patternToUse) :
                                           Pattern.compile(patternToUse, Pattern.CASE_INSENSITIVE);
                    }
                } catch (PatternSyntaxException e) {
                    javafx.application.Platform.runLater(() -> {
                        statusLabel.setText("Errore nel pattern contenuto: " + e.getMessage());
                    });
                    return;
                }
            }
            
            for (File file : files) {
                if (!isSearching) {
                    break;
                }
                
                try {
                    if (file.isDirectory()) {
                        // Cerca ricorsivamente nelle sottocartelle
                        if (currentDepth < maxDepth) {
                            searchFiles(file.toPath(), pattern, useRegex, maxDepth,
                                      searchInContent, contentPattern, caseSensitive, currentDepth + 1);
                        }
                    } else {
                        // Verifica il nome del file
                        boolean nameMatches = true;
                        if (namePattern != null) {
                            nameMatches = namePattern.matcher(file.getName()).matches();
                        }
                        
                        // Se il nome non corrisponde e c'è un pattern per il nome, salta il file
                        if (!nameMatches && namePattern != null) {
                            continue;
                        }
                        
                        // Verifica il contenuto se richiesto
                        boolean contentMatches = true;
                        if (searchInContent && contentPatternObj != null) {
                            // Salta i file binari (immagini, video, eseguibili, ecc.)
                            if (isBinaryFile(file)) {
                                // Per file binari, considera solo il nome del file
                                contentMatches = true;
                            } else {
                                contentMatches = searchInFileContent(file, contentPatternObj, 
                                                                   searchFilesNotContainingCheckBox.isSelected(),
                                                                   caseSensitive);
                            }
                        }
                        
                        if (nameMatches && contentMatches) {
                            // File trovato!
                            SearchResult result = new SearchResult(
                                file.getParent(),
                                file.getName(),
                                formatBytes(file.length()),
                                formatDate(file.lastModified())
                            );
                            
                            javafx.application.Platform.runLater(() -> {
                                resultsTable.getItems().add(result);
                                statusLabel.setText("Trovati " + resultsTable.getItems().size() + " file...");
                            });
                        }
                    }
                } catch (Exception e) {
                    // Ignora errori di accesso
                }
            }
        } catch (Exception e) {
            // Ignora errori
        }
    }
    
    private boolean searchInFileContent(File file, Pattern pattern, boolean notContaining, boolean caseSensitive) {
        try {
            if (file.length() > 10 * 1024 * 1024) { // Skip file > 10MB
                return !notContaining;
            }
            
            if (file.length() == 0) {
                return !notContaining; // File vuoto
            }
            
            String fileName = file.getName().toLowerCase();
            String content = null;
            
            // Determina il charset da usare in base alle checkbox e al tipo di file
            String charset = determineCharset(fileName);
            
            byte[] bytes = Files.readAllBytes(file.toPath());
            
            // Prova diversi charset in ordine di priorità
            // Per file di testo comuni (.txt, .asp, .html, .js, .css, ecc.) usa UTF-8 o Windows-1252
            String[] charsetsToTry = {charset, "UTF-8", "Windows-1252", "ISO-8859-1", "US-ASCII"};
            
            for (String charsetToTry : charsetsToTry) {
                try {
                    content = new String(bytes, java.nio.charset.Charset.forName(charsetToTry));
                    // Se la decodifica funziona, cerca il pattern
                    if (content != null && content.length() > 0) {
                        boolean found = pattern.matcher(content).find();
                        // Se trovato (o non trovato se notContaining), ritorna subito
                        return notContaining ? !found : found;
                    }
                } catch (Exception e) {
                    // Prova il prossimo charset solo se questo fallisce
                    continue;
                }
            }
            
            // Se tutti i charset falliscono, prova UTF-8 come ultimo tentativo
            try {
                content = new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
                if (content != null && content.length() > 0) {
                    boolean found = pattern.matcher(content).find();
                    return notContaining ? !found : found;
                }
            } catch (Exception e) {
                // Ignora
            }
            
            // Se non riesce a decodificare, considera come non trovato
            return !notContaining;
            
        } catch (Exception e) {
            // Ignora errori e considera come non trovato
            return !notContaining;
        }
    }
    
    /**
     * Verifica se un file è binario (immagini, video, audio, eseguibili, ecc.)
     * e non dovrebbe essere cercato nel contenuto
     */
    private boolean isBinaryFile(File file) {
        String fileName = file.getName().toLowerCase();
        String extension = "";
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            extension = fileName.substring(lastDot + 1);
        }
        
        // Estensioni di file binari comuni
        String[] binaryExtensions = {
            // Immagini
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "ico", "svg", "webp",
            "psd", "raw", "cr2", "nef", "orf", "sr2", "dng", "heic", "heif",
            // Video
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg",
            "3gp", "3g2", "asf", "rm", "rmvb", "vob", "ogv", "divx", "xvid",
            // Audio
            "mp3", "wav", "flac", "ogg", "aac", "m4a", "wma", "opus", "ape", "ac3",
            "amr", "au", "ra", "voc", "mid", "midi", "mod", "s3m", "xm", "it",
            // Eseguibili e librerie
            "exe", "dll", "so", "dylib", "bin", "com", "bat", "cmd", "scr", "msi",
            // Archivi binari
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "lz", "lzma", "cab",
            "ace", "arj", "z", "lzh", "sit", "sitx", "deb", "rpm", "pkg", "dmg",
            // Documenti binari (vecchi formati Office)
            "doc", "xls", "ppt", "mdb", "accdb", "pub", "vsd", "vsdx",
            // PDF (binario anche se contiene testo)
            "pdf",
            // Database
            "db", "sqlite", "sqlite3", "mdb", "accdb", "fdb", "gdb",
            // Font
            "ttf", "otf", "woff", "woff2", "eot", "fon",
            // Altri binari
            "iso", "img", "bin", "dat", "pak", "vpk", "bsp", "bsp2"
        };
        
        for (String ext : binaryExtensions) {
            if (extension.equals(ext)) {
                return true;
            }
        }
        
        return false;
    }
    
    /**
     * Determina il charset da usare in base alle checkbox e al tipo di file
     */
    private String determineCharset(String fileName) {
        // Se più checkbox sono selezionate, usa la priorità
        // Office XML files
        if (officeXmlCheckBox.isSelected() && 
            (fileName.endsWith(".docx") || fileName.endsWith(".xlsx") || 
             fileName.endsWith(".pptx") || fileName.endsWith(".odt"))) {
            return "UTF-8"; // Office XML è UTF-8
        }
        
        // Unicode UTF16
        if (unicodeUtf16CheckBox.isSelected()) {
            return "UTF-16";
        }
        
        // UTF8
        if (utf8CheckBox.isSelected()) {
            return "UTF-8";
        }
        
        // DOS/ASCII
        if (dosCharsetCheckBox.isSelected()) {
            return "US-ASCII";
        }
        
        // ANSI (Windows) - default se selezionato
        if (ansiCharsetCheckBox.isSelected()) {
            return "Windows-1252"; // ANSI Windows
        }
        
        // Default: prova UTF-8 (più comune per file di testo moderni)
        // Se fallisce, il metodo searchInFileContent proverà altri charset
        return "UTF-8";
    }
    
    /**
     * Decodifica il contenuto del file con il charset appropriato
     */
    private String decodeContent(byte[] bytes, String charset, String fileName) {
        try {
            // Per Office XML, devo estrarre il testo dall'archivio ZIP
            if (officeXmlCheckBox.isSelected() && 
                (fileName.endsWith(".docx") || fileName.endsWith(".xlsx") || fileName.endsWith(".pptx"))) {
                return extractTextFromOfficeXml(bytes, fileName);
            }
            
            // Per altri formati, usa il charset specificato
            return new String(bytes, java.nio.charset.Charset.forName(charset));
        } catch (Exception e) {
            // Fallback a UTF-8
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        }
    }
    
    /**
     * Estrae il testo da file Office XML (docx, xlsx, pptx sono archivi ZIP)
     */
    private String extractTextFromOfficeXml(byte[] bytes, String fileName) {
        try {
            // Per semplicità, per ora leggiamo come testo normale
            // In futuro si potrebbe implementare l'estrazione vera dall'archivio ZIP
            return new String(bytes, java.nio.charset.StandardCharsets.UTF_8);
        } catch (Exception e) {
            return "";
        }
    }
    
    /**
     * Converte un pattern esadecimale in stringa di ricerca
     */
    private String convertHexToPattern(String hexPattern) {
        try {
            // Rimuovi spazi e separatori comuni
            String cleanHex = hexPattern.replaceAll("[\\s-]", "");
            
            // Verifica che sia esadecimale valido
            if (!cleanHex.matches("[0-9A-Fa-f]+")) {
                return hexPattern; // Se non è valido, ritorna originale
            }
            
            // Converti esadecimale in bytes, poi in stringa
            StringBuilder result = new StringBuilder();
            for (int i = 0; i < cleanHex.length(); i += 2) {
                if (i + 1 < cleanHex.length()) {
                    String hexByte = cleanHex.substring(i, i + 2);
                    int byteValue = Integer.parseInt(hexByte, 16);
                    result.append((char) byteValue);
                }
            }
            
            return Pattern.quote(result.toString());
        } catch (Exception e) {
            return hexPattern; // Se fallisce, ritorna originale
        }
    }
    
    private String formatBytes(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }
    
    private String formatDate(long millis) {
        return new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm").format(new java.util.Date(millis));
    }
    
    private void showHelp() {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aiuto - Ricerca File");
        alert.setHeaderText("Come usare la ricerca");
        alert.setContentText(
            "• Trova: Pattern per il nome del file (es: *.txt, test?.doc)\n" +
            "• RegEx: Usa espressioni regolari per il pattern\n" +
            "• Cerca in: Directory dove cercare\n" +
            "• Ricerca nelle sottocartelle: Profondità della ricerca\n" +
            "• Trova testo: Cerca il testo all'interno dei file\n" +
            "• Parola intera: Cerca solo parole complete\n" +
            "• Controlla Maiuscole/minuscole: Distingue maiuscole/minuscole"
        );
        alert.showAndWait();
    }
    
    /**
     * Classe per rappresentare un risultato di ricerca
     */
    private static class SearchResult {
        private final javafx.beans.property.SimpleStringProperty path;
        private final javafx.beans.property.SimpleStringProperty name;
        private final javafx.beans.property.SimpleStringProperty size;
        private final javafx.beans.property.SimpleStringProperty date;
        
        public SearchResult(String path, String name, String size, String date) {
            this.path = new javafx.beans.property.SimpleStringProperty(path);
            this.name = new javafx.beans.property.SimpleStringProperty(name);
            this.size = new javafx.beans.property.SimpleStringProperty(size);
            this.date = new javafx.beans.property.SimpleStringProperty(date);
        }
        
        public javafx.beans.property.StringProperty pathProperty() { return path; }
        public javafx.beans.property.StringProperty nameProperty() { return name; }
        public javafx.beans.property.StringProperty sizeProperty() { return size; }
        public javafx.beans.property.StringProperty dateProperty() { return date; }
    }
}

