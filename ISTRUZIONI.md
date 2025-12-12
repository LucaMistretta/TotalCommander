# Istruzioni per Total Commander

## Requisiti di Sistema

- **Java 17 o superiore** (JDK)
- **Maven 3.6+**
- **Sistema Operativo**: Windows, macOS o Linux

## Installazione Dipendenze

Le dipendenze verranno scaricate automaticamente da Maven al primo build.

## Compilazione

### Compilare il progetto
```bash
mvn clean compile
```

### Eseguire l'applicazione
```bash
mvn javafx:run
```

## Creazione Eseguibili

### Creare JAR eseguibile
```bash
mvn clean package
```
Il JAR eseguibile sarà creato in `target/total-commander-1.0.0.jar`

### Eseguire il JAR
```bash
java -jar target/total-commander-1.0.0.jar
```

### Creare eseguibili nativi (richiede jpackage - JDK 14+)

#### Windows (MSI Installer)
```bash
mvn clean package
jpackage --input target --name TotalCommander --main-jar total-commander-1.0.0.jar --main-class com.totalcommander.Main --type msi --dest target/dist
```

#### Linux (DEB Package)
```bash
mvn clean package
jpackage --input target --name TotalCommander --main-jar total-commander-1.0.0.jar --main-class com.totalcommander.Main --type deb --dest target/dist
```

#### macOS (DMG)
```bash
mvn clean package
jpackage --input target --name TotalCommander --main-jar total-commander-1.0.0.jar --main-class com.totalcommander.Main --type dmg --dest target/dist
```

**Nota**: Per creare eseguibili con `jpackage`, è necessario avere il JDK 14+ installato (non solo JRE) e su macOS serve un certificato per la firma.

## Funzionalità

### Operazioni File
- **Copia**: Seleziona file nel pannello attivo e usa il menu Modifica > Copia (o tasto Tab per cambiare pannello)
- **Sposta**: Seleziona file e usa Modifica > Sposta
- **Elimina**: Seleziona file e usa File > Elimina
- **Crea Cartella**: File > Nuova Cartella
- **Rinomina**: Click destro > Rinomina o File > Rinomina

### Rinomina Multipla
1. Seleziona più file (Ctrl+Click o Shift+Click)
2. File > Rinomina Multipla
3. Usa le opzioni:
   - **Trova/Sostituisci**: Sostituisci testo nel nome
   - **Prefisso/Suffisso**: Aggiungi testo all'inizio/fine
   - **Contatore**: Aggiungi numeri sequenziali (es. file001, file002)

### Archivi
- **Estrai**: Seleziona un archivio (ZIP, TAR, TAR.GZ) e click destro > Estrai Archivio
- **Crea**: Seleziona file, Archivi > Crea Archivio

### FTP
- **Connetti**: FTP > Connetti FTP
- **Gestisci**: FTP > Gestisci Connessioni (visualizza e gestisci connessioni salvate)

## Suggerimenti

- Usa **Tab** per cambiare pannello attivo
- **Doppio click** su una cartella per aprirla
- **Doppio click** su un file per aprirlo con l'applicazione predefinita
- Le connessioni FTP vengono salvate in `~/.totalcommander/ftp_connections.json`

## Risoluzione Problemi

### Errore: "JavaFX runtime components are missing"
Assicurati di avere JavaFX nel classpath. Se usi Java 11+, JavaFX non è incluso di default.

### Errore di compilazione con module-info
Se hai problemi con i moduli, rimuovi il file `src/main/java/module-info.java` e usa JavaFX senza moduli.

### Problemi con jpackage
Su alcuni sistemi, `jpackage` potrebbe non essere disponibile. In alternativa, puoi usare strumenti come:
- **Windows**: Launch4j, Inno Setup
- **macOS**: jpackage (richiede Xcode)
- **Linux**: jpackage o AppImage

