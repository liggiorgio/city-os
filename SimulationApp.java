// PM10 sensor manager

import sensors.PM10Sensor;

import java.util.ArrayList;
import java.util.Scanner;

public class SimulationApp {
    private static int sensorsNo = -1;
    private static String serverAddress;
    private static int serverPort;

    public static void main(String[] args) {
        // Log
        System.out.println("- Starting sensor simulator...");

        // Command-line parameters check
        checkErrors(args);

        // Log
        System.out.println("- Sensor simulator started!" +
                "\n\tServer serverAddress: " + serverAddress + ":" + serverPort +
                "\n\tNo. of sensors: " + sensorsNo);

        // Init and start sensor threads
        if (sensorsNo == 0)
            System.err.println("- WARNING: no sensors are being deployed!");
        else
            System.out.println("- Deploying sensors...");

        ArrayList<PM10Sensor> sensors = new ArrayList<>();
        for (int i = 0; i < sensorsNo; i++) {
            PM10Sensor temp = new PM10Sensor(serverAddress, serverPort);
            sensors.add(temp);
            new Thread(temp).start();
        }

        // Log
        System.out.println("- Sensors deployed! Entering idle mode...");
        Scanner keyIn = new Scanner(System.in);
        keyIn.nextLine();
        System.out.println("- Retiring sensors, please wait...");
        for (PM10Sensor s : sensors) {
            s.retire();
        }
    }

    // Check for errors in command-line parameters
    private static void checkErrors(String[] args) {
        String[] server;

        if (args.length < 2) {
            System.err.println("ERROR: Too few arguments.");
            System.exit(1);
        }

        try {
            sensorsNo = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: First argument must be an integer.");
            System.exit(2);
        }

        if (!args[1].matches(".+:.+")) {
            System.err.println("ERROR: Wrong serverAddress format, expected: HOST:PORT");
            System.exit(3);
        }

        server = args[1].split(":");
        serverAddress = server[0];
        try {
            serverPort = Integer.parseInt(server[1]);
        } catch (NumberFormatException e) {
            System.err.println("ERROR: Server serverPort must be an integer.");
            System.exit(4);
        }
    }
}
