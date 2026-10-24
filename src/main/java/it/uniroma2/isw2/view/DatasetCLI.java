package it.uniroma2.isw2.view;

import it.uniroma2.isw2.controller.ReleaseDatasetController;
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

                ReleaseDatasetController controller = new ReleaseDatasetController();

                System.out.println("Caricamento STORM: release da Jira e classi da Git."
                        + " Il repository viene scaricato automaticamente se assente...");

                List<ReleaseDataset> datasets = controller.getReleaseDataset();

                long entries = datasets.stream().mapToLong(dataset -> dataset.getEntries().size()).sum();

                System.out.printf("Caricamento completato: %d release, %d entry.%n",
                        datasets.size(), entries);

                System.out.println("Costruzione del file CSV di base in corso...");
                Path output = controller.createBaseCsv(datasets);
                System.out.println("Costruzione del CSV di base completata: " + output);

            } catch (IOException | RuntimeException exception) {
                System.err.println("Costruzione del dataset fallita: " + exception.getMessage());
            }
        }
    }

    private String readLine(Scanner input, String prompt) {
        System.out.print(prompt);
        return input.hasNextLine() ? input.nextLine().strip() : null;
    }
}
