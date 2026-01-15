package com.bici.scanner;

import com.bici.scanner.config.AppConfig;
import com.bici.scanner.server.HttpServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ScannerAgent {
    private static final Logger logger = LoggerFactory.getLogger(ScannerAgent.class);
    private static HttpServer httpServer;

    public static void main(String[] args) {
        try {
            // Afficher aide si demandé
            if (args.length > 0 && ("--help".equals(args[0]) || "-h".equals(args[0]))) {
                printHelp();
                return;
            }

            logger.info("Démarrage de l'agent de scan local");
            
            // Initialisation configuration
            AppConfig config = AppConfig.getInstance(args);
            logger.info("Configuration chargée - Port: {}, Base de données: {}", 
                config.getPort(), config.getDatabasePath());

            // Enregistrer shutdown hook
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                logger.info("Arrêt de l'agent de scan...");
                shutdown();
            }));

            // Démarrage serveur HTTP
            httpServer = new HttpServer(config);
            httpServer.start();
            
            logger.info("Agent de scan démarré sur http://{}:{}", config.getHost(), config.getPort());
            logger.info("Interface web disponible sur http://{}:{}/ui", config.getHost(), config.getPort());
            
            // Attendre indéfiniment
            httpServer.join();
            
        } catch (Exception e) {
            logger.error("Erreur fatale lors du démarrage", e);
            System.exit(1);
        }
    }

    private static void shutdown() {
        if (httpServer != null) {
            try {
                httpServer.stop();
                logger.info("Serveur HTTP arrêté");
            } catch (Exception e) {
                logger.error("Erreur lors de l'arrêt du serveur HTTP", e);
            }
        }
    }

    private static void printHelp() {
        System.out.println("Agent de scan local - Usage:");
        System.out.println("  java -jar scanner-agent.jar [options]");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  --port=PORT              Port HTTP (défaut: 7070)");
        System.out.println("  --config=PATH            Chemin vers fichier de configuration");
        System.out.println("  --help, -h               Afficher cette aide");
        System.out.println();
        System.out.println("Exemples:");
        System.out.println("  java -jar scanner-agent.jar");
        System.out.println("  java -jar scanner-agent.jar --port=8080");
        System.out.println("  java -jar scanner-agent.jar --config=/path/to/config.properties");
    }
}

