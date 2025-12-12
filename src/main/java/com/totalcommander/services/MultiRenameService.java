package com.totalcommander.services;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/**
 * Servizio per rinomina multipla file
 */
public class MultiRenameService {
    
    public boolean renameFile(Path oldPath, Path newPath) {
        try {
            // Verifica se il nuovo nome esiste già
            if (Files.exists(newPath) && !oldPath.equals(newPath)) {
                return false;
            }
            Files.move(oldPath, newPath, StandardCopyOption.REPLACE_EXISTING);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
}

