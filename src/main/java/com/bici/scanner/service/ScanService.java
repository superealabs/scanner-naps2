package com.bici.scanner.service;

import com.bici.scanner.api.dto.ScanHistoryResponse;
import com.bici.scanner.api.dto.ScanRequest;
import com.bici.scanner.api.dto.ScanResponse;
import com.bici.scanner.config.AppConfig;
import com.bici.scanner.exception.*;
import com.bici.scanner.repository.ScanSessionRepository;
import com.bici.scanner.repository.entity.ScanSession;
import com.bici.scanner.repository.entity.ScanStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class ScanService {
    private static final Logger logger = LoggerFactory.getLogger(ScanService.class);
    private final ScanSessionRepository repository;
    private final Naps2Service naps2Service;
    private final AppConfig config;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ScanService(ScanSessionRepository repository, Naps2Service naps2Service, AppConfig config) {
        this.repository = repository;
        this.naps2Service = naps2Service;
        this.config = config;
    }

    public ScanResponse createScan(ScanRequest request) throws ScanInProgressException {
        String scanId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli() / 1000; // Unix timestamp en secondes
        
        ScanSession session = new ScanSession();
        session.setScanId(scanId);
        session.setStatus(ScanStatus.PENDING);
        session.setScannerName(request.getScannerName());
        
        // Sérialiser options en JSON
        if (request.getOptions() != null) {
            try {
                session.setOptionsJson(objectMapper.writeValueAsString(request.getOptions()));
            } catch (Exception e) {
                logger.warn("Impossible de sérialiser les options", e);
            }
        }
        
        session.setCreatedAt(timestamp);
        session.setCreatedAtIso(now.toString());
        
        repository.save(session);
        logger.info("Session de scan créée: {}", scanId);
        
        return mapToResponse(session);
    }

    public ScanResponse startScan(String scanId) 
            throws ScanNotFoundException, ScanInProgressException, ScanInvalidStateException, Naps2NotAvailableException {
        // Vérifier qu'aucun scan n'est déjà en cours dans la base de données
        Optional<ScanSession> runningScan = repository.findRunningScan();
        if (runningScan.isPresent()) {
            throw new ScanInProgressException(
                "Un scan est déjà en cours: " + runningScan.get().getScanId()
            );
        }

        ScanSession session = repository.findById(scanId)
            .orElseThrow(() -> new ScanNotFoundException(scanId));

        // Vérifier l'état
        // if (session.getStatus() != ScanStatus.PENDING) {
        //     throw new ScanInvalidStateException(
        //         "Cannot start scan in status: " + session.getStatus()
        //     );
        // }

        // Mettre à jour l'état
        Instant now = Instant.now();
        long timestamp = now.toEpochMilli() / 1000;
        session.setStatus(ScanStatus.RUNNING);
        session.setStartedAt(timestamp);
        repository.update(session);
        logger.info("Scan démarré: {}", scanId);

        // Démarrer le scan dans un thread séparé
        Thread scanThread = new Thread(() -> executeScanAsync(session));
        scanThread.setDaemon(true);
        scanThread.start();

        return mapToResponse(session);
    }

    private void executeScanAsync(ScanSession session) {
        String scanId = session.getScanId();
        try {
            logger.info("Exécution du scan: {}", scanId);
            
            // Exécuter NAPS2
            Path pdfFile = naps2Service.executeScan(
                scanId,
                session.getScannerName(),
                session.getOptionsJson(),
                config.getStorageBasePath()
            );
            
            // Mettre à jour la session avec le résultat
            Instant now = Instant.now();
            long timestamp = now.toEpochMilli() / 1000;
            session.setStatus(ScanStatus.COMPLETED);
            session.setCompletedAt(timestamp);
            session.setPdfPath(pdfFile.toString());
            session.setPdfSize(Files.size(pdfFile));
            repository.update(session);
            
            logger.info("Scan terminé avec succès: {}", scanId);
            
        } catch (Exception e) {
            logger.error("Erreur lors de l'exécution du scan: {}", scanId, e);
            
            // Mettre à jour la session avec l'erreur
            try {
                Instant now = Instant.now();
                long timestamp = now.toEpochMilli() / 1000;
                session.setStatus(ScanStatus.FAILED);
                session.setCompletedAt(timestamp);
                session.setErrorMessage(e.getMessage());
                repository.update(session);
            } catch (Exception updateError) {
                logger.error("Erreur lors de la mise à jour de l'état d'erreur", updateError);
            }
        }
        // Le statut en base de données (COMPLETED/FAILED) indique que le scan est terminé
        // Pas besoin de libérer un verrou
    }

    public ScanResponse getScanStatus(String scanId) throws ScanNotFoundException {
        ScanSession session = repository.findById(scanId)
            .orElseThrow(() -> new ScanNotFoundException(scanId));
        return mapToResponse(session);
    }

    public byte[] getPdf(String scanId) 
            throws ScanNotFoundException, ScanInvalidStateException, IOException {
        ScanSession session = repository.findById(scanId)
            .orElseThrow(() -> new ScanNotFoundException(scanId));

        if (session.getStatus() != ScanStatus.COMPLETED) {
            throw new ScanInvalidStateException("PDF not available for scan in status: " + session.getStatus());
        }

        if (session.getPdfPath() == null) {
            throw new ScanNotFoundException("PDF path not set for scan: " + scanId);
        }

        Path pdfPath = Paths.get(session.getPdfPath());
        if (!Files.exists(pdfPath)) {
            throw new ScanNotFoundException("PDF file not found: " + pdfPath);
        }

        return Files.readAllBytes(pdfPath);
    }

    public ScanResponse cancelScan(String scanId) 
            throws ScanNotFoundException, ScanInvalidStateException {
        ScanSession session = repository.findById(scanId)
            .orElseThrow(() -> new ScanNotFoundException(scanId));

        if (session.getStatus() == ScanStatus.COMPLETED || 
            session.getStatus() == ScanStatus.FAILED ||
            session.getStatus() == ScanStatus.CANCELLED) {
            throw new ScanInvalidStateException("Cannot cancel scan in status: " + session.getStatus());
        }

        Instant now = Instant.now();
        long timestamp = now.toEpochMilli() / 1000;
        session.setStatus(ScanStatus.CANCELLED);
        session.setCancelledAt(timestamp);
        repository.update(session);
        
        logger.info("Scan annulé: {}", scanId);
        // Le statut CANCELLED en base de données suffit
        // Pas besoin de libérer un verrou
        
        return mapToResponse(session);
    }

    public ScanHistoryResponse getHistory(int limit, int offset, String statusFilter) {
        ScanStatus status = null;
        if (statusFilter != null && !statusFilter.isEmpty()) {
            try {
                status = ScanStatus.valueOf(statusFilter.toUpperCase());
            } catch (IllegalArgumentException e) {
                logger.warn("Statut invalide dans le filtre: {}", statusFilter);
            }
        }

        List<ScanSession> sessions = repository.findAll(limit, offset, status);
        int total = repository.count(status);

        ScanHistoryResponse response = new ScanHistoryResponse();
        response.setScans(sessions.stream()
            .map(this::mapToResponse)
            .collect(Collectors.toList()));
        response.setTotal(total);
        response.setLimit(limit);
        response.setOffset(offset);

        return response;
    }

    private ScanResponse mapToResponse(ScanSession session) {
        ScanResponse response = new ScanResponse();
        response.setScanId(session.getScanId());
        response.setStatus(session.getStatus().name());
        response.setCreatedAt(session.getCreatedAtIso());
        
        if (session.getStartedAt() != null) {
            response.setStartedAt(Instant.ofEpochSecond(session.getStartedAt()).toString());
        }
        if (session.getCompletedAt() != null) {
            response.setCompletedAt(Instant.ofEpochSecond(session.getCompletedAt()).toString());
        }
        
        response.setErrorMessage(session.getErrorMessage());
        response.setPdfPath(session.getPdfPath());
        response.setPdfSize(session.getPdfSize());
        
        return response;
    }
}

