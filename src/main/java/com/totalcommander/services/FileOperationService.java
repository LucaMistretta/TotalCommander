package com.totalcommander.services;

import com.totalcommander.ui.FileViewerDialog;
import com.totalcommander.ui.FileEditorDialog;
import javafx.application.Platform;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.awt.Desktop;

/**
 * Servizio per operazioni sui file
 */
public class FileOperationService {
    
    public boolean copyFiles(List<File> files, Path destination) {
        try {
            for (File file : files) {
                Path destPath = destination.resolve(file.getName());
                if (file.isDirectory()) {
                    copyDirectory(file.toPath(), destPath);
                } else {
                    Files.copy(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean moveFiles(List<File> files, Path destination) {
        try {
            for (File file : files) {
                Path destPath = destination.resolve(file.getName());
                Files.move(file.toPath(), destPath, StandardCopyOption.REPLACE_EXISTING);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean deleteFiles(List<File> files) {
        try {
            for (File file : files) {
                deleteRecursive(file);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createFolder(Path folderPath) {
        try {
            Files.createDirectories(folderPath);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean renameFile(Path oldPath, Path newPath) {
        try {
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void openFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return;
        }
        
        // Usa il visualizzatore personalizzato
        Platform.runLater(() -> {
            try {
                FileViewerDialog viewer = new FileViewerDialog(file);
                viewer.show();
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback: prova con Desktop
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().open(file);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    public void editFile(File file) {
        if (file == null || !file.exists() || file.isDirectory()) {
            return;
        }
        
        // Usa l'editor personalizzato
        Platform.runLater(() -> {
            try {
                FileEditorDialog editor = new FileEditorDialog(file);
                editor.show();
            } catch (Exception e) {
                e.printStackTrace();
                // Fallback: prova con Desktop
                try {
                    if (Desktop.isDesktopSupported()) {
                        Desktop.getDesktop().edit(file);
                    }
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        });
    }

    private void copyDirectory(Path source, Path destination) throws IOException {
        Files.walk(source).forEach(sourcePath -> {
            try {
                Path destPath = destination.resolve(source.relativize(sourcePath));
                if (Files.isDirectory(sourcePath)) {
                    Files.createDirectories(destPath);
                } else {
                    Files.copy(sourcePath, destPath, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }

    private void deleteRecursive(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }
}

