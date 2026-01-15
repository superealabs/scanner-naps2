package com.bici.scanner.api;

import com.bici.scanner.api.dto.HealthResponse;
import com.bici.scanner.config.DatabaseConfig;
import com.bici.scanner.service.Naps2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.sql.Connection;

public class HealthController extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(HealthController.class);
    private final Naps2Service naps2Service;
    private final DatabaseConfig dbConfig;
    private final ObjectMapper objectMapper;

    public HealthController(Naps2Service naps2Service, DatabaseConfig dbConfig, ObjectMapper objectMapper) {
        this.naps2Service = naps2Service;
        this.dbConfig = dbConfig;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        HealthResponse response = new HealthResponse();
        
        // Vérifier NAPS2
        boolean naps2Available = naps2Service.isAvailable();
        response.setNaps2Available(naps2Available);
        
        // Vérifier base de données
        boolean dbConnected = false;
        try (Connection conn = dbConfig.getConnection()) {
            dbConnected = conn.isValid(1);
        } catch (Exception e) {
            logger.warn("Erreur lors de la vérification de la base de données", e);
        }
        response.setDatabaseConnected(dbConnected);
        
        // Statut global
        if (naps2Available && dbConnected) {
            response.setStatus("UP");
        } else {
            response.setStatus("DEGRADED");
        }
        
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        objectMapper.writeValue(resp.getWriter(), response);
    }
}

