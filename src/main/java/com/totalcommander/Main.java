package com.totalcommander;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextArea;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.stage.Stage;
import com.totalcommander.ui.MainWindow;
import com.totalcommander.services.SettingsService;

/**
 * Classe principale dell'applicazione Total Commander
 */
public class Main extends Application {
    
    @Override
    public void start(Stage primaryStage) {
        try {
            // Handler per eccezioni non gestite nel thread JavaFX
            Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
                Platform.runLater(() -> {
                    showError("Errore Imprevisto", 
                        "Si è verificato un errore: " + exception.getMessage() + "\n\n" +
                        "L'applicazione continuerà a funzionare.\n" +
                        "Dettagli: " + exception.getClass().getSimpleName());
                    exception.printStackTrace();
                });
            });
            
            // Carica le impostazioni
            SettingsService settingsService = SettingsService.getInstance();
            SettingsService.ApplicationSettings settings = settingsService.getSettings();
            
            MainWindow mainWindow = new MainWindow();
            
            // Applica dimensione e posizione salvate
            double width = settings.windowWidth > 0 ? settings.windowWidth : 1200;
            double height = settings.windowHeight > 0 ? settings.windowHeight : 700;
            Scene scene = new Scene(mainWindow, width, height);
            
            primaryStage.setTitle("Total Commander");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Applica posizione salvata
            if (settings.windowX >= 0 && settings.windowY >= 0) {
                primaryStage.setX(settings.windowX);
                primaryStage.setY(settings.windowY);
            }
            
            // Applica stato maximized
            if (settings.windowMaximized) {
                primaryStage.setMaximized(true);
            }
            
            // Usa un timer per evitare troppi salvataggi durante il ridimensionamento
            javafx.animation.PauseTransition[] saveTimer = {null};
            
            Runnable scheduleSave = () -> {
                if (saveTimer[0] != null) {
                    saveTimer[0].stop();
                }
                saveTimer[0] = new javafx.animation.PauseTransition(javafx.util.Duration.millis(500));
                saveTimer[0].setOnFinished(ev -> {
                    if (primaryStage.isShowing() && !primaryStage.isMaximized()) {
                        double x = primaryStage.getX();
                        double y = primaryStage.getY();
                        double w = primaryStage.getWidth();
                        double h = primaryStage.getHeight();
                        // Salva solo se tutti i valori sono validi (non NaN)
                        // Rimuovo il controllo x >= 0 && y >= 0 perché su alcuni sistemi può essere negativo
                        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(w) && !Double.isNaN(h) 
                            && w > 0 && h > 0) {
                            settingsService.setWindowBounds(x, y, w, h, false);
                        }
                    }
                });
                saveTimer[0].play();
            };
            
            // Salva posizione e dimensione quando cambiano (con debounce)
            primaryStage.xProperty().addListener((obs, oldVal, newVal) -> {
                if (primaryStage.isShowing() && !primaryStage.isMaximized()) {
                    scheduleSave.run();
                }
            });
            
            primaryStage.yProperty().addListener((obs, oldVal, newVal) -> {
                if (primaryStage.isShowing() && !primaryStage.isMaximized()) {
                    scheduleSave.run();
                }
            });
            
            primaryStage.widthProperty().addListener((obs, oldVal, newVal) -> {
                if (primaryStage.isShowing() && !primaryStage.isMaximized()) {
                    scheduleSave.run();
                }
            });
            
            primaryStage.heightProperty().addListener((obs, oldVal, newVal) -> {
                if (primaryStage.isShowing() && !primaryStage.isMaximized()) {
                    scheduleSave.run();
                }
            });
            
            primaryStage.maximizedProperty().addListener((obs, oldVal, newVal) -> {
                double x = primaryStage.getX();
                double y = primaryStage.getY();
                double w = primaryStage.getWidth();
                double h = primaryStage.getHeight();
                // Salva solo se i valori sono validi
                if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(w) && !Double.isNaN(h) 
                    && w > 0 && h > 0) {
                    settingsService.setWindowBounds(x, y, w, h, newVal);
                }
            });
            
            // Salva quando la finestra viene chiusa
            primaryStage.setOnCloseRequest(e -> {
                double x = primaryStage.getX();
                double y = primaryStage.getY();
                double w = primaryStage.getWidth();
                double h = primaryStage.getHeight();
                if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(w) && !Double.isNaN(h) 
                    && w > 0 && h > 0) {
                    settingsService.setWindowBounds(x, y, w, h, primaryStage.isMaximized());
                }
            });
            
            // Salva quando la finestra viene mostrata (per catturare la posizione iniziale)
            primaryStage.setOnShown(e -> {
                javafx.application.Platform.runLater(() -> {
                    // Aspetta un po' per assicurarsi che la finestra sia completamente posizionata
                    javafx.animation.PauseTransition pause = new javafx.animation.PauseTransition(
                        javafx.util.Duration.millis(500));
                    pause.setOnFinished(ev -> {
                        double x = primaryStage.getX();
                        double y = primaryStage.getY();
                        double w = primaryStage.getWidth();
                        double h = primaryStage.getHeight();
                        if (!Double.isNaN(x) && !Double.isNaN(y) && !Double.isNaN(w) && !Double.isNaN(h) 
                            && w > 0 && h > 0) {
                            settingsService.setWindowBounds(x, y, w, h, primaryStage.isMaximized());
                        }
                    });
                    pause.play();
                });
            });
            
            primaryStage.show();
        } catch (Exception e) {
            showError("Errore di Avvio", 
                "Impossibile avviare l'applicazione: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
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

    public static void main(String[] args) {
        launch(args);
    }
}

