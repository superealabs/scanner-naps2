package com.bici.scanner.api;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;

public class OpenApiController extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(OpenApiController.class);

    public OpenApiController() {
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Charger le fichier OpenAPI spec depuis les ressources
            InputStream is = getClass().getClassLoader().getResourceAsStream("openapi.json");
            if (is == null) {
                logger.error("Fichier openapi.json non trouvé dans les ressources");
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                resp.setContentType("application/json");
                resp.getWriter().write("{\"error\": \"OpenAPI spec not found\"}");
                return;
            }

            // Lire et servir le fichier JSON
            String json = new String(is.readAllBytes(), "UTF-8");
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            resp.setCharacterEncoding("UTF-8");
            resp.getWriter().write(json);
            resp.getWriter().flush();
            
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de la spec OpenAPI", e);
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.setContentType("application/json");
            resp.getWriter().write("{\"error\": \"Failed to load OpenAPI spec\"}");
        }
    }
}

