package com.totalcommander.services;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Servizio per connessioni FTP
 */
public class FtpService {
    
    private FTPClient ftpClient;
    private String currentServer;
    private String currentUser;

    public FtpService() {
        this.ftpClient = new FTPClient();
    }

    public boolean connect(String host, int port, String username, String password) {
        return connect(host, port, username, password, true);
    }
    
    public boolean connect(String host, int port, String username, String password, boolean usePassiveMode) {
        try {
            ftpClient.connect(host, port);
            if (ftpClient.login(username, password)) {
                if (usePassiveMode) {
                    ftpClient.enterLocalPassiveMode();
                } else {
                    ftpClient.enterLocalActiveMode();
                }
                ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
                this.currentServer = host;
                this.currentUser = username;
                return true;
            }
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public void disconnect() {
        try {
            if (ftpClient.isConnected()) {
                ftpClient.logout();
                ftpClient.disconnect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean isConnected() {
        return ftpClient.isConnected();
    }

    public FTPFile[] listFiles(String remotePath) throws IOException {
        if (remotePath == null || remotePath.isEmpty()) {
            remotePath = "/";
        }
        return ftpClient.listFiles(remotePath);
    }
    
    public boolean changeDirectory(String remotePath) throws IOException {
        return ftpClient.changeWorkingDirectory(remotePath);
    }
    
    public String getCurrentDirectory() throws IOException {
        return ftpClient.printWorkingDirectory();
    }

    public boolean downloadFile(String remotePath, Path localPath) {
        try (OutputStream os = Files.newOutputStream(localPath)) {
            return ftpClient.retrieveFile(remotePath, os);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean uploadFile(Path localPath, String remotePath) {
        try (InputStream is = Files.newInputStream(localPath)) {
            return ftpClient.storeFile(remotePath, is);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public boolean uploadFile(InputStream inputStream, String remotePath) {
        try {
            return ftpClient.storeFile(remotePath, inputStream);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }
    
    public InputStream retrieveFileStream(String remotePath) throws IOException {
        return ftpClient.retrieveFileStream(remotePath);
    }
    
    public boolean completePendingCommand() throws IOException {
        return ftpClient.completePendingCommand();
    }

    public boolean deleteFile(String remotePath) {
        try {
            return ftpClient.deleteFile(remotePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public boolean createDirectory(String remotePath) {
        try {
            return ftpClient.makeDirectory(remotePath);
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    public String getCurrentServer() {
        return currentServer;
    }

    public String getCurrentUser() {
        return currentUser;
    }
    
    public void setFileType(boolean binary) throws IOException {
        if (binary) {
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
        } else {
            ftpClient.setFileType(FTP.ASCII_FILE_TYPE);
        }
    }
    
    public String getReplyString() {
        return ftpClient.getReplyString();
    }
    
    public int getReplyCode() {
        return ftpClient.getReplyCode();
    }
}

