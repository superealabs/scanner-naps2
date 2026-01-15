package com.bici.scanner.service;

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

