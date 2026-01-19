package com.bici.scanner.api;

import com.bici.scanner.api.dto.DeviceInfo;
import com.bici.scanner.api.dto.DeviceListResponse;
import com.bici.scanner.api.dto.ErrorResponse;
import com.bici.scanner.exception.Naps2NotAvailableException;
import com.bici.scanner.exception.ScannerException;
import com.bici.scanner.service.Naps2Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class DeviceController extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(DeviceController.class);
    private final Naps2Service naps2Service;
    private final ObjectMapper objectMapper;

    public DeviceController(Naps2Service naps2Service, ObjectMapper objectMapper) {
        this.naps2Service = naps2Service;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // GET /api/devices - Lister les périphériques
        // Le servlet est mappé exactement à /api/devices, donc pas besoin de vérifier pathInfo
        listDevices(req, resp);
    }

    private void listDevices(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Récupérer le paramètre driver optionnel
            String driver = req.getParameter("driver");
            if (driver != null && driver.trim().isEmpty()) {
                driver = null;
            }
            
            // Appeler le service
            List<DeviceInfo> devices = naps2Service.listDevices(driver);
            DeviceListResponse response = new DeviceListResponse(devices);
            
            resp.setStatus(HttpServletResponse.SC_OK);
            resp.setContentType("application/json");
            objectMapper.writeValue(resp.getWriter(), response);
            logger.info("Liste des périphériques retournée: {} périphérique(s)", devices.size());
        } catch (Naps2NotAvailableException e) {
            sendError(resp, HttpServletResponse.SC_SERVICE_UNAVAILABLE, "NAPS2_UNAVAILABLE", e.getMessage());
        } catch (ScannerException e) {
            logger.error("Erreur lors de la récupération de la liste des périphériques", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", e.getMessage());
        } catch (Exception e) {
            logger.error("Erreur inattendue lors de la récupération de la liste des périphériques", e);
            sendError(resp, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "INTERNAL_ERROR", "Internal server error");
        }
    }

    private void sendError(HttpServletResponse resp, int status, String errorCode, String message) throws IOException {
        resp.setStatus(status);
        resp.setContentType("application/json");
        
        ErrorResponse errorResponse = new ErrorResponse(errorCode, message);
        objectMapper.writeValue(resp.getWriter(), errorResponse);
    }
}

