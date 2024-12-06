package yml_reader;

import utils.DataExtractor;
import utils.GeneratePieChart;

import java.util.InputMismatchException;
import java.util.Scanner;

public class YMLReaderAPP {

    public static void main(String[] args) {
        GeneratePieChart generatePieChart = new GeneratePieChart();
        Scanner scanner = new Scanner(System.in);

        while (true) {
            try {
                // Display menu options
                System.out.println("ENTER YOUR CHOICE: ");
                System.out.println("1 : Get all data about Build Plan and Pie Chart");
                System.out.println("2 : Get data about duplicate Configuration");
                System.out.println("3 : EXIT");

                int mainChoice = scanner.nextInt();

                switch (mainChoice) {
                    case 1:
                        DataExtractor.getAppConfig();
                        generatePieChart.generateChart();
                        break;
                    case 2:
                        DataExtractor.getDuplicateUsage();
                        break;
                    case 3:
                        System.out.println("Exiting program. Goodbye!");
                        scanner.close();
                        return;
                    default:
                        System.err.println("Invalid choice. Please try again.");
                }
            } catch (InputMismatchException e) {
                System.err.println("Invalid input. Please enter a valid number.");
                scanner.nextLine();
            }

            // Return to the main menu after each action
            System.out.println("\nReturning to the main menu...\n");
        }
    }
}
