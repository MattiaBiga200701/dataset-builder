package it.uniroma2.isw2.view;

import it.uniroma2.isw2.controller.BuildDatasetController;
import it.uniroma2.isw2.controller.FeatureComputingController;
import it.uniroma2.isw2.model.entity.ReleaseDataset;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Scanner;

public class DatasetCLI {

    public void start() {


        Scanner input = new Scanner(System.in);

        while (true) {

            System.out.println("\n1. Costruisci il dataset di base CSV");
            System.out.println("0. Esci");

            String choice = readLine(input, "Scelta: ");

            if (choice == null || choice.equals("0")) {
                return;
            }
            if (!choice.equals("1")) {
                System.out.println("Scelta non valida.");
                continue;
            }
            try {

                BuildDatasetController controller = new BuildDatasetController();

                System.out.println("Caricamento STORM: release da Jira e classi da Git."
                        + " Il repository viene scaricato automaticamente se assente...");

                List<ReleaseDataset> datasets = controller.getReleaseDataset();

                long entries = datasets.stream().mapToLong(dataset -> dataset.getEntries().size()).sum();

                System.out.printf("Caricamento completato: %d release, %d entry.%n",
                        datasets.size(), entries);

                System.out.println("Costruzione del file CSV di base in corso...");
                Path output = controller.createBaseCsv(datasets);
                System.out.println("Creazione confermata: il file CSV di base e' stato scritto e chiuso correttamente.");
                System.out.println("File creato: " + output);

                showFeatureMenu(input, datasets);
                return;

            } catch (IOException | RuntimeException exception) {
                System.err.println("Costruzione del dataset fallita: " + exception.getMessage());
            }
        }
    }

    private void showFeatureMenu(Scanner input, List<ReleaseDataset> datasets) {
        while (true) {
            System.out.println("\n1. Crea Dataset.csv con le feature calcolate");
            System.out.println("0. Esci");

            String choice = readLine(input, "Scelta: ");
            if (choice == null || choice.equals("0")) {
                return;
            }
            if (!choice.equals("1")) {
                System.out.println("Scelta non valida.");
                continue;
            }

            try {
                System.out.println("Creazione del CSV con calcolo di LOC e LOC Touched in corso...");
                Path output = new FeatureComputingController().createDatasetCsv(datasets);
                System.out.println("File creato: " + output);
                System.out.println("LOC e LOC Touched compilati; le altre feature sono ancora da implementare.");
                return;
            } catch (IOException | RuntimeException exception) {
                System.err.println("Calcolo delle feature fallito: " + exception.getMessage());
            }
        }
    }

    private String readLine(Scanner input, String prompt) {
        System.out.print(prompt);
        return input.hasNextLine() ? input.nextLine().strip() : null;
    }
}
