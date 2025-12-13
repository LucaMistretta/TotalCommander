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
 * Dialog per visualizzare il contenuto dei file
 */
public class FileViewerDialog extends Stage {
    
    private TextArea textArea;
    private TextArea hexArea;
    private TabPane tabPane;
    private File file;
    
    public FileViewerDialog(File file) {
        this.file = file;
        initStyle(StageStyle.UTILITY);
        setTitle("Visualizza: " + file.getName());
        setWidth(900);
        setHeight(700);
        setResizable(true);
        
        initializeUI();
        loadFile();
    }
    
    private void initializeUI() {
        VBox root = new VBox(10);
        root.setPadding(new Insets(10));
        
        // TabPane per testo e esadecimale
        tabPane = new TabPane();
        
        // Tab Testo
        Tab textTab = new Tab("Testo");
        textTab.setClosable(false);
        textArea = new TextArea();
        textArea.setEditable(false);
        textArea.setFont(javafx.scene.text.Font.font("Consolas", 12));
        textArea.setWrapText(true);
        ScrollPane textScroll = new ScrollPane(textArea);
        textScroll.setFitToWidth(true);
        textScroll.setFitToHeight(true);
        textTab.setContent(textScroll);
        
        // Tab Esadecimale
        Tab hexTab = new Tab("Esadecimale");
        hexTab.setClosable(false);
        hexArea = new TextArea();
        hexArea.setEditable(false);
        hexArea.setFont(javafx.scene.text.Font.font("Consolas", 10));
        hexArea.setWrapText(false);
        ScrollPane hexScroll = new ScrollPane(hexArea);
        hexScroll.setFitToWidth(true);
        hexScroll.setFitToHeight(true);
        hexTab.setContent(hexScroll);
        
        tabPane.getTabs().addAll(textTab, hexTab);
        
        // Bottoni
        HBox buttonBox = new HBox(10);
        buttonBox.setAlignment(javafx.geometry.Pos.CENTER_RIGHT);
        Button closeButton = new Button("Chiudi");
        closeButton.setOnAction(e -> close());
        buttonBox.getChildren().add(closeButton);
        
        root.getChildren().addAll(tabPane, buttonBox);
        VBox.setVgrow(tabPane, Priority.ALWAYS);
        
        setScene(new javafx.scene.Scene(root));
    }
    
    private void loadFile() {
        try {
            if (!file.exists() || file.isDirectory()) {
                textArea.setText("File non valido o directory.");
                return;
            }
            
            long fileSize = file.length();
            if (fileSize > 50 * 1024 * 1024) { // 50MB
                textArea.setText("File troppo grande per la visualizzazione (> 50MB).");
                hexArea.setText("File troppo grande per la visualizzazione (> 50MB).");
                return;
            }
            
            byte[] bytes = Files.readAllBytes(file.toPath());
            
            // Carica vista esadecimale
            loadHexView(bytes);
            
            // Prova a caricare come testo
            if (isTextFile(file)) {
                loadTextView(bytes);
                tabPane.getSelectionModel().select(0); // Seleziona tab testo
            } else {
                textArea.setText("File binario - usa la vista esadecimale.");
                tabPane.getSelectionModel().select(1); // Seleziona tab esadecimale
            }
            
        } catch (IOException e) {
            textArea.setText("Errore durante la lettura del file: " + e.getMessage());
            hexArea.setText("Errore durante la lettura del file: " + e.getMessage());
        }
    }
    
    private void loadTextView(byte[] bytes) {
        // Prova diversi charset
        String[] charsets = {"UTF-8", "Windows-1252", "ISO-8859-1", "US-ASCII"};
        
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
                    return;
                }
            } catch (Exception e) {
                // Prova il prossimo charset
            }
        }
        
        // Se tutti i charset falliscono, usa UTF-8 comunque
        textArea.setText(new String(bytes, StandardCharsets.UTF_8));
    }
    
    private void loadHexView(byte[] bytes) {
        StringBuilder hex = new StringBuilder();
        int bytesPerLine = 16;
        
        for (int i = 0; i < bytes.length; i += bytesPerLine) {
            // Offset
            hex.append(String.format("%08X: ", i));
            
            // Bytes esadecimali
            for (int j = 0; j < bytesPerLine; j++) {
                if (i + j < bytes.length) {
                    hex.append(String.format("%02X ", bytes[i + j]));
                } else {
                    hex.append("   ");
                }
                if (j == 7) {
                    hex.append(" ");
                }
            }
            
            hex.append(" ");
            
            // Caratteri ASCII
            for (int j = 0; j < bytesPerLine && i + j < bytes.length; j++) {
                byte b = bytes[i + j];
                char c = (b >= 32 && b < 127) ? (char) b : '.';
                hex.append(c);
            }
            
            hex.append("\n");
        }
        
        hexArea.setText(hex.toString());
    }
    
    private boolean isTextFile(File file) {
        String name = file.getName().toLowerCase();
        String[] textExtensions = {
            "txt", "log", "ini", "cfg", "conf", "xml", "html", "htm", "css", "js",
            "json", "java", "c", "cpp", "h", "hpp", "py", "rb", "php", "asp", "aspx",
            "sql", "sh", "bat", "cmd", "ps1", "md", "yml", "yaml", "properties",
            "csv", "tsv", "rtf", "tex", "latex", "bib", "clj", "cljs", "edn",
            "go", "rs", "swift", "kt", "scala", "r", "m", "pl", "pm", "lua",
            "vim", "vimrc", "gitignore", "gitconfig", "dockerfile", "makefile"
        };
        
        int lastDot = name.lastIndexOf('.');
        if (lastDot > 0 && lastDot < name.length() - 1) {
            String ext = name.substring(lastDot + 1);
            for (String textExt : textExtensions) {
                if (ext.equals(textExt)) {
                    return true;
                }
            }
        }
        
        return false;
    }
}

