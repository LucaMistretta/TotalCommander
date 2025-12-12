package com.totalcommander.services;

import org.apache.commons.compress.archivers.ArchiveEntry;
import org.apache.commons.compress.archivers.ArchiveException;
import org.apache.commons.compress.archivers.ArchiveInputStream;
import org.apache.commons.compress.archivers.ArchiveStreamFactory;
import org.apache.commons.compress.archivers.zip.ZipArchiveOutputStream;
import org.apache.commons.compress.utils.IOUtils;
import java.io.*;
import java.util.List;

/**
 * Servizio per gestione archivi (ZIP, TAR, etc.)
 */
public class ArchiveService {
    
    public boolean extractArchive(File archiveFile, File destinationDir) {
        try {
            String filename = archiveFile.getName().toLowerCase();
            
            if (filename.endsWith(".zip")) {
                return extractZip(archiveFile, destinationDir);
            } else if (filename.endsWith(".tar") || filename.endsWith(".tar.gz") || filename.endsWith(".tgz")) {
                return extractTar(archiveFile, destinationDir);
            } else {
                // Prova con ArchiveStreamFactory per altri formati
                return extractGeneric(archiveFile, destinationDir);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createZipArchive(List<File> files, File outputFile) {
        try (ZipArchiveOutputStream zos = new ZipArchiveOutputStream(new FileOutputStream(outputFile))) {
            for (File file : files) {
                addFileToZip(file, file.getName(), zos);
            }
            return true;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean extractZip(File zipFile, File destDir) throws IOException {
        try (ArchiveInputStream ais = new ArchiveStreamFactory()
                .createArchiveInputStream(ArchiveStreamFactory.ZIP, 
                    new BufferedInputStream(new FileInputStream(zipFile)))) {
            return extractArchive(ais, destDir);
        } catch (ArchiveException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean extractTar(File tarFile, File destDir) throws IOException {
        try (ArchiveInputStream ais = new ArchiveStreamFactory()
                .createArchiveInputStream(ArchiveStreamFactory.TAR, 
                    new BufferedInputStream(new FileInputStream(tarFile)))) {
            return extractArchive(ais, destDir);
        } catch (ArchiveException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean extractGeneric(File archiveFile, File destDir) {
        try (ArchiveInputStream ais = new ArchiveStreamFactory()
                .createArchiveInputStream(new BufferedInputStream(new FileInputStream(archiveFile)))) {
            return extractArchive(ais, destDir);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean extractArchive(ArchiveInputStream ais, File destDir) throws IOException {
        ArchiveEntry entry;
        while ((entry = ais.getNextEntry()) != null) {
            if (ais.canReadEntryData(entry)) {
                File file = new File(destDir, entry.getName());
                if (entry.isDirectory()) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    try (OutputStream os = new FileOutputStream(file)) {
                        IOUtils.copy(ais, os);
                    }
                }
            }
        }
        return true;
    }

    private void addFileToZip(File file, String entryName, ZipArchiveOutputStream zos) throws IOException {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    addFileToZip(child, entryName + "/" + child.getName(), zos);
                }
            }
        } else {
            ArchiveEntry entry = zos.createArchiveEntry(file, entryName);
            zos.putArchiveEntry(entry);
            try (FileInputStream fis = new FileInputStream(file)) {
                IOUtils.copy(fis, zos);
            }
            zos.closeArchiveEntry();
        }
    }
}

