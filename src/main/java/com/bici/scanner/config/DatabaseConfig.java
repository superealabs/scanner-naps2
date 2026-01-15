package com.bici.scanner.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Scanner;

public class DatabaseConfig {
    private static final Logger logger = LoggerFactory.getLogger(DatabaseConfig.class);
    private final String databasePath;

    public DatabaseConfig(AppConfig config) {
        this.databasePath = config.getDatabasePath().toString();
        initialize();
    }

    public Connection getConnection() throws SQLException {
        String url = "jdbc:sqlite:" + databasePath;
        return DriverManager.getConnection(url);
    }

    private void initialize() {
        try (Connection conn = getConnection()) {
            logger.info("Connexion à la base de données SQLite: {}", databasePath);
            
            // Vérifier si les tables existent
            if (!tablesExist(conn)) {
                logger.info("Initialisation du schéma de base de données");
                executeSchema(conn);
            } else {
                logger.debug("Schéma de base de données déjà initialisé");
            }
        } catch (SQLException e) {
            logger.error("Erreur lors de l'initialisation de la base de données", e);
            throw new RuntimeException("Impossible d'initialiser la base de données", e);
        }
    }

    private boolean tablesExist(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement()) {
            stmt.executeQuery("SELECT 1 FROM scan_sessions LIMIT 1");
            return true;
        } catch (SQLException e) {
            return false;
        }
    }

    private void executeSchema(Connection conn) throws SQLException {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("db/schema.sql");
             Scanner scanner = new Scanner(is, StandardCharsets.UTF_8)) {
            if (is == null) {
                throw new RuntimeException("Fichier schema.sql non trouvé dans les ressources");
            }
            
            String schema = scanner.useDelimiter("\\A").next();
            
            try (Statement stmt = conn.createStatement()) {
                // Exécuter chaque instruction SQL séparément
                String[] statements = schema.split(";");
                for (String statement : statements) {
                    String trimmed = statement.trim();
                    if (!trimmed.isEmpty()) {
                        stmt.execute(trimmed);
                    }
                }
                logger.info("Schéma de base de données initialisé avec succès");
            }
        } catch (Exception e) {
            logger.error("Erreur lors de l'exécution du schéma", e);
            throw new SQLException("Erreur exécution schéma", e);
        }
    }
}

