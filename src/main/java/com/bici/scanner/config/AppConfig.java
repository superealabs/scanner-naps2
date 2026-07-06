package com.bici.scanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class AppConfig {
    private static final Logger logger = LoggerFactory.getLogger(AppConfig.class);
    private static AppConfig instance;
    
    private final Properties properties;
    private final int port;
    private final String host;
    private final Path databasePath;
    private final String naps2Command;
    private final String saneCommand;
    private final int naps2TimeoutSeconds;
    private final int saneTimeoutSeconds;
    private final Path storageBasePath;
    private final boolean storageCleanupEnabled;
    private final int storageCleanupDays;
    private final String loggingLevel;
    private final boolean loggingFileEnabled;
    private final String loggingFilePath;

    private AppConfig(String[] args) {
        this.properties = loadProperties(args);
        
        // Serveur HTTP
        this.port = parsePort(properties.getProperty("server.port", "7070"), args);
        this.host = properties.getProperty("server.host", "127.0.0.1");
        
        // Base de données
        String dbPath = properties.getProperty("database.path", "./data/scanner.db");
        this.databasePath = Paths.get(dbPath).toAbsolutePath().normalize();
        
        // NAPS2
        this.naps2Command = properties.getProperty("naps2.command", "naps2");
        this.naps2TimeoutSeconds = Integer.parseInt(
            properties.getProperty("naps2.timeout.seconds", "300")
        );

        // SANE
        this.saneCommand = properties.getProperty("sane.command", "sane");
        this.saneTimeoutSeconds = Integer.parseInt(
                properties.getProperty("sane.timeout.seconds", "300")
        );
        
        // Stockage PDF
        String storagePath = properties.getProperty("storage.base.path", "./scans");
        this.storageBasePath = Paths.get(storagePath).toAbsolutePath().normalize();
        this.storageCleanupEnabled = Boolean.parseBoolean(
            properties.getProperty("storage.cleanup.enabled", "false")
        );
        this.storageCleanupDays = Integer.parseInt(
            properties.getProperty("storage.cleanup.days", "30")
        );
        
        // Logging
        this.loggingLevel = properties.getProperty("logging.level", "INFO");
        this.loggingFileEnabled = Boolean.parseBoolean(
            properties.getProperty("logging.file.enabled", "false")
        );
        this.loggingFilePath = properties.getProperty("logging.file.path", "./logs/scanner-agent.log");
        
        // Validation
        validate();
        
        // Initialisation répertoires
        initializeDirectories();
    }

    public static synchronized AppConfig getInstance(String[] args) {
        if (instance == null) {
            instance = new AppConfig(args);
        }
        return instance;
    }

    public static AppConfig getInstance() {
        if (instance == null) {
            throw new IllegalStateException("AppConfig not initialized. Call getInstance(String[]) first.");
        }
        return instance;
    }

    private Properties loadProperties(String[] args) {
        Properties props = new Properties();
        
        // Charger depuis ressources (dans JAR)
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("application.properties")) {
            if (is != null) {
                props.load(is);
                logger.debug("Configuration chargée depuis ressources");
            }
        } catch (IOException e) {
            logger.warn("Impossible de charger application.properties depuis ressources", e);
        }
        
        // Charger depuis fichier externe (si spécifié)
        String configFile = findConfigFile(args);
        if (configFile != null) {
            try (FileInputStream fis = new FileInputStream(configFile)) {
                props.load(fis);
                logger.info("Configuration chargée depuis fichier externe: {}", configFile);
            } catch (IOException e) {
                logger.warn("Impossible de charger le fichier de configuration: {}", configFile, e);
            }
        }
        
        return props;
    }

    private String findConfigFile(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--config=")) {
                return arg.substring("--config=".length());
            }
        }
        return null;
    }

    private int parsePort(String defaultPort, String[] args) {
        // Chercher --port= dans les arguments CLI
        for (String arg : args) {
            if (arg.startsWith("--port=")) {
                try {
                    return Integer.parseInt(arg.substring("--port=".length()));
                } catch (NumberFormatException e) {
                    logger.warn("Port invalide dans argument CLI: {}", arg);
                }
            }
        }
        return Integer.parseInt(defaultPort);
    }

    private void validate() {
        // Validation port
        if (port < 1024 || port > 65535) {
            throw new IllegalArgumentException("Port doit être entre 1024 et 65535: " + port);
        }
        
        // Validation chemins (vérifier capacité création)
        Path dbParent = databasePath.getParent();
        if (dbParent != null && !Files.exists(dbParent)) {
            try {
                Files.createDirectories(dbParent);
                logger.info("Répertoire base de données créé: {}", dbParent);
            } catch (IOException e) {
                throw new IllegalArgumentException("Impossible de créer le répertoire pour la base de données: " + dbParent, e);
            }
        }
    }

    private void initializeDirectories() {
        try {
            if (!Files.exists(storageBasePath)) {
                Files.createDirectories(storageBasePath);
                logger.info("Répertoire stockage PDF créé: {}", storageBasePath);
            }
        } catch (IOException e) {
            logger.error("Impossible de créer le répertoire de stockage: {}", storageBasePath, e);
            throw new RuntimeException("Erreur initialisation répertoires", e);
        }
    }

    // Getters
    public int getPort() {
        return port;
    }

    public String getHost() {
        return host;
    }

    public Path getDatabasePath() {
        return databasePath;
    }

    public String getNaps2Command() {
        return naps2Command;
    }

    public int getNaps2TimeoutSeconds() {
        return naps2TimeoutSeconds;
    }

    public Path getStorageBasePath() {
        return storageBasePath;
    }

    public boolean isStorageCleanupEnabled() {
        return storageCleanupEnabled;
    }

    public int getStorageCleanupDays() {
        return storageCleanupDays;
    }

    public String getLoggingLevel() {
        return loggingLevel;
    }

    public boolean isLoggingFileEnabled() {
        return loggingFileEnabled;
    }

    public String getLoggingFilePath() {
        return loggingFilePath;
    }

    public String getSaneCommand() {
        return saneCommand;
    }

    public int getSaneTimeoutSeconds() {
        return saneTimeoutSeconds;
    }
}

