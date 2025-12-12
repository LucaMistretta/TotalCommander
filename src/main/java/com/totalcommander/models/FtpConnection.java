package com.totalcommander.models;

/**
 * Modello per salvare una connessione FTP
 */
public class FtpConnection {
    private String name;
    private String host;
    private int port;
    private String username;
    private String password;
    private String initialPath;
    private boolean usePassiveMode = true; // Default: modalità passiva

    public FtpConnection() {
        this.port = 21;
        this.usePassiveMode = true;
    }

    public FtpConnection(String name, String host, int port, String username, String password, String initialPath) {
        this.name = name;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
        this.initialPath = initialPath;
    }

    // Getters e Setters
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getHost() { return host; }
    public void setHost(String host) { this.host = host; }

    public int getPort() { return port; }
    public void setPort(int port) { this.port = port; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getInitialPath() { return initialPath; }
    public void setInitialPath(String initialPath) { this.initialPath = initialPath; }

    public boolean isUsePassiveMode() { return usePassiveMode; }
    public void setUsePassiveMode(boolean usePassiveMode) { this.usePassiveMode = usePassiveMode; }
}

