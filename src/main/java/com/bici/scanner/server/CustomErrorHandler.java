package com.bici.scanner.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.handler.ErrorHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

public class CustomErrorHandler extends ErrorHandler {
    private static final Logger logger = LoggerFactory.getLogger(CustomErrorHandler.class);
    private final ObjectMapper objectMapper;

    public CustomErrorHandler(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void writeErrorPage(HttpServletRequest request, Writer writer, int code, String message, boolean showStacks)
            throws IOException {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "HTTP_" + code);
        errorResponse.put("message", message != null ? message : "Internal server error");
        errorResponse.put("timestamp", Instant.now().toString());
        errorResponse.put("path", request.getRequestURI());

        String json = objectMapper.writeValueAsString(errorResponse);
        writer.write(json);
    }

    @Override
    public void handle(String target, Request baseRequest,
                      HttpServletRequest request, HttpServletResponse response) throws IOException {
        int status = response.getStatus();
        
        if (status >= 400) {
            logger.warn("Erreur HTTP {} pour {} {}", status, request.getMethod(), request.getRequestURI());
        }
        
        try {
            super.handle(target, baseRequest, request, response);
        } catch (Exception e) {
            logger.error("Erreur dans le handler d'erreur", e);
            throw new IOException("Error handling failed", e);
        }
    }
}

