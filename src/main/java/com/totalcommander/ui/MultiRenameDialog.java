package com.totalcommander.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.totalcommander.models.FileItem;
import com.totalcommander.services.MultiRenameService;
import java.io.File;
import java.nio.file.Path;
import java.util.List;
import java.util.ArrayList;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

/**
 * Dialog per rinomina multipla file con pattern e sostituzioni
 */
public class MultiRenameDialog extends Dialog<Void> {
    
    private TableView<RenameItem> renameTable;
    private ObservableList<RenameItem> renameItems;
    private TextField findField;
    private TextField replaceField;
    private TextField prefixField;
    private TextField suffixField;
    private TextField counterField;
    private TextField startNumberField;
    private CheckBox useRegexCheck;
    private Path currentPath;
    private List<File> selectedFiles;

    public MultiRenameDialog(List<File> selectedFiles, Path currentPath) {
        this.selectedFiles = selectedFiles;
        this.currentPath = currentPath;
        this.renameItems = FXCollections.observableArrayList();
        
        setTitle("Rinomina Multipla");
        setHeaderText("Rinomina multipla file selezionati");
        
        initializeUI();
        loadFiles();
    }

    private void initializeUI() {
        VBox mainBox = new VBox(10);
        mainBox.setPadding(new Insets(10));

        // Tabella con anteprima rinomina
        renameTable = new TableView<>();
        renameTable.setEditable(true);
        renameTable.setPrefHeight(300);

        TableColumn<RenameItem, String> originalColumn = new TableColumn<>("Nome Originale");
        originalColumn.setCellValueFactory(cell -> cell.getValue().originalNameProperty());
        originalColumn.setPrefWidth(250);

        TableColumn<RenameItem, String> newNameColumn = new TableColumn<>("Nuovo Nome");
        newNameColumn.setCellValueFactory(cell -> cell.getValue().newNameProperty());
        newNameColumn.setCellFactory(TextFieldTableCell.forTableColumn());
        newNameColumn.setOnEditCommit(e -> {
            e.getRowValue().setNewName(e.getNewValue());
        });
        newNameColumn.setPrefWidth(250);
        newNameColumn.setEditable(true);

        renameTable.getColumns().addAll(originalColumn, newNameColumn);
        renameTable.setItems(renameItems);

        // Pannello opzioni
        TitledPane optionsPane = new TitledPane("Opzioni di Rinomina", createOptionsPanel());
        optionsPane.setExpanded(true);

        mainBox.getChildren().addAll(renameTable, optionsPane);
        getDialogPane().setContent(mainBox);

        ButtonType renameButtonType = new ButtonType("Rinomina", ButtonBar.ButtonData.OK_DONE);
        getDialogPane().getButtonTypes().addAll(renameButtonType, ButtonType.CANCEL);

        setResultConverter(dialogButton -> {
            if (dialogButton == renameButtonType) {
                performRename();
            }
            return null;
        });
    }

    private VBox createOptionsPanel() {
        VBox optionsBox = new VBox(10);
        optionsBox.setPadding(new Insets(10));

        // Trova e Sostituisci
        HBox findReplaceBox = new HBox(5);
        findField = new TextField();
        findField.setPromptText("Trova");
        replaceField = new TextField();
        replaceField.setPromptText("Sostituisci con");
        useRegexCheck = new CheckBox("Usa Regex");
        Button applyFindReplace = new Button("Applica");
        applyFindReplace.setOnAction(e -> applyFindReplace());
        findReplaceBox.getChildren().addAll(
            new Label("Trova:"), findField,
            new Label("Sostituisci:"), replaceField,
            useRegexCheck, applyFindReplace
        );

        // Prefisso e Suffisso
        HBox prefixSuffixBox = new HBox(5);
        prefixField = new TextField();
        prefixField.setPromptText("Prefisso");
        suffixField = new TextField();
        suffixField.setPromptText("Suffisso");
        Button applyPrefixSuffix = new Button("Applica");
        applyPrefixSuffix.setOnAction(e -> applyPrefixSuffix());
        prefixSuffixBox.getChildren().addAll(
            new Label("Prefisso:"), prefixField,
            new Label("Suffisso:"), suffixField,
            applyPrefixSuffix
        );

        // Contatore
        HBox counterBox = new HBox(5);
        counterField = new TextField("###");
        counterField.setPromptText("Formato contatore (es. ###)");
        startNumberField = new TextField("1");
        startNumberField.setPromptText("Numero iniziale");
        Button applyCounter = new Button("Applica Contatore");
        applyCounter.setOnAction(e -> applyCounter());
        counterBox.getChildren().addAll(
            new Label("Formato:"), counterField,
            new Label("Inizia da:"), startNumberField,
            applyCounter
        );

        optionsBox.getChildren().addAll(findReplaceBox, prefixSuffixBox, counterBox);
        return optionsBox;
    }

    private void loadFiles() {
        renameItems.clear();
        for (File file : selectedFiles) {
            renameItems.add(new RenameItem(file.getName(), file.getName()));
        }
    }

    private void applyFindReplace() {
        String find = findField.getText();
        String replace = replaceField.getText();
        if (find.isEmpty()) return;

        try {
            Pattern pattern = useRegexCheck.isSelected() ? Pattern.compile(find) : null;
            
            for (RenameItem item : renameItems) {
                String newName = item.getNewName();
                if (useRegexCheck.isSelected() && pattern != null) {
                    newName = pattern.matcher(newName).replaceAll(replace);
                } else {
                    newName = newName.replace(find, replace);
                }
                item.setNewName(newName);
            }
        } catch (PatternSyntaxException e) {
            showError("Errore Regex", "Pattern regex non valido: " + e.getMessage());
        }
    }

    private void applyPrefixSuffix() {
        String prefix = prefixField.getText();
        String suffix = suffixField.getText();

        for (RenameItem item : renameItems) {
            String newName = item.getNewName();
            if (!prefix.isEmpty()) {
                newName = prefix + newName;
            }
            if (!suffix.isEmpty()) {
                int lastDot = newName.lastIndexOf('.');
                if (lastDot > 0) {
                    newName = newName.substring(0, lastDot) + suffix + newName.substring(lastDot);
                } else {
                    newName = newName + suffix;
                }
            }
            item.setNewName(newName);
        }
    }

    private void applyCounter() {
        String format = counterField.getText();
        int startNumber;
        try {
            startNumber = Integer.parseInt(startNumberField.getText());
        } catch (NumberFormatException e) {
            startNumber = 1;
        }

        int counter = startNumber;
        for (RenameItem item : renameItems) {
            String newName = item.getNewName();
            int lastDot = newName.lastIndexOf('.');
            String nameWithoutExt = lastDot > 0 ? newName.substring(0, lastDot) : newName;
            String ext = lastDot > 0 ? newName.substring(lastDot) : "";

            String counterStr = formatCounter(counter, format);
            newName = nameWithoutExt + counterStr + ext;
            item.setNewName(newName);
            counter++;
        }
    }

    private String formatCounter(int number, String format) {
        String numStr = String.valueOf(number);
        if (format.contains("#")) {
            int padding = format.length();
            return String.format("%0" + padding + "d", number);
        }
        return numStr;
    }

    private void performRename() {
        MultiRenameService service = new MultiRenameService();
        List<String> errors = new ArrayList<>();

        for (RenameItem item : renameItems) {
            if (!item.getOriginalName().equals(item.getNewName())) {
                Path oldPath = currentPath.resolve(item.getOriginalName());
                Path newPath = currentPath.resolve(item.getNewName());
                
                if (!service.renameFile(oldPath, newPath)) {
                    errors.add(item.getOriginalName());
                }
            }
        }

        if (!errors.isEmpty()) {
            showError("Errori", "Impossibile rinominare alcuni file:\n" + String.join("\n", errors));
        }
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Classe interna per gestire gli elementi della tabella
    private static class RenameItem {
        private javafx.beans.property.SimpleStringProperty originalName;
        private javafx.beans.property.SimpleStringProperty newName;

        public RenameItem(String originalName, String newName) {
            this.originalName = new javafx.beans.property.SimpleStringProperty(originalName);
            this.newName = new javafx.beans.property.SimpleStringProperty(newName);
        }

        public String getOriginalName() { return originalName.get(); }
        public String getNewName() { return newName.get(); }
        public void setNewName(String newName) { this.newName.set(newName); }
        
        public javafx.beans.property.StringProperty originalNameProperty() { return originalName; }
        public javafx.beans.property.StringProperty newNameProperty() { return newName; }
    }
}

