package com.bici.scanner.service;

import com.bici.scanner.api.dto.DeviceInfo;
import com.bici.scanner.config.AppConfig;
import com.bici.scanner.exception.SaneNotAvailableException;
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

/**
 * Service de scan basé sur SANE (via l'utilitaire en ligne de commande scanimage).
 */
public class SaneService {
    private static final Logger logger = LoggerFactory.getLogger(SaneService.class);
    private final String saneCommand;
    private final int timeoutSeconds;

    public SaneService(AppConfig config) {
        this.saneCommand = config.getSaneCommand();
        logger.info("SANE command: {}", saneCommand);
        this.timeoutSeconds = config.getSaneTimeoutSeconds();
        logger.info("SANE timeout: {}", timeoutSeconds);
    }

    /**
     * Vérifie que scanimage est disponible sur le système.
     */
    public boolean isAvailable() {
        try {
            ProcessBuilder pb = new ProcessBuilder(saneCommand, "-V");
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
            logger.debug("SANE non disponible: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Liste les périphériques SANE disponibles.
     * Utilise le format structuré de scanimage (-f) pour faciliter le parsing :
     * "%d;%v;%m;%n" -> device;vendor;model
     */
    public List<DeviceInfo> listDevices() throws SaneNotAvailableException, ScannerException {
        if (!isAvailable()) {
            throw new SaneNotAvailableException("scanimage command not found or not executable");
        }

        try {
            List<String> command = buildListDevicesCommand();
            logger.info("Exécution SANE listdevices: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);

            Process process = pb.start();

            StringBuilder output = new StringBuilder();
            Future<?> readerFuture = Executors.newSingleThreadExecutor().submit(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        output.append(line).append("\n");
                        logger.debug("SANE listdevices output: {}", line);
                    }
                } catch (IOException e) {
                    logger.warn("Erreur lors de la lecture de la sortie SANE", e);
                }
            });

            int listDevicesTimeout = Math.min(5, timeoutSeconds);
            boolean finished = process.waitFor(listDevicesTimeout, TimeUnit.SECONDS);

            if (!finished) {
                logger.warn("Timeout lors de la liste des périphériques SANE");
                process.destroyForcibly();
                readerFuture.cancel(true);
                throw new ScannerException("List devices timeout after " + listDevicesTimeout + " seconds");
            }

            readerFuture.get(1, TimeUnit.SECONDS);

            int exitCode = process.exitValue();

            if (exitCode != 0) {
                String errorOutput = output.toString();
                logger.error("scanimage -L a échoué avec le code {}: {}", exitCode, errorOutput);
                throw new ScannerException("scanimage listdevices failed with exit code " + exitCode + ": " + errorOutput);
            }

            List<DeviceInfo> devices = parseDeviceList(output.toString());
            logger.info("{} périphérique(s) SANE trouvé(s)", devices.size());
            return devices;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScannerException("List devices interrupted", e);
        } catch (IOException e) {
            logger.error("Erreur lors de l'exécution de scanimage listdevices", e);
            throw new ScannerException("Failed to execute scanimage listdevices", e);
        } catch (ExecutionException | TimeoutException e) {
            logger.error("Erreur lors de la lecture de la sortie SANE", e);
            throw new ScannerException("Failed to read scanimage listdevices output", e);
        }
    }

    private List<String> buildListDevicesCommand() {
        List<String> command = new ArrayList<>();
        command.add(saneCommand);
        // Format structuré: device;vendor;model puis retour à la ligne
        command.add("-f");
        command.add("%d;%v;%m;%n");
        return command;
    }

    private List<DeviceInfo> parseDeviceList(String output) {
        List<DeviceInfo> devices = new ArrayList<>();

        if (output == null || output.trim().isEmpty()) {
            return devices;
        }

        String[] lines = output.split("\n");
        for (String line : lines) {
            String trimmedLine = line.trim();
            if (trimmedLine.isEmpty()) {
                continue;
            }

            // Format attendu : device;vendor;model
            String[] parts = trimmedLine.split(";", -1);
            String deviceAddress = parts.length > 0 ? parts[0].trim() : trimmedLine;
            String vendor = parts.length > 1 ? parts[1].trim() : "";
            String model = parts.length > 2 ? parts[2].trim() : "";

            String displayName = (vendor + " " + model).trim();
            if (displayName.isEmpty()) {
                displayName = deviceAddress;
            }

            String deviceId = generateDeviceId(deviceAddress);
            DeviceInfo device = new DeviceInfo(displayName, "sane", deviceId);
            devices.add(device);
        }

        return devices;
    }

    private String generateDeviceId(String deviceAddress) {
        String normalized = deviceAddress.toLowerCase()
                .replaceAll("\\s+", "_")
                .replaceAll("[^a-z0-9_:]", "");
        return "sane_" + normalized;
    }

    /**
     * Lance un scan via scanimage et produit un PDF.
     *
     * @param scanId       identifiant du scan (utilisé pour nommer le fichier)
     * @param scannerName  nom/adresse du device SANE (ex: "pixma:04A91913"), peut être null
     * @param optionsJson  options JSON : resolution, colorMode, source
     * @param outputDir    répertoire de sortie
     */
    public Path executeScan(String scanId, String scannerName, String optionsJson, Path outputDir)
            throws SaneNotAvailableException, ScannerException {
        if (!isAvailable()) {
            throw new SaneNotAvailableException("scanimage command not found or not executable");
        }

        Path outputFile = outputDir.resolve(scanId + ".pdf");

        try {
            List<String> command = buildCommand(scannerName, optionsJson, outputFile);
            logger.info("Exécution SANE: {}", String.join(" ", command));

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectOutput(ProcessBuilder.Redirect.PIPE);
            pb.redirectErrorStream(true);
            pb.redirectInput(ProcessBuilder.Redirect.PIPE);

            Process process = pb.start();

            ScanMonitor monitor = new ScanMonitor(process, scanId);
            Future<?> monitorFuture = Executors.newSingleThreadExecutor().submit(monitor);

            boolean finished = process.waitFor(timeoutSeconds, TimeUnit.SECONDS);

            if (!finished) {
                logger.warn("Timeout lors du scan SANE: {}", scanId);
                process.destroyForcibly();
                monitorFuture.cancel(true);
                throw new ScannerException("Scan timeout after " + timeoutSeconds + " seconds");
            }

            int exitCode = process.exitValue();
            monitorFuture.cancel(true);

            if (exitCode != 0) {
                String errorOutput = monitor.getErrorOutput();
                logger.error("scanimage a échoué avec le code {}: {}", exitCode, errorOutput);
                throw new ScannerException("scanimage scan failed with exit code " + exitCode + ": " + errorOutput);
            }

            if (!Files.exists(outputFile)) {
                throw new ScannerException("PDF file was not created: " + outputFile);
            }

            logger.info("Scan SANE terminé avec succès: {}", outputFile);
            return outputFile;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ScannerException("Scan interrupted", e);
        } catch (IOException e) {
            logger.error("Erreur lors de l'exécution de scanimage", e);
            throw new ScannerException("Failed to execute scanimage", e);
        }
    }

    private List<String> buildCommand(String scannerName, String optionsJson, Path outputFile) {
        List<String> command = new ArrayList<>();
        command.add(saneCommand);

        // Fichier de sortie + format PDF natif
        command.add("-o");
        command.add(outputFile.toAbsolutePath().toString());
        command.add("--format=pdf");

        // Progression
        command.add("-p");

        // Device
        if (scannerName != null && !scannerName.isEmpty()) {
            command.add("-d");
            command.add(scannerName);
        }

        boolean sourceSet = false;

        if (optionsJson != null && !optionsJson.isEmpty()) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                com.fasterxml.jackson.databind.JsonNode options = mapper.readTree(optionsJson);

                // Résolution (DPI)
                if (options.has("resolution")) {
                    int resolution = options.get("resolution").asInt();
                    if (resolution > 0) {
                        command.add("--resolution");
                        command.add(resolution + "dpi");
                    }
                }

                // Mode couleur -> --mode (Color/Gray/Lineart)
                if (options.has("colorMode")) {
                    String colorMode = options.get("colorMode").asText();
                    if (colorMode != null && !colorMode.isEmpty()) {
                        command.add("--mode");
                        String mode;
                        if ("Color".equalsIgnoreCase(colorMode)) {
                            mode = "Color";
                        } else if ("Grayscale".equalsIgnoreCase(colorMode)) {
                            mode = "Gray";
                        } else if ("BlackAndWhite".equalsIgnoreCase(colorMode)) {
                            mode = "Lineart";
                        } else {
                            mode = "Color"; // Par défaut
                        }
                        command.add(mode);
                    }
                }

                // Source (Flatbed / ADF / Duplex selon le device)
                if (options.has("source")) {
                    String source = options.get("source").asText();
                    if (source != null && !source.isEmpty()) {
                        command.add("--source");
                        command.add(source);
                        sourceSet = true;
                    }
                }

            } catch (Exception e) {
                logger.warn("Impossible de parser les options JSON: {}", optionsJson, e);
            }
        }

        // Par défaut : Flatbed (comme "glass" côté NAPS2), si non précisé
        if (!sourceSet) {
            command.add("--source");
            command.add("Flatbed");
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
                    logger.debug("SANE output [{}]: {}", scanId, line);
                }
            } catch (IOException e) {
                logger.warn("Erreur lors de la lecture de la sortie SANE", e);
            }
        }

        public String getErrorOutput() {
            return errorOutput.toString();
        }
    }
}