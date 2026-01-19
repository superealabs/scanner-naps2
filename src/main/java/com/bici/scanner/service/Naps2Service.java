package com.bici.scanner.service;

import com.bici.scanner.api.dto.DeviceInfo;
import com.bici.scanner.config.AppConfig;
import com.bici.scanner.exception.Naps2NotAvailableException;
import com.bici.scanner.exception.ScannerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class Naps2Service {
    private static final Logger logger = LoggerFactory.getLogger(Naps2Service.class);
    private final String naps2Command;
    private final int timeoutSeconds;

    public Naps2Service(AppConfig config) {
        this.naps2Command = config.getNaps2Command();
        logger.info("NAPS2 command: {}", naps2Command);
        this.timeoutSeconds = config.getNaps2TimeoutSeconds();
        logger.info("NAPS2 timeout: {}", timeoutSeconds);
    }

    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(naps2Command, "--version");
            // Redirection des streams pour éviter l'ouverture de fenêtres
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectError(ProcessBuilder.Redirect.PIPE);
            Process process = pb.start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            logger.debug("NAPS2 non disponible: {}", e.getMessage());
            return false;
        }
    }

    public List<DeviceInfo> listDevices(String driver) 
            throws Naps2NotAvailableException, ScannerException {
        if (!isAvailable()) {
            throw new Naps2NotAvailableException("NAPS2 command not found or not executable");
        }

        try {
            List<String> command = buildListDevicesCommand(driver);
            logger.info("Exécution NAPS2 listdevices: {}", String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            
            Process process = pb.start();
            
            // Lire la sortie dans un thread séparé
            StringBuilder output = new StringBuilder();
            Future<?> readerFuture = Executors.newSingleThreadExecutor().submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.debug("NAPS2 listdevices output: {}", line);
                    }
                } catch (IOException e) {
                    logger.warn("Erreur lors de la lecture de la sortie NAPS2", e);
                }
            });
            
            // Timeout plus court pour listdevices (5 secondes)
            int listDevicesTimeout = Math.min(5, timeoutSeconds);
            boolean finished = process.waitFor(listDevicesTimeout, TimeUnit.SECONDS);
            
            if (!finished) {
                logger.warn("Timeout lors de la liste des périphériques");
                process.destroyForcibly();
                readerFuture.cancel(true);
                throw new ScannerException("List devices timeout after " + listDevicesTimeout + " seconds");
            }
            
            readerFuture.get(1, TimeUnit.SECONDS); // Attendre que la lecture soit terminée
            
            int exitCode = process.exitValue();
            
            if (exitCode != 0) {
                String errorOutput = output.toString();
                logger.error("NAPS2 listdevices a échoué avec le code {}: {}", exitCode, errorOutput);
                throw new ScannerException("NAPS2 listdevices failed with exit code " + exitCode + ": " + errorOutput);
            }
            
            // Parser la sortie : une ligne = un périphérique
            List<DeviceInfo> devices = parseDeviceList(output.toString(), driver);
            logger.info("{} périphérique(s) trouvé(s)", devices.size());
            return devices;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScannerException("List devices interrupted", e);
        } catch (IOException e) {
            logger.error("Erreur lors de l'exécution de NAPS2 listdevices", e);
            throw new ScannerException("Failed to execute NAPS2 listdevices", e);
        } catch (ExecutionException | TimeoutException e) {
            logger.error("Erreur lors de la lecture de la sortie NAPS2", e);
            throw new ScannerException("Failed to read NAPS2 listdevices output", e);
        }
    }

    private List<String> buildListDevicesCommand(String driver) {
        List<String> command = new ArrayList<>();
        command.add(naps2Command);
        command.add("--listdevices");
        
        // Ajouter --driver si spécifié et valide
        if (driver != null && !driver.isEmpty()) {
            String normalizedDriver = driver.toLowerCase().trim();
            // Valider que le driver est dans la liste autorisée
            if (isValidDriver(normalizedDriver)) {
                command.add("--driver");
                command.add(normalizedDriver);
            } else {
                logger.warn("Driver invalide ignoré: {}", driver);
            }
        }
        
        return command;
    }

    private boolean isValidDriver(String driver) {
        return driver.equals("wia") || driver.equals("twain") || 
               driver.equals("escl") || driver.equals("sane") || 
               driver.equals("apple");
    }

    private List<DeviceInfo> parseDeviceList(String output, String driver) {
        List<DeviceInfo> devices = new ArrayList<>();
        
        if (output == null || output.trim().isEmpty()) {
            return devices;
        }
        
        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            // Ignorer les lignes vides
            if (trimmedLine.isEmpty()) {
                continue;
            }
            
            // Créer un DeviceInfo pour chaque ligne
            String deviceName = trimmedLine;
            String deviceId = generateDeviceId(deviceName, driver);
            DeviceInfo device = new DeviceInfo(deviceName, driver, deviceId);
            devices.add(device);
        }
        
        return devices;
    }

    private String generateDeviceId(String deviceName, String driver) {
        // Générer un ID unique basé sur le nom et le driver
        // Normaliser : lowercase, remplacer espaces par underscores
        String normalized = deviceName.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_]", "");
        
        if (driver != null && !driver.isEmpty()) {
            normalized = driver.toLowerCase() + "_" + normalized;
        }
        
        return normalized;
    }

    public Path executeScan(String scanId, String scannerName, String optionsJson, Path outputDir) 
            throws Naps2NotAvailableException, ScannerException {
        if (!isAvailable()) {
            throw new Naps2NotAvailableException("NAPS2 command not found or not executable");
        }

        Path outputFile = outputDir.resolve(scanId + ".pdf");
        
        try {
            List<String> command = buildCommand(scannerName, optionsJson, outputFile);
            logger.info("Exécution NAPS2: {}", String.join(" ", command));
            
            ProcessBuilder pb = new ProcessBuilder(command);
            // Redirection explicite des streams pour éviter l'ouverture de fenêtres
            // redirectErrorStream(true) combine stderr dans stdout pour faciliter la lecture
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);
            
            Process process = pb.start();
            
            // Monitoring dans un thread séparé
            ScanMonitor monitor = new ScanMonitor(process, scanId);
            Future<?> monitorFuture = Executors.newSingleThreadExecutor().submit(monitor);
            
            // Attendre la fin du processus avec timeout
            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);
            
            if (!finished) {
                logger.warn("Timeout lors du scan: {}", scanId);
                process.destroyForcibly();
                monitorFuture.cancel(true);
                throw new ScannerException("Scan timeout after " + timeoutSeconds + " seconds");
            }
            
            int exitCode = process.exitValue();
            monitorFuture.cancel(true);
            
            if (exitCode != 0) {
                String errorOutput = monitor.getErrorOutput();
                logger.error("NAPS2 a échoué avec le code {}: {}", exitCode, errorOutput);
                throw new ScannerException("NAPS2 scan failed with exit code " + exitCode + ": " + errorOutput);
            }
            
            // Vérifier que le fichier PDF a été créé
            if (!Files.exists(outputFile)) {
                throw new ScannerException("PDF file was not created: " + outputFile);
            }
            
            logger.info("Scan terminé avec succès: {}", outputFile);
            return outputFile;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScannerException("Scan interrupted", e);
        } catch (IOException e) {
            logger.error("Erreur lors de l'exécution de NAPS2", e);
            throw new ScannerException("Failed to execute NAPS2", e);
        }
    }

    private List<String> buildCommand(String scannerName, String optionsJson, Path outputFile) {
        List<String> command = new ArrayList<>();
        command.add(naps2Command);
        
        // Option de sortie
        command.add("-o");
        command.add(outputFile.toAbsolutePath().toString());
        
        // Mode verbeux pour voir la progression dans les logs
        command.add("-v");

        // Forcer l'ecrasement du fichier PDF
        command.add("--force");
        
        // Forcer le mode non-interactif pour éviter l'ouverture de fenêtres
        command.add("--noprofile");
        
        // Device (si scannerName fourni)
        if (scannerName != null && !scannerName.isEmpty()) {
            command.add("--device");
            command.add(scannerName);
        }
        
        // Options de scan depuis JSON (si disponibles)
        if (optionsJson != null && !optionsJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode options = mapper.readTree(optionsJson);
                
                // Taille de page
                if (options.has("pageSize")) {
                    String pageSize = options.get("pageSize").asText();
                    if (pageSize != null && !pageSize.isEmpty()) {
                        command.add("--pagesize");
                        command.add(pageSize.toLowerCase()); // a4, letter, etc.
                    }
                }
                
                // Résolution (DPI)
                if (options.has("resolution")) {
                    int resolution = options.get("resolution").asInt();
                    if (resolution > 0) {
                        command.add("--dpi");
                        command.add(String.valueOf(resolution));
                    }
                }
                
                // Mode couleur (bitdepth: color/gray/bw)
                if (options.has("colorMode")) {
                    String colorMode = options.get("colorMode").asText();
                    if (colorMode != null && !colorMode.isEmpty()) {
                        command.add("--bitdepth");
                        String bitdepth;
                        if ("Color".equalsIgnoreCase(colorMode)) {
                            bitdepth = "color";
                        } else if ("Grayscale".equalsIgnoreCase(colorMode)) {
                            bitdepth = "gray";
                        } else if ("BlackAndWhite".equalsIgnoreCase(colorMode)) {
                            bitdepth = "bw";
                        } else {
                            bitdepth = "color"; // Par défaut
                        }
                        command.add(bitdepth);
                    }
                }
                
                // Driver (wia, twain, etc.)
                if (options.has("driver")) {
                    String driver = options.get("driver").asText();
                    if (driver != null && !driver.isEmpty()) {
                        command.add("--driver");
                        command.add(driver);
                    }
                }
                
                // Source (glass/feeder/duplex)
                if (options.has("source")) {
                    String source = options.get("source").asText();
                    if (source != null && !source.isEmpty()) {
                        command.add("--source");
                        command.add(source.toLowerCase()); // glass, feeder, duplex
                    }
                } else {
                    // Par défaut : glass
                    command.add("--source");
                    command.add("glass");
                }
                
            } catch (Exception e) {
                logger.warn("Impossible de parser les options JSON: {}", optionsJson, e);
            }
        }
        
        return command;
    }

    private static class ScanMonitor implements Runnable {
        private final Process process;
        private final String scanId;
        private final StringBuilder errorOutput = new StringBuilder();

        public ScanMonitor(Process process, String scanId) {
            this.process = process;
            this.scanId = scanId;
        }

        @Override
        public void run() {
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    errorOutput.append(line).append("\n");
                    logger.debug("NAPS2 output [{}]: {}", scanId, line);
                }
            } catch (IOException e) {
                logger.warn("Erreur lors de la lecture de la sortie NAPS2", e);
            }
        }

        public String getErrorOutput() {
            return errorOutput.toString();
        }
    }
}

