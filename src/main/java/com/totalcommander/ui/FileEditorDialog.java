package com.totalcommander.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.charset.StandardCharsets;
import java.nio.charset.Charset;

/**
 * Dialog per modificare file di testo
 */
public class FileEditorDialog extends Stage {
    
    private TextArea textArea;
    private File file;
    private boolean hasChanges = false;
    
    public FileEditorDialog(File file) {
        this.file = file;
        initStyle(StageStyle.UTILITY);
        setTitle("Modifica: " + file.getName());
        setWidth(900);
        setHeight(700);
        setResizable(true);
        
        initializeUI();
        loadFile();
        
        // Avvisa se si chiude con modifiche non salvate
        setOnCloseRequest(e -> {
            if (hasChanges) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Conferma chiusura");
                alert.setHeaderText("Ci sono modifiche non salvate.");
                alert.setContentText("Vuoi salvare le modifiche prima di chiudere?");
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        saveFile();
                    }
                });
            }
        });
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // TextArea per l'editing
        textArea = new TextArea();
        textArea.setFont(javafx.scene.text.Font.font("Consolas", 12));
        textArea.setWrapText(false);
        textArea.textProperty().addListener((obs, oldVal, newVal) -> {
            hasChanges = true;
            setTitle("Modifica: " + file.getName() + " *");
        });
        
        ScrollPane scrollPane = new ScrollPane(textArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setFitToHeight(true);
        
        // Bottoni
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        
        Button saveButton = new Button("Salva");
        saveButton.setOnAction(e -> {
            if (saveFile()) {
                hasChanges = false;
                setTitle("Modifica: " + file.getName());
            }
        });
        
        Button cancelButton = new Button("Annulla");
        cancelButton.setOnAction(e -> {
            if (hasChanges) {
                Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                alert.setTitle("Conferma annullamento");
                alert.setHeaderText("Ci sono modifiche non salvate.");
                alert.setContentText("Vuoi salvare le modifiche?");
                
                alert.showAndWait().ifPresent(response -> {
                    if (response == ButtonType.OK) {
                        saveFile();
                    }
                    close();
                });
            } else {
                close();
            }
        });
        
        buttonBox.getChildren().addAll(saveButton, cancelButton);
        
        root.getChildren().addAll(scrollPane, buttonBox);
        VBox.setVgrow(scrollPane, Priority.ALWAYS);
        
        setScene(new javafx.scene.Scene(root));
    }
    
    private void loadFile() {
        try {
            if (!file.exists() || file.isDirectory()) {
                textArea.setText("File non valido o directory.");
                textArea.setEditable(false);
                return;
            }
            
            // Verifica se è un file binario
            if (isBinaryFile(file)) {
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("File binario");
                alert.setHeaderText("Impossibile modificare file binari");
                alert.setContentText("Il file selezionato è un file binario (immagine, eseguibile, ecc.) e non può essere modificato come testo.");
                alert.showAndWait();
                close();
                return;
            }
            
            long fileSize = file.length();
            if (fileSize > 10 * 1024 * 1024) { // 10MB
                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("File troppo grande");
                alert.setHeaderText("File troppo grande per l'editing");
                alert.setContentText("Il file è troppo grande (> 10MB) per essere modificato.");
                alert.showAndWait();
                close();
                return;
            }
            
            byte[] bytes = Files.readAllBytes(file.toPath());
            
            // Prova diversi charset per leggere il file
            String[] charsets = {"UTF-8", "Windows-1252", "ISO-8859-1", "US-ASCII"};
            boolean loaded = false;
            
            for (String charsetName : charsets) {
                try {
                    Charset charset = Charset.forName(charsetName);
                    String content = new String(bytes, charset);
                    // Verifica se contiene caratteri non stampabili (eccetto spazi, tab, newline)
                    boolean hasInvalidChars = false;
                    for (char c : content.toCharArray()) {
                        if (c < 32 && c != 9 && c != 10 && c != 13) {
                            hasInvalidChars = true;
                            break;
                        }
                    }
                    if (!hasInvalidChars || charsetName.equals("UTF-8")) {
                        textArea.setText(content);
                        loaded = true;
                        break;
                    }
                } catch (Exception e) {
                    // Prova il prossimo charset
                }
            }
            
            if (!loaded) {
                // Se tutti i charset falliscono, usa UTF-8 comunque
                textArea.setText(new String(bytes, StandardCharsets.UTF_8));
            }
            
            hasChanges = false;
            setTitle("Modifica: " + file.getName());
            
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("Errore durante la lettura del file");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            close();
        }
    }
    
    private boolean saveFile() {
        try {
            String content = textArea.getText();
            Files.write(file.toPath(), content.getBytes(StandardCharsets.UTF_8));
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Salvataggio completato");
            alert.setHeaderText(null);
            alert.setContentText("File salvato con successo.");
            alert.showAndWait();
            
            return true;
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Errore");
            alert.setHeaderText("Errore durante il salvataggio");
            alert.setContentText(e.getMessage());
            alert.showAndWait();
            return false;
        }
    }
    
    private boolean isBinaryFile(File file) {
        String name = file.getName().toLowerCase();
        String[] binaryExtensions = {
            // Immagini
            "jpg", "jpeg", "png", "gif", "bmp", "tiff", "tif", "ico", "svg", "webp",
            "psd", "raw", "cr2", "nef", "orf", "sr2", "dng", "heic", "heif",
            // Video
            "mp4", "avi", "mkv", "mov", "wmv", "flv", "webm", "m4v", "mpg", "mpeg",
            // Audio
            "mp3", "wav", "flac", "ogg", "aac", "m4a", "wma", "opus",
            // Eseguibili
            "exe", "dll", "so", "dylib", "bin", "com", "bat", "cmd", "scr", "msi",
            // Archivi
            "zip", "rar", "7z", "tar", "gz", "bz2", "xz", "cab",
            // Documenti binari
            "doc", "xls", "ppt", "mdb", "accdb", "pdf",
            // Database
            "db", "sqlite", "sqlite3", "mdb", "accdb",
            // Font
            "ttf", "otf", "woff", "woff2", "eot", "fon"
        };
        
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            String ext = name.substring(lastDot + 1);
            for (String binaryExt : binaryExtensions) {
                if (ext.equals(binaryExt)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}



