package com.bici.scanner.api;

import com.bici.scanner.api.dto.*;
import com.bici.scanner.exception.*;
import com.bici.scanner.service.ScanService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

public class ScanController extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(ScanController.class);
    private final ScanService scanService;
    private final ObjectMapper objectMapper;

    public ScanController(ScanService scanService, ObjectMapper objectMapper) {
        this.scanService = scanService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            // POST /api/scans - Créer une session
            createScan(req, resp);
        } else if (pathInfo.matches("/[^/]+/start")) {
            // POST /api/scans/{id}/start - Démarrer un scan
            String scanId = extractScanId(pathInfo);
            startScan(scanId, resp);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo == null || pathInfo.equals("/")) {
            // GET /api/scans - Historique
            getHistory(req, resp);
        } else if (pathInfo.matches("/[^/]+/pdf")) {
            // GET /api/scans/{id}/pdf - Télécharger PDF
            String scanId = extractScanId(pathInfo);
            getPdf(scanId, resp);
        } else if (pathInfo.matches("/[^/]+")) {
            // GET /api/scans/{id} - Statut
            String scanId = extractScanId(pathInfo);
            getScanStatus(scanId, resp);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String pathInfo = req.getPathInfo();
        
        if (pathInfo != null && pathInfo.matches("/[^/]+")) {
            String scanId = extractScanId(pathInfo);
            cancelScan(scanId, resp);
        } else {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "NOT_FOUND", "Endpoint not found");
        }
    }

    private void createScan(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            ScanRequest request = objectMapper.readValue(req.getReader(), ScanRequest.class);
            ScanResponse response = scanService.createScan(request);
            
            resp.setStatus(HttpServletResponse.SC_CREATED);
            resp.setContentType("application/json");
            objectMapper.writeValue(resp.getWriter(), response);
            logger.info("Session de scan créée: {}", response.getScanId());
        } catch (ScanInProgressException e) {
            sendError(resp, HttpServletResponse.SC_CONFLICT, "SCAN_IN_PROGRESS", e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur lors de la création du scan", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
        }
    }

    private void startScan(String scanId, HttpServletResponse resp) throws IOException {
        try {
            ScanResponse response = scanService.startScan(scanId);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            objectMapper.writeValue(resp.getWriter(), response);
            logger.info("Scan démarré: {}", scanId);
        } catch (ScanNotFoundException e) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "SCAN_NOT_FOUND", e.getMessage(), scanId);
        } catch (ScanInProgressException e) {
            sendError(resp, HttpServletResponse.SC_CONFLICT, "SCAN_IN_PROGRESS", e.getMessage(), scanId);
        } catch (ScanInvalidStateException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_STATE", e.getMessage(), scanId);
        } catch (Naps2NotAvailableException e) {
            sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "NAPS2_UNAVAILABLE", e.getMessage(), scanId);
        } catch (Exception e) {
            logger.error("Erreur lors du démarrage du scan: {}", scanId, e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", scanId);
        }
    }

    private void getScanStatus(String scanId, HttpServletResponse resp) throws IOException {
        try {
            ScanResponse response = scanService.getScanStatus(scanId);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            objectMapper.writeValue(resp.getWriter(), response);
        } catch (ScanNotFoundException e) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "SCAN_NOT_FOUND", e.getMessage(), scanId);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du statut: {}", scanId, e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", scanId);
        }
    }

    private void getPdf(String scanId, HttpServletResponse resp) throws IOException {
        try {
            byte[] pdfData = scanService.getPdf(scanId);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/pdf");
            resp.setHeader("Content-Disposition", "attachment; filename=\"" + scanId + ".pdf\"");
            resp.setContentLength(pdfData.length);
            resp.getOutputStream().write(pdfData);
            resp.getOutputStream().flush();
            logger.info("PDF téléchargé: {}", scanId);
        } catch (ScanNotFoundException e) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "SCAN_NOT_FOUND", e.getMessage(), scanId);
        } catch (ScanInvalidStateException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_STATE", e.getMessage(), scanId);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération du PDF: {}", scanId, e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Failed to read PDF file", scanId);
        }
    }

    private void cancelScan(String scanId, HttpServletResponse resp) throws IOException {
        try {
            ScanResponse response = scanService.cancelScan(scanId);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            objectMapper.writeValue(resp.getWriter(), response);
            logger.info("Scan annulé: {}", scanId);
        } catch (ScanNotFoundException e) {
            sendError(resp, HttpServletResponse.SC_NOT_FOUND, "SCAN_NOT_FOUND", e.getMessage(), scanId);
        } catch (ScanInvalidStateException e) {
            sendError(resp, HttpServletResponse.SC_BAD_REQUEST, "INVALID_STATE", e.getMessage(), scanId);
        } catch (Exception e) {
            logger.error("Erreur lors de l'annulation du scan: {}", scanId, e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error", scanId);
        }
    }

    private void getHistory(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            int limit = parseIntParameter(req, "limit", 50);
            int offset = parseIntParameter(req, "offset", 0);
            String statusFilter = req.getParameter("status");
            
            ScanHistoryResponse response = scanService.getHistory(limit, offset, statusFilter);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            objectMapper.writeValue(resp.getWriter(), response);
        } catch (Exception e) {
            logger.error("Erreur lors de la récupération de l'historique", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
        }
    }

    private String extractScanId(String pathInfo) {
        // pathInfo format: "/{scanId}" ou "/{scanId}/start" ou "/{scanId}/pdf"
        String[] parts = pathInfo.split("/");
        return parts[1];
    }

    private int parseIntParameter(HttpServletRequest req, String name, int defaultValue) {
        String value = req.getParameter(name);
        if (value != null) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return defaultValue;
            }
        }
        return defaultValue;
    }

    private void sendError(HttpServletResponse resp, int status, String errorCode, String message) throws IOException {
        sendError(resp, status, errorCode, message, null);
    }

    private void sendError(HttpServletResponse resp, int status, String errorCode, String message, String scanId) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        if (scanId != null) {
            errorResponse.setScanId(scanId);
        }
        
        objectMapper.writeValue(resp.getWriter(), errorResponse);
    }
}

