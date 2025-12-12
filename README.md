# Total Commander - File Manager

File manager con doppio pannello sviluppato in Java con JavaFX, supporto per archivi e connessioni FTP.

## Caratteristiche

- **Doppio Pannello**: Due finestre affiancate per navigare e gestire file
- **Operazioni File**: Copia, sposta, elimina, rinomina, crea cartelle
- **Editor Testi**: Modifica file di testo
- **Supporto Archivi**: Estrazione e creazione di archivi ZIP, TAR, TAR.GZ
- **Rinomina Multipla**: Rinomina multipla file (in sviluppo)
- **Connessione FTP**: Connetti e gestisci server FTP con salvataggio connessioni

## Requisiti

- Java 17 o superiore
- Maven 3.6+

## Compilazione

```bash
mvn clean compile
```

## Esecuzione

```bash
mvn javafx:run
```

## Creazione Eseguibili

### Windows
```bash
mvn jpackage:jpackage -Dtype=msi
```

### Linux
```bash
mvn jpackage:jpackage -Dtype=deb
```

### macOS
```bash
mvn jpackage:jpackage -Dtype=dmg
```

## Struttura Progetto

```
src/main/java/com/totalcommander/
├── Main.java                    # Entry point
├── ui/
│   ├── MainWindow.java          # Finestra principale
│   ├── panels/
│   │   └── FilePanel.java       # Pannello file
│   ├── FtpConnectionDialog.java # Dialog connessione FTP
│   └── FtpManagerDialog.java    # Gestione connessioni
├── models/
│   ├── FileItem.java            # Modello file
│   └── FtpConnection.java        # Modello connessione FTP
└── services/
    ├── FileOperationService.java # Operazioni file
    ├── ArchiveService.java       # Gestione archivi
    ├── FtpService.java           # Servizio FTP
    └── FtpConnectionManager.java # Gestione connessioni salvate
```

## Funzionalità in Sviluppo

- Rinomina multipla file
- Creazione archivi da interfaccia
- Integrazione FTP nei pannelli
- Editor testi avanzato

## Note

Le connessioni FTP vengono salvate in `~/.totalcommander/ftp_connections.json`

