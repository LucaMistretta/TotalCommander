package com.totalcommander.models;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Modello per rappresentare un elemento file nella tabella
 */
public class FileItem {
    private String name;
    private String size;
    private String type;
    private String modifiedDate;
    private boolean isDirectory;

    public FileItem(String name, String type, String size, String modifiedDate, boolean isDirectory) {
        this.name = name;
        this.type = type;
        this.size = size;
        this.modifiedDate = modifiedDate;
        this.isDirectory = isDirectory;
    }

    public static FileItem fromFile(File file) {
        String name = file.getName();
        boolean isDir = file.isDirectory();
        String type = isDir ? "Cartella" : getFileExtension(name);
        String size = isDir ? "<DIR>" : formatFileSize(file.length());
        String date = formatDate(file.lastModified());
        
        return new FileItem(name, type, size, date, isDir);
    }

    private static String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1).toUpperCase();
        }
        return "File";
    }

    private static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.2f KB", bytes / 1024.0);
        if (bytes < 1024 * 1024 * 1024) return String.format("%.2f MB", bytes / (1024.0 * 1024.0));
        return String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0));
    }

    private static String formatDate(long millis) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(new Date(millis));
    }

    // Getters
    public String getName() { return name; }
    public String getSize() { return size; }
    public String getType() { return type; }
    public String getModifiedDate() { return modifiedDate; }
    public boolean isDirectory() { return isDirectory; }
}

