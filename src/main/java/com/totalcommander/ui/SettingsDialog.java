package com.totalcommander.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import com.totalcommander.services.LanguageService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Dialog per le impostazioni dell'applicazione
 */
public class SettingsDialog extends Stage {
    
    private ListView<String> categoriesList;
    private StackPane contentPane;
    private LanguageService languageService;
    private String selectedLanguageCode = "ITA";
    private TextField menuFilePathField;
    
    // Mappa nomi lingue -> codici
    private final Map<String, String> languageMap = new HashMap<>();
    
    // Lista delle categorie
    private final String[] categories = {
        "Lingua (Language)",
        "Disposizione (Layout)",
        "Visualizzazione (Display)",
        "Icone (Icons)",
        "Font",
        "Colori (Colors)",
        "Tabulazioni/formati dati (Tabs/data formats)",
        "Schede (Tabs)",
        "Colonne personalizzate (Custom columns)",
        "Modo visualizzazione (View mode)",
        "Modo auto switch (Auto switch mode)",
        "Operazioni (Operations)",
        "Visualizzatore/Editor (Viewer/Editor)",
        "Copia/cancellazione (Copy/delete)",
        "Aggiornamento (Update)",
        "Ricerca veloce (Quick search)",
        "FTP",
        "Plugin",
        "Anteprime (Thumbnails)",
        "Registro operazioni (Operation log)",
        "Ignora elenco (Ignore list)",
        "Programmi compressione (Compression programs)",
        "Compressione ZIP (ZIP compression)",
        "Varie (Miscellaneous)"
    };
    
    // Lista delle lingue disponibili con nomi display
    private final String[] languageNames = {
        "Italiano (APP_ITA.MNU)",
        "English (APP_ENG.MNU)"
    };
    
    public SettingsDialog() {
        initStyle(StageStyle.UTILITY);
        setTitle("Impostazioni");
        setWidth(700);
        setHeight(600);
        setResizable(true);
        
        languageService = LanguageService.getInstance();
        selectedLanguageCode = languageService.getCurrentLanguage();
        
        // Inizializza mappa lingue
        languageMap.put("Italiano (APP_ITA.MNU)", "ITA");
        languageMap.put("English (APP_ENG.MNU)", "ENG");
        
        // Carica lingue disponibili dal servizio
        List<String> availableCodes = languageService.getAvailableLanguages();
        if (!availableCodes.isEmpty()) {
            // Aggiorna la lista con le lingue trovate
            java.util.List<String> availableLanguages = new ArrayList<>();
            for (String code : availableCodes) {
                String displayName = getLanguageDisplayName(code);
                availableLanguages.add(displayName);
                languageMap.put(displayName, code);
            }
            // Aggiungi le lingue trovate alla lista
            if (!availableLanguages.isEmpty()) {
                // Usa le lingue trovate invece di quelle hardcoded
            }
        }
        
        initializeUI();
    }
    
    private String getLanguageDisplayName(String code) {
        switch (code.toUpperCase()) {
            case "ITA": return "Italiano (APP_ITA.MNU)";
            case "ENG": return "English (APP_ENG.MNU)";
            case "FRA": return "Français (APP_FRA.MNU)";
            case "ESP": return "Español (APP_ESP.MNU)";
            case "POR": return "Português (APP_POR.MNU)";
            case "DEU": return "Deutsch (APP_DEU.MNU)";
            default: return code + " (APP_" + code + ".MNU)";
        }
    }
    
    private void initializeUI() {
        BorderPane root = new BorderPane();
        root.setPadding(new Insets(10));
        
        // Pannello sinistro con lista categorie
        VBox leftPane = new VBox(5);
        leftPane.setPrefWidth(250);
        leftPane.setStyle("-fx-background-color: #f5f5f5; -fx-border-color: #d0d0d0; -fx-border-width: 0 1 0 0;");
        
        Label categoriesLabel = new Label("Categorie:");
        categoriesLabel.setStyle("-fx-font-weight: bold; -fx-padding: 5;");
        
        categoriesList = new ListView<>();
        categoriesList.getItems().addAll(categories);
        categoriesList.getSelectionModel().select(0); // Seleziona "Lingua" di default
        categoriesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                showCategoryContent(newVal);
            }
        });
        
        VBox.setVgrow(categoriesList, Priority.ALWAYS);
        leftPane.getChildren().addAll(categoriesLabel, categoriesList);
        
        // Pannello destro con contenuto
        contentPane = new StackPane();
        contentPane.setPadding(new Insets(10));
        
        // Mostra il contenuto iniziale
        showCategoryContent(categories[0]);
        
        // Layout principale
        HBox mainContent = new HBox(10);
        mainContent.getChildren().addAll(leftPane, contentPane);
        HBox.setHgrow(contentPane, Priority.ALWAYS);
        
        root.setCenter(mainContent);
        
        // Bottoni in basso
        HBox buttonBar = new HBox(10);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));
        buttonBar.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button okButton = new Button("OK");
        okButton.setPrefWidth(80);
        okButton.setOnAction(e -> {
            applySettings();
            close();
        });
        
        Button cancelButton = new Button("Annulla");
        cancelButton.setPrefWidth(80);
        cancelButton.setOnAction(e -> close());
        
        Button applyButton = new Button("Applica");
        applyButton.setPrefWidth(80);
        applyButton.setOnAction(e -> applySettings());
        
        Button helpButton = new Button("?");
        helpButton.setPrefWidth(40);
        helpButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Aiuto");
            alert.setHeaderText("Impostazioni");
            alert.setContentText("Seleziona una categoria dal menu a sinistra per modificare le relative impostazioni.");
            alert.showAndWait();
        });
        
        buttonBar.getChildren().addAll(okButton, cancelButton, applyButton, helpButton);
        root.setBottom(buttonBar);
        
        setScene(new javafx.scene.Scene(root));
    }
    
    private void showCategoryContent(String category) {
        contentPane.getChildren().clear();
        
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        if (category.equals("Lingua (Language)")) {
            content.getChildren().addAll(createLanguageContent());
        } else {
            // Placeholder per altre categorie
            Label placeholderLabel = new Label("Impostazioni per: " + category);
            placeholderLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
            Label infoLabel = new Label("Questa sezione sarà implementata in futuro.");
            content.getChildren().addAll(placeholderLabel, infoLabel);
        }
        
        scrollPane.setContent(content);
        contentPane.getChildren().add(scrollPane);
    }
    
    private VBox createLanguageContent() {
        VBox languageBox = new VBox(10);
        
        Label titleLabel = new Label("Lingua interfaccia utente");
        titleLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        // Lista delle lingue disponibili
        ListView<String> languagesList = new ListView<>();
        
        // Carica le lingue disponibili dal servizio
        List<String> availableCodes = languageService.getAvailableLanguages();
        java.util.List<String> availableLanguageNames = new ArrayList<>();
        
        if (availableCodes.isEmpty()) {
            // Se non ci sono lingue trovate, usa quelle di default
            availableLanguageNames.addAll(java.util.Arrays.asList(languageNames));
        } else {
            for (String code : availableCodes) {
                String displayName = getLanguageDisplayName(code);
                availableLanguageNames.add(displayName);
                if (!languageMap.containsKey(displayName)) {
                    languageMap.put(displayName, code);
                }
            }
        }
        
        languagesList.getItems().addAll(availableLanguageNames);
        languagesList.setPrefHeight(300);
        
        // Seleziona la lingua corrente
        String currentLanguageDisplay = getLanguageDisplayName(selectedLanguageCode);
        if (languagesList.getItems().contains(currentLanguageDisplay)) {
            languagesList.getSelectionModel().select(currentLanguageDisplay);
        } else if (!languagesList.getItems().isEmpty()) {
            languagesList.getSelectionModel().select(0);
        }
        
        languagesList.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String code = languageMap.get(newVal);
                if (code != null) {
                    selectedLanguageCode = code;
                    updateMenuFilePath();
                }
            }
        });
        
        // Campo percorso file menu
        HBox menuFileBox = new HBox(5);
        Label menuFileLabel = new Label("Nome file menu:");
        menuFilePathField = new TextField();
        menuFilePathField.setPrefWidth(400);
        menuFilePathField.setEditable(true);
        updateMenuFilePath(); // Inizializza il percorso
        
        Button browseButton = new Button(">>");
        browseButton.setPrefWidth(40);
        browseButton.setTooltip(new Tooltip("Sfoglia"));
        browseButton.setOnAction(e -> {
            javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
            fileChooser.setTitle("Seleziona file menu");
            fileChooser.getExtensionFilters().add(
                new javafx.stage.FileChooser.ExtensionFilter("File menu", "*.mnu", "*.MNU")
            );
            java.io.File selectedFile = fileChooser.showOpenDialog(this);
            if (selectedFile != null) {
                menuFilePathField.setText(selectedFile.getAbsolutePath());
            }
        });
        
        Button editButton = new Button("Modifica");
        editButton.setPrefWidth(80);
        editButton.setOnAction(e -> {
            // Apri il file menu nell'editor
            String filePath = menuFilePathField.getText();
            if (filePath != null && !filePath.isEmpty()) {
                try {
                    java.io.File file = new java.io.File(filePath);
                    if (file.exists()) {
                        // Apri con l'editor di sistema
                        java.awt.Desktop.getDesktop().edit(file);
                    } else {
                        Alert alert = new Alert(Alert.AlertType.WARNING);
                        alert.setTitle("File non trovato");
                        alert.setHeaderText(null);
                        alert.setContentText("Il file specificato non esiste.");
                        alert.showAndWait();
                    }
                } catch (Exception ex) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Errore");
                    alert.setHeaderText(null);
                    alert.setContentText("Impossibile aprire il file: " + ex.getMessage());
                    alert.showAndWait();
                }
            }
        });
        
        menuFileBox.getChildren().addAll(menuFileLabel, menuFilePathField, browseButton, editButton);
        
        // Bottone Download
        Button downloadButton = new Button("Download");
        downloadButton.setPrefWidth(120);
        downloadButton.setOnAction(e -> {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Download lingua");
            alert.setHeaderText("Download file lingua");
            alert.setContentText("Il download dei file lingua sarà disponibile in futuro.");
            alert.showAndWait();
        });
        
        languageBox.getChildren().addAll(titleLabel, languagesList, menuFileBox, downloadButton);
        
        return languageBox;
    }
    
    private void updateMenuFilePath() {
        // Usa il percorso dal LanguageService
        String filePath = languageService.getMenuFilePath();
        if (menuFilePathField != null) {
            menuFilePathField.setText(filePath);
        }
    }
    
    private void applySettings() {
        // Applica le impostazioni selezionate
        // Carica la lingua selezionata
        if (languageService.loadLanguage(selectedLanguageCode)) {
            // Salva la lingua nelle impostazioni
            com.totalcommander.services.SettingsService settingsService = 
                com.totalcommander.services.SettingsService.getInstance();
            settingsService.setLanguage(selectedLanguageCode);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Impostazioni applicate");
            alert.setHeaderText(null);
            alert.setContentText("Le impostazioni sono state applicate con successo.\n" +
                "La lingua è stata cambiata in: " + getLanguageDisplayName(selectedLanguageCode));
            alert.showAndWait();
        } else {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText(null);
            alert.setContentText("Impossibile caricare la lingua selezionata.");
            alert.showAndWait();
        }
    }
}

