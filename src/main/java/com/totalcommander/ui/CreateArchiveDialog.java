package com.totalcommander.ui;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import com.totalcommander.services.ArchiveService;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

/**
 * Dialog per creare un archivio
 */
public class CreateArchiveDialog extends Dialog<File> {
    
    private TextField archiveNameField;
    private ComboBox<String> archiveTypeCombo;
    private List<File> filesToArchive;
    private Path currentPath;
    private ArchiveService archiveService;

    public CreateArchiveDialog(List<File> filesToArchive, Path currentPath) {
        this.filesToArchive = filesToArchive;
        this.currentPath = currentPath;
        this.archiveService = new ArchiveService();
        
        setTitle("Crea Archivio");
        setHeaderText("Crea un nuovo archivio dai file selezionati");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));

        archiveNameField = new TextField();
        archiveNameField.setPromptText("nome_archivio.zip");
        
        archiveTypeCombo = new ComboBox<>();
        archiveTypeCombo.getItems().addAll("ZIP", "TAR", "TAR.GZ");
        archiveTypeCombo.setValue("ZIP");
        archiveTypeCombo.setOnAction(e -> updateArchiveExtension());

        Button browseButton = new Button("Sfoglia");
        browseButton.setOnAction(e -> browseArchiveLocation());

        grid.add(new Label("Nome Archivio:"), 0, 0);
        grid.add(archiveNameField, 1, 0);
        grid.add(new Label("Tipo:"), 0, 1);
        grid.add(archiveTypeCombo, 1, 1);
        grid.add(browseButton, 2, 0);

        Label infoLabel = new Label("File da archiviare: " + filesToArchive.size());
        grid.add(infoLabel, 0, 2, 3, 1);

        getDialogPane().setContent(grid);

        ButtonType createButtonType = new ButtonType("Crea", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(createButtonType, ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            if (dialogButton == createButtonType) {
                return createArchive();
            }
            return null;
        });
    }

    private void updateArchiveExtension() {
        String currentName = archiveNameField.getText();
        String type = archiveTypeCombo.getValue();
        String extension = "";
        
        switch (type) {
            case "ZIP":
                extension = ".zip";
                break;
            case "TAR":
                extension = ".tar";
                break;
            case "TAR.GZ":
                extension = ".tar.gz";
                break;
        }

        if (!currentName.isEmpty()) {
            // Rimuovi estensioni esistenti
            if (currentName.endsWith(".zip") || currentName.endsWith(".tar") || currentName.endsWith(".tar.gz")) {
                int lastDot = currentName.lastIndexOf('.');
                if (lastDot > 0) {
                    currentName = currentName.substring(0, lastDot);
                }
            }
            archiveNameField.setText(currentName + extension);
        } else {
            archiveNameField.setText("archivio" + extension);
        }
    }

    private void browseArchiveLocation() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Salva Archivio Come");
        chooser.setInitialDirectory(currentPath.toFile());
        
        String type = archiveTypeCombo.getValue();
        FileChooser.ExtensionFilter filter;
        switch (type) {
            case "ZIP":
                filter = new FileChooser.ExtensionFilter("File ZIP", "*.zip");
                break;
            case "TAR":
                filter = new FileChooser.ExtensionFilter("File TAR", "*.tar");
                break;
            case "TAR.GZ":
                filter = new FileChooser.ExtensionFilter("File TAR.GZ", "*.tar.gz");
                break;
            default:
                filter = new FileChooser.ExtensionFilter("Tutti gli archivi", "*.zip", "*.tar", "*.tar.gz");
        }
        chooser.getExtensionFilters().add(filter);
        chooser.setSelectedExtensionFilter(filter);

        File selected = chooser.showSaveDialog(getDialogPane().getScene().getWindow());
        if (selected != null) {
            archiveNameField.setText(selected.getName());
            currentPath = selected.getParentFile().toPath();
        }
    }

    private File createArchive() {
        String archiveName = archiveNameField.getText();
        if (archiveName.isEmpty()) {
            showError("Errore", "Inserisci un nome per l'archivio.");
            return null;
        }

        File archiveFile = currentPath.resolve(archiveName).toFile();
        
        String type = archiveTypeCombo.getValue();
        boolean success = false;

        if ("ZIP".equals(type)) {
            success = archiveService.createZipArchive(filesToArchive, archiveFile);
        } else {
            // Per TAR e TAR.GZ serve implementazione aggiuntiva
            showError("Info", "Il formato " + type + " sarà disponibile a breve. Usa ZIP per ora.");
            return null;
        }

        if (success) {
            showInfo("Successo", "Archivio creato con successo: " + archiveName);
            return archiveFile;
        } else {
            showError("Errore", "Impossibile creare l'archivio.");
            return null;
        }
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

