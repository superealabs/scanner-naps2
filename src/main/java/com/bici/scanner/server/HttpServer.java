package com.bici.scanner.server;

import com.bici.scanner.api.DeviceController;
import com.bici.scanner.api.HealthController;
import com.bici.scanner.api.OpenApiController;
import com.bici.scanner.api.ScanController;
import com.bici.scanner.config.AppConfig;
import com.bici.scanner.config.DatabaseConfig;
import com.bici.scanner.repository.ScanSessionRepository;
import com.bici.scanner.service.ScanService;
import com.bici.scanner.service.Naps2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.ServerConnector;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;

public class HttpServer {
    private static final Logger logger = LoggerFactory.getLogger(HttpServer.class);
    private final Server server;
    private final AppConfig config;

    public HttpServer(AppConfig config) {
        this.config = config;
        this.server = new Server();
        System.out.println("HttpServer constructor");
        // Configuration du connecteur
        ServerConnector connector = new ServerConnector(server);
        connector.setHost(config.getHost());
        connector.setPort(config.getPort());
        server.addConnector(connector);
        
        // Configuration du contexte
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        
        // Configuration CORS
        context.addFilter(CorsFilter.class, "/*", null);
        
        // Initialisation des services
        DatabaseConfig dbConfig = new DatabaseConfig(config);
        Connection connection = createConnection(dbConfig);
        ScanSessionRepository repository = new ScanSessionRepository(connection);
        Naps2Service naps2Service = new Naps2Service(config);
        ScanService scanService = new ScanService(repository, naps2Service, config);
        
        // Configuration Jackson
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        
        // Enregistrement des contrôleurs
        context.addServlet(new ServletHolder(new ScanController(scanService, objectMapper)), "/api/scans/*");
        context.addServlet(new ServletHolder(new HealthController(naps2Service, dbConfig, objectMapper)), "/api/health");
        context.addServlet(new ServletHolder(new DeviceController(naps2Service, objectMapper)), "/api/devices");
        context.addServlet(new ServletHolder(new OpenApiController()), "/api/openapi.json");
        
        // Servir UI statique depuis les ressources
        try {
            java.net.URL uiResource = getClass().getClassLoader().getResource("ui");
            if (uiResource != null) {
                String resourceBase = uiResource.toExternalForm();
                context.setResourceBase(resourceBase);
                context.addServlet(org.eclipse.jetty.servlet.DefaultServlet.class, "/ui/*");
                context.setWelcomeFiles(new String[]{"index.html"});
                logger.info("Interface web configurée: {}", resourceBase);
            } else {
                logger.warn("Répertoire UI non trouvé dans les ressources");
            }
        } catch (Exception e) {
            logger.warn("Impossible de configurer l'interface web statique", e);
        }
        
        // Servir Swagger UI depuis un contexte séparé
        try {
            java.net.URL swaggerResource = getClass().getClassLoader().getResource("swagger-ui");
            if (swaggerResource != null) {
                ServletContextHandler swaggerContext = new ServletContextHandler(ServletContextHandler.SESSIONS);
                swaggerContext.setContextPath("/swagger-ui");
                swaggerContext.setResourceBase(swaggerResource.toExternalForm());
                swaggerContext.addServlet(org.eclipse.jetty.servlet.DefaultServlet.class, "/*");
                swaggerContext.setWelcomeFiles(new String[]{"index.html"});
                
                // Utiliser HandlerList pour servir plusieurs contextes
                org.eclipse.jetty.server.handler.HandlerList handlers = new org.eclipse.jetty.server.handler.HandlerList();
                handlers.setHandlers(new org.eclipse.jetty.server.Handler[]{swaggerContext, context});
                server.setHandler(handlers);
                logger.info("Swagger UI configuré sur /swagger-ui");
            } else {
                logger.warn("Répertoire swagger-ui non trouvé dans les ressources");
                server.setHandler(context);
            }
        } catch (Exception e) {
            logger.warn("Impossible de configurer Swagger UI", e);
            server.setHandler(context);
        }
        
        // Handler d'erreurs global
        context.setErrorHandler(new CustomErrorHandler(objectMapper));
        
        server.setHandler(context);
    }

    private Connection createConnection(DatabaseConfig dbConfig) {
        try {
            return dbConfig.getConnection();
        } catch (Exception e) {
            logger.error("Erreur lors de la création de la connexion à la base de données", e);
            throw new RuntimeException("Impossible de créer la connexion à la base de données", e);
        }
    }

    public void start() throws Exception {
        server.start();
        logger.info("Serveur HTTP démarré sur {}:{}", config.getHost(), config.getPort());
    }

    public void stop() throws Exception {
        server.stop();
        logger.info("Serveur HTTP arrêté");
    }

    public void join() throws InterruptedException {
        server.join();
    }
}

