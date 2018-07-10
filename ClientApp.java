// Client application for analysts to access cloud server records

import beans.Aggregate;
import beans.EdgeNode;
import beans.Average;
import beans.Statistic;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class ClientApp {

    private static final String serverAddress = "localhost";
    private static final int serverPort = 2302;

    public static void main(String[] args) {

        // Check server connection
        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/test");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                System.out.println("Connected to server!");
            } else {
                System.err.println("Not connected! Status: " + connection.getResponseCode());
                System.exit(1);
            }
            connection.disconnect();
        } catch (UnknownHostException e) {
            System.err.println("- WARNING: server doesn't exist!");
            System.exit(1);
        } catch (ProtocolException e) {
            e.printStackTrace();
            System.exit(1);
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL!");
            System.exit(1);
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
            System.exit(1);
        }

        // Successfully connected to cloud server, setup menu
        int choice;
        boolean exit = false, valid;
        Scanner mainInput = new Scanner(System.in);

        System.out.println("Welcome to the CityOS client application!");

        while (!exit) {
            System.out.println("Choose one of the following operations:" +
                    "\n 1. City grid status" +
                    "\n 2. Latest n stats from a given edge node" +
                    "\n 3. Latest n global stats" +
                    "\n 4. SD/AVG of latest n stats from a given node" +
                    "\n 5. SD/AVG of latest n global stats" +
                    "\n 0. Quit application");

            do {
                choice = mainInput.nextInt();
                valid = true;
                switch (choice) {
                    case 0: exit = true; break;
                    case 1: cmdStatus(); break;
                    case 2: cmdValuesLocal(); break;
                    case 3: cmdValuesGlobal(); break;
                    case 4: cmdStatsLocal(); break;
                    case 5: cmdStatsGlobal(); break;
                    default: valid = false;
                }
            } while (!valid);
        }

        // Quit application
        System.out.println("Goodbye!");
        System.exit(0);
    }

    // Get grid status
    private static void cmdStatus() {
        ArrayList<EdgeNode> nodes;
        Scanner localInput = new Scanner(System.in);

        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/nodes");
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                Reader reader = new InputStreamReader(input);
                nodes = new Gson().fromJson(reader, new TypeToken<ArrayList<EdgeNode>>(){}.getType());
            } else {
                System.err.println("Not connected! Status: " + connection.getResponseCode());
                return;
            }
            connection.disconnect();
        } catch (UnknownHostException e) {
            System.err.println("- WARNING: server doesn't exist!");
            return;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return;
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL!");
            return;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
            return;
        }

        System.out.println("City grid status:" +
                "\n P. ID\t| Address\t\t\t| N. port\t| S. port\t| Grid pos." +
                "\n--------+-------------------+-----------+-----------+----------");
        for (EdgeNode n : nodes) {
            System.out.println(" " + n.getId() + "\t| " + n.getAddress() + "\t| " + n.getNodePort() + "\t\t| " + n.getSensorPort() + "\t\t| (" + n.getU() + ", " + n.getV() + ")");
        }
        System.out.println("Press ENTER to continue...");
        localInput.nextLine();
    }

    // Get latest n measurements from given node
    private static void cmdValuesLocal() {
        int n, id;
        ArrayList<Average> averages;
        Scanner localInput = new Scanner(System.in);

        System.out.print("Which node to query? ");
        id = localInput.nextInt();
        localInput.nextLine();
        System.out.print("How many values to query? ");
        n = localInput.nextInt();
        localInput.nextLine();

        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/measurements/" + id + "?n=" + n);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                Reader reader = new InputStreamReader(input);
                averages = new Gson().fromJson(reader, new TypeToken<ArrayList<Average>>(){}.getType());
            } else {
                System.err.println("Not connected! Status: " + connection.getResponseCode());
                return;
            }
            connection.disconnect();
        } catch (UnknownHostException e) {
            System.err.println("- WARNING: server doesn't exist!");
            return;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return;
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL!");
            return;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
            return;
        }

        System.out.println("Latest " + n +" stats for node " + id + ":" +
                "\n KAType\t| Value \t\t\t\t| Timestamp" +
                "\n--------+-----------------------+---------------");
        for (Average m : averages) {
            System.out.println(" " + m.getType() + "\t| " + m.getValue() + "   \t| " + m.getTimestamp());
        }
        System.out.println("Press ENTER to continue...");
        localInput.nextLine();
    }

    // Get latest n global and local measurements
    private static void cmdValuesGlobal() {
        int n;
        ArrayList<Aggregate> aggregates;
        Scanner localInput = new Scanner(System.in);

        System.out.print("How many values to query? ");
        n = localInput.nextInt();
        localInput.nextLine();

        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/measurements?n=" + n);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                Reader reader = new InputStreamReader(input);
                aggregates = new Gson().fromJson(reader, new TypeToken<ArrayList<Aggregate>>(){}.getType());
            } else {
                System.err.println("Not connected! Status: " + connection.getResponseCode());
                return;
            }
            connection.disconnect();
        } catch (UnknownHostException e) {
            System.err.println("- WARNING: server doesn't exist!");
            return;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return;
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL!");
            return;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
            return;
        }

        System.out.println("Latest " + n +" global stats:" +
                "\n N. ID\t| KAType\t| Value \t\t\t\t| Timestamp");
        for (Aggregate a : aggregates) {
            Average g = a.getGlobal();
            ArrayList<Average> l = a.getLocals();
            System.out.println("--------+-------+-----------------------+---------------" +
                    "\n " + g.getId() + "\t| " + g.getType() + "\t| " + g.getValue() + "   \t| " + g.getTimestamp());
            for (Average m : l) {
                System.out.println(" " + m.getId() + "\t| " + m.getType() + "\t| " + m.getValue() + "   \t| " + m.getTimestamp());
            }
        }
        System.out.println("Press ENTER to continue...");
        localInput.nextLine();
    }

    // Get standard deviation & mean of latest n measurements from given node
    private static void cmdStatsLocal() {
        int n, id;
        Statistic stats;
        Scanner localInput = new Scanner(System.in);

        System.out.print("Which node to query? ");
        id = localInput.nextInt();
        System.out.print("How many values to query? ");
        n = localInput.nextInt();
        localInput.nextLine();

        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/measurements/stats/" + id + "?n=" + n);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                Reader reader = new InputStreamReader(input);
                stats = new Gson().fromJson(reader, Statistic.class);
            } else {
                System.err.println("Not connected! Status: " + connection.getResponseCode());
                return;
            }
            connection.disconnect();
        } catch (UnknownHostException e) {
            System.err.println("- WARNING: server doesn't exist!");
            return;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return;
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL!");
            return;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
            return;
        }

        System.out.println("Latest " + n + "measurements statistics for node " + id + ":" +
                "\n - Mean: " + stats.getMn() +
                "\n - Standard deviation: " + stats.getSd());

        System.out.println("Press ENTER to continue...");
        localInput.nextLine();
    }

    // Get standard deviation & mean of latest n global measurements
    private static void cmdStatsGlobal() {
        int n;
        Statistic stats;
        Scanner localInput = new Scanner(System.in);

        System.out.print("How many values to query? ");
        n = localInput.nextInt();
        localInput.nextLine();

        try {
            URL url = new URL("http://" + serverAddress + ":" + serverPort + "/api/measurements/stats?n=" + n);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("GET");
            connection.connect();
            if (connection.getResponseCode() == 200) {
                InputStream input = connection.getInputStream();
                Reader reader = new InputStreamReader(input);
                stats = new Gson().fromJson(reader, Statistic.class);
            } else {
                System.err.println("Not connected! Status: " + connection.getResponseCode());
                return;
            }
            connection.disconnect();
        } catch (UnknownHostException e) {
            System.err.println("- WARNING: server doesn't exist!");
            return;
        } catch (ProtocolException e) {
            e.printStackTrace();
            return;
        } catch (MalformedURLException e) {
            System.err.println("- WARNING: malformed server URL!");
            return;
        } catch (IOException e) {
            System.err.println("- WARNING: couldn't reach server!");
            return;
        }

        System.out.println("Latest " + n + "global measurements statistics:" +
                "\n - Mean: " + stats.getMn() +
                "\n - Standard deviation: " + stats.getSd());

        System.out.println("Press ENTER to continue...");
        localInput.nextLine();
    }
}
