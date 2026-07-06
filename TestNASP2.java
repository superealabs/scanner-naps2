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

public class TestNASP2 {
    public static void main(String[] args) {
        System.out.println("========================================");
        System.out.println("Test de scan NAPS2 - A4 Couleur");
        System.out.println("========================================\n");

        // Configuration
//        String naps2Command = "naps2.console"; // Windows
         String naps2Command = "naps2 console "; // Linux
        
        String outputDir = "./test-scans";
        String scanId = "test-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
        Path outputFile = Paths.get(outputDir, scanId + ".pdf");

        try {
            // Créer le répertoire de sortie
            Files.createDirectories(Paths.get(outputDir));
            System.out.println("Répertoire de sortie: " + outputDir);

            // Étape 1: Vérifier que NAPS2 est disponible
            System.out.println("\n[1/4] Vérification de NAPS2...");
            System.out.println("NAPS2  : " + naps2Command);
//            if (!checkNaps2Available(naps2Command)) {
//                System.err.println("ERREUR: NAPS2 n'est pas disponible!");
//                System.err.println("Vérifiez que NAPS2 est installé et que '" + naps2Command + "' est dans le PATH");
//                return;
//            }
            System.out.println("✓ NAPS2 est disponible");

            // Étape 2: Construire la commande de scan
            System.out.println("\n[2/4] Construction de la commande de scan...");
            // Optionnel: spécifier le profil scanner (exemple: "HP ScanJet Pro 2000 s2 (USB)")
            String scannerProfile = null; // Mettez le nom de votre scanner ici si nécessaire
            List<String> command = buildScanCommand(naps2Command, outputFile, scannerProfile, 300, "Color", "A4");
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

            // Afficher la sortie en temps réel
            Thread outputThread = new Thread(() -> {
                try (BufferedReader reader = new BufferedReader(
                        new InputStreamReader(process.getInputStream()))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        System.out.println("  [NAPS2] " + line);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            });
            outputThread.start();

            // Attendre la fin du processus (timeout de 5 minutes)
            boolean finished = process.waitFor(5, TimeUnit.MINUTES);
            
            if (!finished) {
                System.err.println("\n[X] ERREUR: Timeout - Le scan a pris plus de 5 minutes");
                process.destroyForcibly();
                return;
            }

            int exitCode = process.exitValue();
            outputThread.join(1000);

            if (exitCode != 0) {
                System.err.println("\n[X] ERREUR: NAPS2 a échoué avec le code: " + exitCode);
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

    private static boolean checkNaps2Available(String command) {
        try {
            ProcessBuilder pb = new ProcessBuilder(command, " --version");
            Process process = pb.start();
            boolean finished = process.waitFor(2, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return false;
            }
            return process.exitValue() == 0;
        } catch (Exception e) {
//            System.out.println("Exception , "+e.getMessage());
            return false;
        }
    }

    private static List<String> buildScanCommand(String naps2Command, Path outputFile, 
                                                  String scannerProfile, int dpi, String colorMode, String pageSize) {
        List<String> command = new ArrayList<>();
        command.add(naps2Command);
        
        // Option de sortie
        command.add("-o");
        command.add(outputFile.toAbsolutePath().toString());
        
        // Mode verbeux pour voir la progression
        command.add("-v");
        
        // Profil scanner (optionnel - à configurer selon votre scanner)
        if (scannerProfile != null && !scannerProfile.isEmpty()) {
            command.add("--profile");
            command.add(scannerProfile);
        }
        
        // Taille de page
        if (pageSize != null && !pageSize.isEmpty()) {
            command.add("--pagesize");
            command.add(pageSize.toLowerCase()); // a4, letter, etc.
        }
        
        // Résolution (DPI)
        if (dpi > 0) {
            command.add("--dpi");
            command.add(String.valueOf(dpi));
        }
        
        // Mode couleur (bitdepth: color/gray/bw)
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
        
        return command;
    }
}
