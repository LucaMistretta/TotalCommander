package com.totalcommander;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.stage.Stage;
import com.totalcommander.ui.MainWindow;

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
            
            MainWindow mainWindow = new MainWindow();
            Scene scene = new Scene(mainWindow, 1200, 700);
            
            primaryStage.setTitle("Total Commander");
            primaryStage.setScene(scene);
            primaryStage.setMinWidth(800);
            primaryStage.setMinHeight(600);
            
            // Previene la chiusura accidentale
            primaryStage.setOnCloseRequest(e -> {
                // L'applicazione si chiuderà normalmente
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
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

