package com.totalcommander.ui;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import com.totalcommander.models.FtpConnection;
import com.totalcommander.services.FtpConnectionManager;
import com.totalcommander.services.FtpService;
import com.totalcommander.ui.panels.FilePanel;
import java.util.List;
import java.util.function.Consumer;

/**
 * Dialog per gestire le connessioni FTP salvate
 */
public class FtpManagerDialog extends Dialog<Void> {
    
    private TableView<FtpConnection> connectionsTable;
    private ObservableList<FtpConnection> connections;
    private Consumer<FtpConnection> onConnectCallback;

    public FtpManagerDialog() {
        this(null);
    }
    
    public FtpManagerDialog(Consumer<FtpConnection> onConnectCallback) {
        this.onConnectCallback = onConnectCallback;
        setTitle("Gestisci Connessioni FTP");
        setHeaderText("Connessioni FTP salvate");

        VBox vbox = new VBox(10);
        vbox.setPadding(new Insets(10));

        // Tabella connessioni
        connectionsTable = new TableView<>();
        connections = FXCollections.observableArrayList();
        connections.addAll(FtpConnectionManager.loadConnections());
        connectionsTable.setItems(connections);

        TableColumn<FtpConnection, String> nameColumn = new TableColumn<>("Nome");
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("name"));
        nameColumn.setPrefWidth(150);

        TableColumn<FtpConnection, String> hostColumn = new TableColumn<>("Host");
        hostColumn.setCellValueFactory(new PropertyValueFactory<>("host"));
        hostColumn.setPrefWidth(200);

        TableColumn<FtpConnection, Integer> portColumn = new TableColumn<>("Porta");
        portColumn.setCellValueFactory(new PropertyValueFactory<>("port"));
        portColumn.setPrefWidth(80);

        TableColumn<FtpConnection, String> userColumn = new TableColumn<>("Username");
        userColumn.setCellValueFactory(new PropertyValueFactory<>("username"));
        userColumn.setPrefWidth(150);

        connectionsTable.getColumns().addAll(nameColumn, hostColumn, portColumn, userColumn);
        connectionsTable.setPrefHeight(300);

        // Pulsanti
        HBox buttonBox = new HBox(10);
        Button connectButton = new Button("Connetti");
        connectButton.setOnAction(e -> connectSelected());
        
        Button newButton = new Button("Nuova");
        newButton.setOnAction(e -> createNewConnection());
        
        Button editButton = new Button("Modifica");
        editButton.setOnAction(e -> editSelected());
        
        Button deleteButton = new Button("Elimina");
        deleteButton.setOnAction(e -> deleteSelected());
        
        buttonBox.getChildren().addAll(connectButton, newButton, editButton, deleteButton);

        vbox.getChildren().addAll(connectionsTable, buttonBox);
        getDialogPane().setContent(vbox);

        getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private void connectSelected() {
        FtpConnection selected = connectionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // Connetti usando il manager
            com.totalcommander.services.FtpService ftpService = 
                com.totalcommander.services.FtpConnectionManager.connect(selected);
            
            if (ftpService != null && ftpService.isConnected()) {
                // Chiama il callback se presente (per aprire nel pannello attivo)
                if (onConnectCallback != null) {
                    onConnectCallback.accept(selected);
                } else {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Connessione Riuscita");
                    alert.setHeaderText("Connesso a: " + selected.getName());
                    alert.setContentText("La connessione FTP è attiva.\nHost: " + selected.getHost() + 
                                       "\nPorta: " + selected.getPort());
                    alert.showAndWait();
                }
                close(); // Chiudi il dialog dopo la connessione
            } else {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Errore di Connessione");
                alert.setHeaderText("Impossibile connettersi a: " + selected.getName());
                alert.setContentText("Verifica i dati di connessione e riprova.");
                alert.showAndWait();
            }
        } else {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Attenzione");
            alert.setHeaderText("Nessuna connessione selezionata");
            alert.setContentText("Seleziona una connessione dalla tabella.");
            alert.showAndWait();
        }
    }

    private void editSelected() {
        FtpConnection selected = connectionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            // TODO: Implementare modifica
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Modifica");
            alert.setContentText("La modifica sarà disponibile a breve.");
            alert.showAndWait();
        }
    }

    private void deleteSelected() {
        FtpConnection selected = connectionsTable.getSelectionModel().getSelectedItem();
        if (selected != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Conferma");
            confirm.setHeaderText("Eliminare la connessione: " + selected.getName() + "?");
            
            confirm.showAndWait().ifPresent(response -> {
                if (response == ButtonType.OK) {
                    FtpConnectionManager.deleteConnection(selected);
                    connections.remove(selected);
                }
            });
        }
    }
    
    private void createNewConnection() {
        Dialog<FtpConnection> dialog = new Dialog<>();
        dialog.setTitle("Nuova Connessione FTP");
        dialog.setHeaderText("Inserisci i dati di connessione FTP");
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Nome connessione");
        TextField hostField = new TextField();
        hostField.setPromptText("es. ftp.example.com");
        TextField portField = new TextField("21");
        TextField usernameField = new TextField();
        PasswordField passwordField = new PasswordField();
        TextField pathField = new TextField("/");
        pathField.setPromptText("Percorso iniziale");
        CheckBox usePassiveModeCheck = new CheckBox("Usa modalità passiva");
        usePassiveModeCheck.setSelected(true);
        
        grid.add(new Label("Nome:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Host:"), 0, 1);
        grid.add(hostField, 1, 1);
        grid.add(new Label("Porta:"), 0, 2);
        grid.add(portField, 1, 2);
        grid.add(new Label("Username:"), 0, 3);
        grid.add(usernameField, 1, 3);
        grid.add(new Label("Password:"), 0, 4);
        grid.add(passwordField, 1, 4);
        grid.add(new Label("Percorso:"), 0, 5);
        grid.add(pathField, 1, 5);
        grid.add(usePassiveModeCheck, 1, 6);
        
        dialog.getDialogPane().setContent(grid);
        
        ButtonType connectButtonType = new ButtonType("Connetti", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(connectButtonType, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == connectButtonType) {
                if (hostField.getText().isEmpty()) {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Errore");
                    alert.setHeaderText("Campo obbligatorio mancante");
                    alert.setContentText("Inserisci almeno l'host del server FTP.");
                    alert.showAndWait();
                    return null;
                }
                
                FtpConnection connection = new FtpConnection();
                connection.setName(nameField.getText().isEmpty() ? hostField.getText() : nameField.getText());
                connection.setHost(hostField.getText());
                try {
                    connection.setPort(Integer.parseInt(portField.getText()));
                } catch (NumberFormatException e) {
                    connection.setPort(21);
                }
                connection.setUsername(usernameField.getText());
                connection.setPassword(passwordField.getText());
                connection.setInitialPath(pathField.getText().isEmpty() ? "/" : pathField.getText());
                connection.setUsePassiveMode(usePassiveModeCheck.isSelected());
                
                // Connetti usando il manager
                FtpService ftpService = FtpConnectionManager.connect(connection);
                if (ftpService != null && ftpService.isConnected()) {
                    // Salva la connessione
                    FtpConnectionManager.saveConnection(connection);
                    connections.add(connection);
                    
                    // Chiama il callback se presente
                    if (onConnectCallback != null) {
                        onConnectCallback.accept(connection);
                    }
                    
                    return connection;
                } else {
                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Errore");
                    alert.setHeaderText("Impossibile connettersi al server FTP");
                    alert.setContentText("Verifica i dati inseriti e riprova.");
                    alert.showAndWait();
                }
            }
            return null;
        });
        
        dialog.showAndWait();
    }
}

