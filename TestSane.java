import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test standalone de scan via SANE (scanimage) - équivalent de TestNASP2
 * mais pour l'outil en ligne de commande scanimage (Linux).
 */
public class TestSane {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Test de scan SANE (scanimage) - A4 Couleur");
        System.out.println("========================================\n");

        // Configuration
        String saneCommand = "scanimage"; // Linux/SANE

        String outputDir = "./test-scans";
        String scanId = "test-sane-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path outputFile = Paths.get(outputDir, scanId + ".pdf");

        try {
            Files.createDirectories(Paths.get(outputDir));
            System.out.println("Répertoire de sortie: " + outputDir);

            // Étape 1: Vérifier que scanimage est disponible
            System.out.println("\n[1/4] Vérification de SANE (scanimage)...");
            if (!checkSaneAvailable(saneCommand)) {
                System.err.println("ERREUR: scanimage n'est pas disponible!");
                System.err.println("Vérifiez que sane-utils est installé (ex: apt install sane-utils)");
                return;
            }
            System.out.println("✓ scanimage est disponible");

            // Étape 1bis (optionnelle): lister les périphériques disponibles
            System.out.println("\nPériphériques SANE détectés:");
            listDevices(saneCommand);

            // Étape 2: Construire la commande de scan
            System.out.println("\n[2/4] Construction de la commande de scan...");
            // Optionnel: spécifier le device (exemple: "pixma:04A91913" ou "airscan:e0:Canon LiDE 300 (USB)")
            String deviceName = null; // Mettez le nom de votre device ici si nécessaire
            List<String> command = buildScanCommand(saneCommand, outputFile, deviceName, 300, "Color", "Flatbed");
            System.out.println("Commande: " + String.join(" ", command));
            System.out.println("Fichier de sortie: " + outputFile.toAbsolutePath());

            // Étape 3: Lancer le scan
            System.out.println("\n[3/4] Lancement du scan...");
            System.out.println("!!!  Assurez-vous qu'un document A4 est placé dans le scanner!");
            System.out.println("!!!  Le scan va commencer dans 3 secondes...");
            Thread.sleep(3000);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("  [SANE] " + line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            outputThread.start();

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);

            if (!finished) {
                System.err.println("\n[X] ERREUR: Timeout - Le scan a pris plus de 5 minutes");
                process.destroyForcibly();
                return;
            }

            int exitCode = process.exitValue();
            outputThread.join(1000);

            if (exitCode != 0) {
                System.err.println("\n[X] ERREUR: scanimage a échoué avec le code: " + exitCode);
                return;
            }

            System.out.println("✓ Scan terminé (code: " + exitCode + ")");

            // Étape 4: Vérifier le fichier généré
            System.out.println("\n[4/4] Vérification du fichier PDF...");
            if (Files.exists(outputFile)) {
                long fileSize = Files.size(outputFile);
                System.out.println("✓ PDF généré avec succès!");
                System.out.println("  Fichier: " + outputFile.toAbsolutePath());
                System.out.println("  Taille: " + fileSize + " octets (" + (fileSize / 1024) + " KB)");

                System.out.println("\n========================================");
                System.out.println("[V] TEST RÉUSSI!");
                System.out.println("========================================");
            } else {
                System.err.println("\n[X] ERREUR: Le fichier PDF n'a pas été créé!");
                System.err.println("  Chemin attendu: " + outputFile.toAbsolutePath());
            }

        } catch (Exception e) {
            System.err.println("\n[X] ERREUR: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static boolean checkSaneAvailable(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, "-V");
            Process process = pb.start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
            return false;
        }
    }

    private static void listDevices(String saneCommand) {
        try {
            ProcessBuilder pb = new ProcessBuilder(saneCommand, "-L");
            pb.redirectErrorStream(true);
            Process process = pb.start();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    System.out.println("  " + line);
                }
            }
            process.waitFor(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("  (impossible de lister les périphériques: " + e.getMessage() + ")");
        }
    }

    private static List<String> buildScanCommand(String saneCommand, Path outputFile, String deviceName,
                                                   int dpi, String colorMode, String source) {
        List<String> command = new ArrayList<>();
        command.add(saneCommand);

        // Fichier de sortie + format PDF natif à scanimage
        command.add("-o");
        command.add(outputFile.toAbsolutePath().toString());
        command.add("--format=pdf");

        // Progression
        command.add("-p");

        // Device (optionnel)
        if (deviceName != null && !deviceName.isEmpty()) {
            command.add("-d");
            command.add(deviceName);
        }

        // Source (Flatbed, ADF, Duplex... dépend du device)
        if (source != null && !source.isEmpty()) {
            command.add("--source");
            command.add(source);
        }

        // Résolution (DPI) - format attendu par scanimage: "300dpi"
        if (dpi > 0) {
            command.add("--resolution");
            command.add(dpi + "dpi");
        }

        // Mode couleur: Color / Gray / Lineart
        if (colorMode != null && !colorMode.isEmpty()) {
            command.add("--mode");
            String mode;
            if ("Color".equalsIgnoreCase(colorMode)) {
                mode = "Color";
            } else if ("Grayscale".equalsIgnoreCase(colorMode) || "Gray".equalsIgnoreCase(colorMode)) {
                mode = "Gray";
            } else if ("BlackAndWhite".equalsIgnoreCase(colorMode) || "Lineart".equalsIgnoreCase(colorMode)) {
                mode = "Lineart";
            } else {
                mode = "Color";
            }
            command.add(mode);
        }

        return command;
    }
}